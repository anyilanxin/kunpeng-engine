/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.loadbalancer;

import com.anyilanxin.kunpeng.client.ClientLoggers;
import com.anyilanxin.kunpeng.client.ExecutorResource;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.KunpengClientImpl;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.Gateway;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayConnectorInfo;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayInfo;
import com.anyilanxin.kunpeng.client.store.GatewayInfoPersistedStore;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import org.slf4j.Logger;

/**
 * Default adapter: discovery and load both flow through a single {@code discoveryClient} bound to a
 * reachable seed. The load RPC carries the target gateway's grpcAddress; the receiving gateway
 * forwards via cluster messaging when it isn't the target itself. The client therefore only needs
 * one gRPC channel regardless of gateway count.
 *
 * <p>On startup the configured gateway address is tried first. If it is unreachable, persisted
 * gateway addresses from {@link GatewayInfoPersistedStore} are tried immediately. If no gateway can
 * be reached and no discoverable gateway is returned, client startup fails.
 *
 * <p>Every successful discovery response is persisted so it can be used as a fallback after a
 * client restart.
 */
public class GatewayServiceDiscoverAdapterImpl implements GatewayServiceDiscoverAdapter {
  private static final Logger LOG = ClientLoggers.GATEWAY_DISCOVER_LOGGER;

  private final URI seedServer;
  private final List<URI> discoverGrpcAddress = new ArrayList<>();
  private final KunpengClientConfiguration configuration;

  private KunpengClientImpl discoveryClient;
  private ExecutorResource discoveryExecutor;
  private URI currentClientUri;
  private boolean hasSuccessfulDiscovery;
  private boolean seedServerFailed;

  public GatewayServiceDiscoverAdapterImpl(final KunpengClientConfiguration configuration) {
    seedServer = configuration.getGrpcAddress();
    this.configuration = configuration;
    loadStoredGateways();
    final List<URI> initial = eagerDiscover();
    if (initial.isEmpty()) {
      throw new IllegalStateException(
          "Unable to discover any reachable gateway. Configured address: "
              + seedServer
              + ", stored addresses: "
              + discoverGrpcAddress);
    }
    LOG.debug("Startup discovery succeeded with {} gateway(s)", initial.size());
  }

  private void loadStoredGateways() {
    try {
      final Map<String, String> stored = GatewayInfoPersistedStore.getGatewayInfo();
      for (final String address : stored.values()) {
        if (address == null || address.isBlank()) {
          continue;
        }
        try {
          final URI uri = URI.create(address);
          if (uri.getHost() != null && !uri.getHost().isBlank()) {
            discoverGrpcAddress.add(uri);
          } else {
            LOG.warn("Skipping stored gateway address with no host: {}", address);
          }
        } catch (final IllegalArgumentException e) {
          LOG.warn("Skipping invalid stored gateway address: {}", address, e);
        }
      }
      LOG.debug("Loaded {} persisted gateway addresses", discoverGrpcAddress.size());
    } catch (final Exception e) {
      LOG.warn("Failed to load persisted gateway addresses", e);
    }
  }

  /**
   * Synchronous startup discovery: try the configured seed first, then every persisted address, and
   * stop as soon as a reachable gateway returns at least one discoverable gateway.
   */
  private List<URI> eagerDiscover() {
    final List<URI> candidates = new ArrayList<>();
    if (seedServer != null) {
      candidates.add(seedServer);
    }
    candidates.addAll(discoverGrpcAddress);

    final Set<URI> tried = new LinkedHashSet<>();
    for (final URI queryUri : candidates) {
      if (!tried.add(queryUri)) {
        continue;
      }
      try {
        ensureDiscoveryClient(queryUri);
        final Gateway gateway = discoveryClient.newQueryGatewayRequest().send().join();
        final List<GatewayInfo> gateways = gateway.getGateways();
        final Set<URI> discovered = parseGatewayAddresses(gateways);
        updateState(gateways, discovered);
        if (!discovered.isEmpty()) {
          return new ArrayList<>(discovered);
        }
        LOG.warn("Gateway {} returned an empty discoverable gateway list", queryUri);
      } catch (final Exception e) {
        LOG.error("Failed to query gateway address from {} during startup", queryUri, e);
        resetDiscoveryClient();
        if (queryUri.equals(seedServer)) {
          seedServerFailed = true;
        }
        discoverGrpcAddress.remove(queryUri);
      }
    }
    return List.of();
  }

  @Override
  public List<URI> discoverGateways() {
    final URI queryUri = pickQueryUri();
    if (queryUri == null) {
      return List.of();
    }
    try {
      ensureDiscoveryClient(queryUri);
      final Gateway gateway = discoveryClient.newQueryGatewayRequest().send().join();
      final List<GatewayInfo> gateways = gateway.getGateways();
      final Set<URI> discovered = parseGatewayAddresses(gateways);
      updateState(gateways, discovered);
      return new ArrayList<>(discovered);
    } catch (final Exception e) {
      LOG.error("Failed to query gateway address from {}", queryUri, e);
      resetDiscoveryClient();
      if (queryUri.equals(seedServer)) {
        seedServerFailed = true;
      }
      discoverGrpcAddress.remove(queryUri);
      // Fallback: remaining candidates so the LB still has somewhere to send traffic.
      return new ArrayList<>(discoverGrpcAddress);
    }
  }

  @Override
  public int queryGatewayLoad(final URI gateway) throws Exception {
    final URI queryUri = pickQueryUri();
    if (queryUri == null) {
      throw new IllegalStateException("No gateway available for load query");
    }
    if (queryUri.getHost() == null || queryUri.getHost().isBlank()) {
      throw new IllegalStateException("Discovery URI has no host authority: " + queryUri);
    }
    try {
      ensureDiscoveryClient(queryUri);
      final GatewayConnectorInfo join =
          discoveryClient.newQueryGatewayLoadRequest(gateway.toString()).send().join();
      return join.getActiveConnections();
    } catch (final Exception e) {
      LOG.debug("Load query via {} for {} failed", queryUri, gateway, e);
      resetDiscoveryClient();
      if (queryUri.equals(seedServer)) {
        seedServerFailed = true;
      }
      discoverGrpcAddress.remove(queryUri);
      throw e;
    }
  }

  @Override
  public void close() {
    resetDiscoveryClient();
  }

  private Set<URI> parseGatewayAddresses(final List<GatewayInfo> gateways) {
    final Set<URI> discovered = new LinkedHashSet<>();
    for (final GatewayInfo g : gateways) {
      String address = g.getGrpcAddress();
      if (address == null || address.isBlank()) {
        address = g.getGatewayAddress();
      }
      if (address == null || address.isBlank()) {
        continue;
      }
      try {
        final URI uri = URI.create(address);
        if (uri.getHost() != null && !uri.getHost().isBlank()) {
          discovered.add(uri);
        } else {
          LOG.warn("Skipping discovered gateway address with no host: {}", address);
        }
      } catch (final IllegalArgumentException e) {
        LOG.warn("Skipping invalid discovered gateway address: {}", address, e);
      }
    }
    return discovered;
  }

  private void updateState(final List<GatewayInfo> gateways, final Set<URI> discovered) {
    discoverGrpcAddress.clear();
    discoverGrpcAddress.addAll(discovered);
    persistGateways(gateways);
    if (!discovered.isEmpty()) {
      hasSuccessfulDiscovery = true;
    }
  }

  private URI pickQueryUri() {
    if (!hasSuccessfulDiscovery && !seedServerFailed && seedServer != null) {
      return seedServer;
    }
    if (!discoverGrpcAddress.isEmpty()) {
      Collections.shuffle(discoverGrpcAddress);
      return discoverGrpcAddress.getFirst();
    }
    return seedServer;
  }

  private void ensureDiscoveryClient(final URI queryUri) {
    if (discoveryClient != null && currentClientUri != null && currentClientUri.equals(queryUri)) {
      return;
    }
    resetDiscoveryClient();
    discoveryExecutor = new ExecutorResource(Executors.newScheduledThreadPool(1), true);
    final ManagedChannel managedChannel = KunpengClientImpl.buildChannel(configuration, queryUri);
    discoveryClient = new KunpengClientImpl(configuration, managedChannel, discoveryExecutor);
    currentClientUri = queryUri;
  }

  private void resetDiscoveryClient() {
    if (discoveryClient != null) {
      discoveryClient.close();
    }
    if (discoveryExecutor != null) {
      discoveryExecutor.close();
    }
    discoveryExecutor = null;
    discoveryClient = null;
    currentClientUri = null;
  }

  private void persistGateways(final List<GatewayInfo> gateways) {
    try {
      final Map<String, String> toStore = new LinkedHashMap<>();
      for (final GatewayInfo g : gateways) {
        String address = g.getGrpcAddress();
        if (address == null || address.isBlank()) {
          address = g.getGatewayAddress();
        }
        if (address != null && !address.isBlank()) {
          toStore.put(g.getNodeId(), address);
        }
      }
      GatewayInfoPersistedStore.setGatewayInfo(toStore);
      LOG.debug("Persisted {} gateway addresses", toStore.size());
    } catch (final Exception e) {
      LOG.warn("Failed to persist gateway addresses", e);
    }
  }
}
