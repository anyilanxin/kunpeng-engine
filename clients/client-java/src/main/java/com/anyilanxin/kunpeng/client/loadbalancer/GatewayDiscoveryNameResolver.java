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
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.StatusOr;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Dynamic-discovery name resolver: runs two scheduled cycles on a single-thread executor.
 *
 * <ul>
 *   <li><b>Discovery</b> ({@code gatewayDiscover}): asks the cluster for the current gateway list,
 *       reconciles the local state map, and fires an immediate load query for any newly seen
 *       gateway so it doesn't have to wait for the next load tick.
 *   <li><b>Load</b> ({@code gatewayLoadQuery}): re-queries every known gateway; failures flip the
 *       gateway's {@code online} flag so the load balancer skips it until the next success.
 * </ul>
 *
 * <p>The first discovery fires at {@code t=0}; the first standalone load fires at {@code t=
 * gatewayLoadQuery} (skipped on startup because discovery already triggered one). All state
 * mutations happen on the executor thread, so no locking is needed.
 */
public class GatewayDiscoveryNameResolver extends NameResolver {
  private static final Logger LOG = ClientLoggers.GATEWAY_DISCOVER_LOGGER;

  private final URI targetUri;
  private final GatewayServiceDiscoverAdapter addressAdapter;
  private final Duration gatewayDiscover;
  private final Duration gatewayLoadQuery;
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            final Thread t = new Thread(r, "kunpeng-gateway-discover");
            t.setDaemon(true);
            return t;
          });
  // Bounded only by gateway count — daemons, idle between cycles. Lets one slow/dead gateway's
  // timeout overlap with the others instead of stacking N × timeout per cycle.
  private final ExecutorService queryPool =
      Executors.newCachedThreadPool(
          r -> {
            final Thread t = new Thread(r, "kunpeng-gateway-discover-query");
            t.setDaemon(true);
            return t;
          });
  private final Map<URI, GatewayLoadState> discovered = new LinkedHashMap<>();

  private ScheduledFuture<?> discoverTask;
  private ScheduledFuture<?> loadTask;
  // Package-private so tests in the same package can drive cycles without invoking the scheduler.
  Listener2 listener;

  public GatewayDiscoveryNameResolver(
      final URI targetUri,
      final Args args,
      final GatewayServiceDiscoverAdapter addressAdapter,
      final Duration gatewayDiscover,
      final Duration gatewayLoadQuery) {
    this.targetUri = targetUri;
    this.addressAdapter = addressAdapter;
    this.gatewayDiscover = gatewayDiscover;
    this.gatewayLoadQuery = gatewayLoadQuery;
  }

  @Override
  public String getServiceAuthority() {
    return targetUri.getAuthority();
  }

  @Override
  public void start(final Listener2 listener) {
    this.listener = listener;
    final long discoverSec = Math.max(1L, gatewayDiscover.getSeconds());
    final long loadSec = Math.max(1L, gatewayLoadQuery.getSeconds());
    discoverTask =
        executor.scheduleAtFixedRate(this::safeDiscover, 0L, discoverSec, TimeUnit.SECONDS);
    // Skip the immediate load tick — the t=0 discovery already triggers one for fresh gateways.
    loadTask =
        executor.scheduleAtFixedRate(this::safeQueryLoad, loadSec, loadSec, TimeUnit.SECONDS);
  }

  private void safeDiscover() {
    try {
      discover();
    } catch (final Exception e) {
      LOG.error("Gateway discovery cycle failed", e);
    }
  }

  private void safeQueryLoad() {
    try {
      queryLoad();
    } catch (final Exception e) {
      LOG.error("Gateway load query cycle failed", e);
    }
  }

  /** Refresh the gateway list; immediately query load for any new gateway we haven't seen. */
  void discover() {
    final List<URI> gateways = addressAdapter.discoverGateways();
    final Set<URI> seen = new HashSet<>(gateways.size());
    final List<URI> fresh = new ArrayList<>();
    for (final URI uri : gateways) {
      seen.add(uri);
      if (!discovered.containsKey(uri)) {
        discovered.put(uri, new GatewayLoadState());
        fresh.add(uri);
      }
    }
    final var stale = new ArrayList<URI>();
    for (final URI known : discovered.keySet()) {
      if (!seen.contains(known)) {
        stale.add(known);
      }
    }
    for (final URI uri : stale) {
      discovered.remove(uri);
    }
    if (!fresh.isEmpty()) {
      queryLoadFor(fresh);
    }
    pushAddresses();
  }

  /** Re-query every known gateway; failures flip the gateway offline. */
  void queryLoad() {
    if (discovered.isEmpty()) {
      return;
    }
    queryLoadFor(new ArrayList<>(discovered.keySet()));
    pushAddresses();
  }

  private void queryLoadFor(final List<URI> uris) {
    if (uris.isEmpty()) {
      return;
    }
    final List<CompletableFuture<Void>> futures = new ArrayList<>(uris.size());
    for (final URI uri : uris) {
      final GatewayLoadState state = discovered.get(uri);
      if (state == null) {
        continue;
      }
      futures.add(
          CompletableFuture.runAsync(
              () -> {
                try {
                  final int active = addressAdapter.queryGatewayLoad(uri);
                  state.activeConnections = active;
                  state.online = true;
                } catch (final Exception e) {
                  LOG.warn("Gateway {} load query failed, marking offline", uri, e);
                  state.online = false;
                }
              },
              queryPool));
    }
    if (futures.isEmpty()) {
      return;
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  private void pushAddresses() {
    if (listener == null || discovered.isEmpty()) {
      return;
    }
    final List<EquivalentAddressGroup> groups = new ArrayList<>(discovered.size());
    for (final Map.Entry<URI, GatewayLoadState> entry : discovered.entrySet()) {
      final URI uri = entry.getKey();
      final GatewayLoadState state = entry.getValue();
      final SocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
      final Attributes attrs =
          Attributes.newBuilder()
              .set(KunpengLoadBalancerAttributes.ACTIVE_CONNECTIONS, state.activeConnections)
              .set(KunpengLoadBalancerAttributes.ONLINE, state.online)
              .build();
      groups.add(new EquivalentAddressGroup(socketAddress, attrs));
    }
    listener.onResult(
        ResolutionResult.newBuilder().setAddressesOrError(StatusOr.fromValue(groups)).build());
  }

  @Override
  public void shutdown() {
    if (discoverTask != null) {
      discoverTask.cancel(false);
      discoverTask = null;
    }
    if (loadTask != null) {
      loadTask.cancel(false);
      loadTask = null;
    }
    executor.shutdownNow();
    queryPool.shutdownNow();
    try {
      addressAdapter.close();
    } catch (final Exception e) {
      LOG.warn("Failed to close gateway service discover adapter", e);
    }
    discovered.clear();
  }
}
