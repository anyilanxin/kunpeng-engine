/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver.Listener2;
import io.grpc.NameResolver.ResolutionResult;
import io.grpc.Status;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Direct-drive tests for {@link GatewayDiscoveryNameResolver}: no scheduler involved — tests attach
 * a capturing listener directly and invoke {@code discover()} / {@code queryLoad()} synchronously.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class GatewayDiscoveryNameResolverTest {

  @Test
  void discoverAddsNewGatewaysAndQueriesLoadImmediately() {
    final FakeAdapter adapter =
        new FakeAdapter(
            List.of(URI.create("grpc://gw1:26500"), URI.create("grpc://gw2:26500")),
            uri -> Integer.parseInt(uri.getHost().substring(2)));
    final GatewayDiscoveryNameResolver resolver = newResolver(adapter);
    final CapturingListener listener = new CapturingListener();
    resolver.listener = listener;

    resolver.discover();

    final ResolutionResult result = listener.last.get();
    assertThat(result).isNotNull();
    final List<EquivalentAddressGroup> groups = result.getAddressesOrError().getValue();
    assertThat(groups).hasSize(2);
    final Map<String, EquivalentAddressGroup> byHost = byEagKey(groups);
    assertThat(activeConnections(byHost.get("gw1"))).isEqualTo(1);
    assertThat(activeConnections(byHost.get("gw2"))).isEqualTo(2);
    assertThat(adapter.queried).hasSize(2);
  }

  @Test
  void discoverRemovesStaleGateways() {
    final FakeAdapter adapter =
        new FakeAdapter(
            List.of(URI.create("grpc://gw1:26500"), URI.create("grpc://gw2:26500")),
            uri -> 0);
    final GatewayDiscoveryNameResolver resolver = newResolver(adapter);
    final CapturingListener listener = new CapturingListener();
    resolver.listener = listener;

    resolver.discover();
    adapter.setGateways(List.of(URI.create("grpc://gw1:26500")));
    resolver.discover();

    final List<EquivalentAddressGroup> groups =
        listener.last.get().getAddressesOrError().getValue();
    assertThat(byEagKey(groups)).containsOnlyKeys("gw1");
  }

  @Test
  void loadQueryMarksGatewayOfflineOnFailure() {
    final FakeAdapter adapter =
        new FakeAdapter(
            List.of(URI.create("grpc://good:26500"), URI.create("grpc://bad:26500")),
            uri -> {
              if (uri.getHost().equals("bad")) {
                throw new RuntimeException("gateway down");
              }
              return 7;
            });
    final GatewayDiscoveryNameResolver resolver = newResolver(adapter);
    final CapturingListener listener = new CapturingListener();
    resolver.listener = listener;

    resolver.discover();
    adapter.queried.clear();
    resolver.queryLoad();

    final List<EquivalentAddressGroup> groups =
        listener.last.get().getAddressesOrError().getValue();
    final Map<String, EquivalentAddressGroup> byHost = byEagKey(groups);
    assertThat(online(byHost.get("good"))).isTrue();
    assertThat(online(byHost.get("bad"))).isFalse();
    assertThat(activeConnections(byHost.get("good"))).isEqualTo(7);
  }

  private static GatewayDiscoveryNameResolver newResolver(final FakeAdapter adapter) {
    return new GatewayDiscoveryNameResolver(
        URI.create("gateway://test"),
        null,
        adapter,
        Duration.ofSeconds(10),
        Duration.ofSeconds(10));
  }

  private static Map<String, EquivalentAddressGroup> byEagKey(
      final List<EquivalentAddressGroup> groups) {
    final Map<String, EquivalentAddressGroup> map = new LinkedHashMap<>();
    for (final EquivalentAddressGroup g : groups) {
      final var addr = (java.net.InetSocketAddress) g.getAddresses().getFirst();
      map.put(addr.getHostString(), g);
    }
    return map;
  }

  private static Integer activeConnections(final EquivalentAddressGroup eag) {
    return eag.getAttributes().get(KunpengLoadBalancerAttributes.ACTIVE_CONNECTIONS);
  }

  private static Boolean online(final EquivalentAddressGroup eag) {
    return eag.getAttributes().get(KunpengLoadBalancerAttributes.ONLINE);
  }

  private static final class FakeAdapter implements GatewayServiceDiscoverAdapter {
    private volatile List<URI> gateways;
    private volatile java.util.function.Function<URI, Integer> loadFn;
    final List<URI> queried = new CopyOnWriteArrayList<>();

    FakeAdapter(
        final List<URI> gateways, final java.util.function.Function<URI, Integer> loadFn) {
      this.gateways = gateways;
      this.loadFn = loadFn;
    }

    void setGateways(final List<URI> gateways) {
      this.gateways = gateways;
    }

    @Override
    public List<URI> discoverGateways() {
      return new ArrayList<>(gateways);
    }

    @Override
    public int queryGatewayLoad(final URI gateway) throws Exception {
      queried.add(gateway);
      return loadFn.apply(gateway);
    }

    @Override
    public void close() {}
  }

  private static final class CapturingListener extends Listener2 {
    final AtomicReference<ResolutionResult> last = new AtomicReference<>();

    @Override
    public void onResult(final ResolutionResult resolutionResult) {
      last.set(resolutionResult);
    }

    @Override
    public Status onResult2(final ResolutionResult resolutionResult) {
      last.set(resolutionResult);
      return Status.OK;
    }

    @Override
    public void onError(final Status error) {}
  }
}
