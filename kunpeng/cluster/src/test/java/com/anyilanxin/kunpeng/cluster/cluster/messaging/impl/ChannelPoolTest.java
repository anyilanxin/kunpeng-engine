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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import io.netty.channel.Channel;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** 验证 {@link ChannelPool} 不会把连接错误地复用到地址已变化的节点上。 */
class ChannelPoolTest {

  private static final String KIND = "rpc";

  /** 每次建连都返回一个全新的活跃 channel。 */
  private final Function<Address, CompletableFuture<Channel>> channelFactory =
      target -> {
        final var fresh = mock(Channel.class);
        when(fresh.isActive()).thenReturn(true);
        return CompletableFuture.completedFuture(fresh);
      };

  private final ChannelPool pool = new ChannelPool(channelFactory);

  private Channel openChannelFor(final String host, final String ip) throws Exception {
    return pool
        .getChannel(new Address(host, 51017, InetAddress.getByName(ip)), KIND)
        .join();
  }

  @Test
  void reopenConnectionWhenHostResolvesToNewIp() throws Exception {
    // given：域名原先解析到 172.16.0.7
    final var firstChannel = openChannelFor("svc.example.internal", "172.16.0.7");

    // when：同一域名换了解析结果
    final var secondChannel = openChannelFor("svc.example.internal", "172.16.0.99");

    // then：不会复用旧 IP 上建立的连接
    assertThat(secondChannel).isNotSameAs(firstChannel);
  }

  @Test
  void reopenConnectionWhenIpIsReassignedToDifferentHost() throws Exception {
    // given：节点 A 占用 172.16.0.7
    final var channelOfA = openChannelFor("node-a.example.internal", "172.16.0.7");

    // when：同一 IP 被另一个主机名复用
    final var channelOfB = openChannelFor("node-b.example.internal", "172.16.0.7");

    // then：两个主机不能共享同一条连接
    assertThat(channelOfB).isNotSameAs(channelOfA);
  }
}
