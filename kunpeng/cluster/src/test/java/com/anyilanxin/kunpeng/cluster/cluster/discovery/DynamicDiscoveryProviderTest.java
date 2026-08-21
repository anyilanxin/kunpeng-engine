/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.cluster.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.Node;
import com.anyilanxin.kunpeng.cluster.cluster.NodeId;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEvent.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 验证 {@link DynamicDiscoveryProvider} 的地址解析、节点生成与 JOIN/LEAVE 事件派发。 */
class DynamicDiscoveryProviderTest {

  /** 每个用例创建的 provider，结束后统一离场，避免泄漏线程。 */
  private final List<DynamicDiscoveryProvider> active = new ArrayList<>();

  @AfterEach
  void stopAll() throws Exception {
    for (final DynamicDiscoveryProvider provider : active) {
      provider.leave(null).get(15, TimeUnit.SECONDS);
    }
    active.clear();
  }

  /** 以默认解析器构造一个配置为给定地址、刷新周期 30s 的 provider。 */
  private DynamicDiscoveryProvider joinWith(final String... addresses) throws Exception {
    final var provider = new DynamicDiscoveryProvider(configOf(addresses, Duration.ofSeconds(30)));
    active.add(provider);
    provider.join(null, null).get(15, TimeUnit.SECONDS);
    return provider;
  }

  private static DynamicDiscoveryConfig configOf(final String[] addresses, final Duration refresh) {
    return new DynamicDiscoveryConfig().setAddresses(List.of(addresses)).setRefreshInterval(refresh);
  }

  @Test
  void discoversNodeFromLocalhostWithExplicitPort() throws Exception {
    // when：以 localhost:26500 作为初始地址加入
    final var provider = joinWith("localhost:26500");

    // then：解析出一个端口为 26500 的节点
    Awaitility.await().untilAsserted(() -> assertThat(provider.getNodes()).hasSize(1));
    assertThat(provider.getNodes()).allMatch(n -> n.address().port() == 26500);
  }

  @Test
  void appliesDefaultPortWhenAddressOmitsPort() throws Exception {
    // when：地址不带端口
    final var provider = joinWith("localhost");

    // then：节点端口落到默认端口 26502
    Awaitility.await()
        .untilAsserted(() -> assertThat(provider.getNodes()).isNotEmpty());
    assertThat(provider.getNodes()).allMatch(n -> n.address().port() == 26502);
  }

  @Test
  void discoversEachProvidedAddress() throws Exception {
    // when：提供两个地址
    final var provider = joinWith("localhost:26500", "127.0.0.1:26501");

    // then：两个地址各自产生节点
    Awaitility.await()
        .untilAsserted(() -> assertThat(provider.getNodes()).hasSizeGreaterThanOrEqualTo(2));
  }

  @Test
  void expandsNodePerResolvedInetAddress() throws Exception {
    // given：模拟域名解析出两个 A 记录
    final Function<String, List<InetAddress>> resolver =
        host -> resolveAll("10.20.30.40", "10.20.30.41");
    final var provider = new DynamicDiscoveryProvider(configOf(new String[] {"multi.acme.test"}, Duration.ofSeconds(30)), resolver);
    active.add(provider);

    // when
    provider.join(null, null);

    // then：每个解析结果生成一个节点，host 即解析出的 IP
    Awaitility.await()
        .untilAsserted(() -> assertThat(provider.getNodes()).hasSize(2));
    assertThat(provider.getNodes())
        .extracting(n -> n.address().host())
        .containsExactlyInAnyOrder("10.20.30.40", "10.20.30.41");
  }

  @Test
  void derivesNodeIdFromAddress() throws Exception {
    // when
    final var provider = joinWith("127.0.0.1:26500");

    // then：节点 id 就是地址原文
    Awaitility.await()
        .untilAsserted(() -> assertThat(provider.getNodes()).hasSize(1));
    final Set<Node> discovered = provider.getNodes();
    assertThat(discovered).first().extracting(Node::id).isEqualTo(NodeId.from("127.0.0.1:26500"));
  }

  @Test
  void emitsOnlyJoinEventsForStableNodes() throws Exception {
    // given：记录所有事件
    final var provider = joinWith("localhost:26500");
    final List<NodeDiscoveryEvent> seen = new CopyOnWriteArrayList<>();
    provider.addListener(seen::add);

    // when：等待刷新周期带来事件
    Awaitility.await()
        .atMost(Duration.ofSeconds(6))
        .untilAsserted(() -> assertThat(seen).isNotEmpty());

    // then：稳定节点只会产生 JOIN 事件
    assertThat(seen).allMatch(e -> e.type() == Type.JOIN);
  }

  @Test
  void emitsLeaveEventWhenNodeDisappears() throws Exception {
    // given：解析器第一次返回地址、之后返回空，模拟节点下线
    final var calls = new AtomicInteger();
    final Function<String, List<InetAddress>> resolver =
        host -> calls.getAndIncrement() == 0 ? resolveAll("10.30.50.70") : List.of();
    final var provider =
        new DynamicDiscoveryProvider(
            configOf(new String[] {"vanishing.acme.test"}, Duration.ofSeconds(1)), resolver);
    active.add(provider);
    final List<NodeDiscoveryEvent> seen = new CopyOnWriteArrayList<>();
    provider.addListener(seen::add);

    // when：加入并等待刷新
    provider.join(null, null).get(15, TimeUnit.SECONDS);
    Awaitility.await()
        .atMost(Duration.ofSeconds(6))
        .untilAsserted(() -> assertThat(provider.getNodes()).isEmpty());

    // then：观察到了 LEAVE 事件
    assertThat(seen).anyMatch(e -> e.type() == Type.LEAVE);
  }

  @Test
  void skipsUnresolvableAddressWithoutFailingOthers() throws Exception {
    // when：混入一个无法解析的地址
    final var provider = joinWith("nonexistent.invalid.acme:26500", "localhost:26500");

    // then：坏地址被跳过，localhost 仍被发现
    Awaitility.await()
        .untilAsserted(() -> assertThat(provider.getNodes()).hasSize(1));
  }

  @Test
  void retainsConfiguredValuesOnConfigObject() {
    // given / when：构造带自定义刷新周期的配置
    final Duration refresh = Duration.ofMinutes(3);
    final var config = configOf(new String[] {"localhost:26500"}, refresh);
    final var provider = new DynamicDiscoveryProvider(config);
    active.add(provider);

    // then：配置原样保留
    assertThat(config.getAddresses()).containsExactly("localhost:26500");
    assertThat(config.getRefreshInterval()).isEqualTo(refresh);
  }

  @Test
  void createsProviderViaTypeFactory() {
    // given
    final var config = configOf(new String[] {"localhost:26500"}, Duration.ofSeconds(30));

    // when：通过 TYPE 工厂创建
    final NodeDiscoveryProvider created = DynamicDiscoveryProvider.TYPE.newProvider(config);
    active.add((DynamicDiscoveryProvider) created);

    // then：实例类型与配置均正确
    assertThat(created).isInstanceOf(DynamicDiscoveryProvider.class);
    assertThat(created.config()).isEqualTo(config);
  }

  /** 把若干 IP 字面量转换为 InetAddress 列表。 */
  private static List<InetAddress> resolveAll(final String... ips) {
    try {
      final var out = new ArrayList<InetAddress>(ips.length);
      for (final String ip : ips) {
        out.add(InetAddress.getByName(ip));
      }
      return out;
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }
}
