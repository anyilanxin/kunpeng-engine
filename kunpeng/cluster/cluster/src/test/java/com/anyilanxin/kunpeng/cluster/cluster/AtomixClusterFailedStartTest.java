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
package com.anyilanxin.kunpeng.cluster.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryProvider;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.UnicastService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.TestMessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.TestUnicastService;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 集群启动失败后的回滚。
 *
 * <p>{@link AtomixCluster} 和 leader 发现服务在构造时就各建了一条非守护线程。启动链失败后调用方只拿到失败（Spring 的工厂方法此时抛出，实例不会注册成
 * bean，销毁方法不会被调用），不回滚的话这两条线程和失败前已启动的服务都留下，启动失败的进程永远不退出。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("集群启动失败回滚")
class AtomixClusterFailedStartTest {

  private static final String MEMBER_ID = "failed-start";

  @Test
  @Timeout(60)
  @DisplayName("集群端口被占用导致启动失败后，构造时建的两条线程都结束")
  void threadsCreatedByTheConstructorTerminateWhenStartFails() throws Exception {
    // 占住一个 UDP 端口且不复用地址：集群消息服务绑定同一地址时失败
    try (final var holder = new DatagramSocket(null)) {
      holder.setReuseAddress(false);
      holder.bind(new InetSocketAddress("127.0.0.1", 0));
      final int port = holder.getLocalPort();
      final var address = Address.from("127.0.0.1", port);
      final Set<Thread> threadsBefore = clusterThreads();
      final var cluster =
          AtomixCluster.builder(new ClusterConfig(), new SimpleMeterRegistry())
              .withMemberId(MEMBER_ID)
              .withHost("127.0.0.1")
              .withPort(port)
              .withMembershipProvider(
                  new BootstrapDiscoveryProvider(
                      Node.builder().withId(MEMBER_ID).withAddress(address).build()))
              .build();
      final Set<Thread> createdThreads = clusterThreads();
      createdThreads.removeAll(threadsBefore);
      assertThat(createdThreads)
          .extracting(Thread::getName)
          .containsExactlyInAnyOrder("atomix-cluster-0", "atomix-cluster-leader-found-0");

      assertThatThrownBy(() -> cluster.start().join())
          .isInstanceOf(CompletionException.class)
          .hasMessageContaining("Address already in use");

      assertTerminated(createdThreads);
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("启动链中途失败时，失败前已启动的服务被停掉，线程同样结束")
  void servicesStartedBeforeTheFailureAreStoppedOnRollback() throws Exception {
    final var address = Address.from("127.0.0.1", 5000);
    final var config =
        new ClusterConfig()
            .setClusterId("rollback-test")
            .setNodeConfig(new MemberConfig().setId(MEMBER_ID).setAddress(address))
            .setMessagingConfig(new MessagingConfig().setPort(address.port()))
            .setProtocolConfig(new SwimMembershipProtocolConfig());
    final var messagingService = new RecordingMessagingService(address);
    final Set<Thread> threadsBefore = clusterThreads();
    // 第一步（消息服务）启动成功，第二步（单播服务）失败
    final var cluster =
        new AtomixCluster(
            config,
            Version.from("1.0.0"),
            messagingService,
            new FailingUnicastService(address),
            new SimpleMeterRegistry()) {};
    final Set<Thread> createdThreads = clusterThreads();
    createdThreads.removeAll(threadsBefore);

    assertThatThrownBy(() -> cluster.start().join())
        .isInstanceOf(CompletionException.class)
        .hasMessageContaining("unicast service failed to start");

    assertThat(messagingService.stopped).as("失败前已启动的消息服务应被停掉").isTrue();
    assertTerminated(createdThreads);
  }

  /** 当前存活的集群线程（本类的上下文线程与 leader 发现服务的上下文线程） */
  private static Set<Thread> clusterThreads() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(thread -> thread.isAlive() && thread.getName().startsWith("atomix-cluster"))
        .collect(Collectors.toSet());
  }

  private static void assertTerminated(final Set<Thread> threads) throws InterruptedException {
    for (final var thread : threads) {
      // 启动 future 是在集群线程上完成的，那一刻线程本身还在收尾
      thread.join(TimeUnit.SECONDS.toMillis(30));
      assertThat(thread.isAlive()).as("启动失败后线程 %s 应结束", thread.getName()).isFalse();
    }
  }

  /** 记录 stop 是否被调用 */
  private static final class RecordingMessagingService extends TestMessagingService {
    private final AtomicBoolean stopped = new AtomicBoolean();

    RecordingMessagingService(final Address address) {
      super(address, new ConcurrentHashMap<>());
    }

    @Override
    public CompletableFuture<Void> stop() {
      stopped.set(true);
      return super.stop();
    }
  }

  /** 启动即失败的单播服务 */
  private static final class FailingUnicastService extends TestUnicastService {
    FailingUnicastService(final Address address) {
      super(address, new ConcurrentHashMap<>());
    }

    @Override
    public CompletableFuture<UnicastService> start() {
      return CompletableFuture.failedFuture(
          new IllegalStateException("unicast service failed to start"));
    }
  }
}
