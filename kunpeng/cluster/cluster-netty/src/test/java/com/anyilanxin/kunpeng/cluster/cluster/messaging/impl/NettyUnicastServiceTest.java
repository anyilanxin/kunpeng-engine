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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ManagedUnicastService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.cluster.utils.SocketUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jodah.concurrentunit.ConcurrentTestCase;
import org.agrona.CloseHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;

/** Netty unicast service test. */
public class NettyUnicastServiceTest extends ConcurrentTestCase {
  ManagedUnicastService service1;
  ManagedUnicastService service2;
  Address address1;
  Address address2;
  @AutoClose private final MeterRegistry registry = new SimpleMeterRegistry();

  @Test
  public void testUnicast() throws Exception {
    service1.addListener(
        "test",
        (address, payload) -> {
          assertThat(address).isEqualTo(address2);
          assertThat(payload).containsExactly("Hello world!".getBytes());
          resume();
        });

    service2.unicast(address1, "test", "Hello world!".getBytes());
    await(5000);
  }

  @Test
  public void shouldNotThrowExceptionWhenServiceStopped() {
    // given
    service2.stop();

    // when - then
    assertThatCode(() -> service2.unicast(address1, "test", "Hello world!".getBytes()))
        .doesNotThrowAnyException();
  }

  @Test
  public void shouldIgnoreMalformedUnicastPackets() throws Exception {
    // given：注册 listener，用于断言恶意报文不会被投递
    final AtomicInteger malformedDeliveryCount = new AtomicInteger();
    service1.addListener(
        "malformed", (address, payload) -> malformedDeliveryCount.incrementAndGet());

    final int preamble = "testClusterId".hashCode();
    final var target = new InetSocketAddress("127.0.0.1", address1.port());
    // 三类恶意报文：前导码正确但 length 超大 / length 为负 / 报文过短（不足 8 字节）
    final List<byte[]> malformedPackets =
        List.of(
            ByteBuffer.allocate(8).putInt(preamble).putInt(Integer.MAX_VALUE).array(),
            ByteBuffer.allocate(8).putInt(preamble).putInt(-1).array(),
            ByteBuffer.allocate(4).putInt(preamble).array());

    // when：向真实 unicast 端口发送恶意 UDP 报文
    try (final DatagramSocket socket = new DatagramSocket()) {
      for (final byte[] data : malformedPackets) {
        socket.send(new DatagramPacket(data, data.length, target));
      }
    }

    // 先发一条合法消息并等待送达，证明接收端已处理完先入站的恶意报文，
    // 之后的零投递断言才有效（否则慢机上可能只是还没轮询到恶意报文）
    final CountDownLatch validDelivered = new CountDownLatch(1);
    service1.addListener("still-alive", (address, payload) -> validDelivered.countDown());
    service2.unicast(address1, "still-alive", "still-alive".getBytes());
    assertThat(validDelivered.await(5, TimeUnit.SECONDS)).isTrue();

    // then：恶意报文被安全丢弃，listener 未收到任何投递
    assertThat(malformedDeliveryCount.get()).isZero();
  }

  @Before
  public void setUp() throws Exception {
    address1 = Address.from("127.0.0.1", SocketUtil.getNextAddress().getPort());
    address2 = Address.from("127.0.0.1", SocketUtil.getNextAddress().getPort());

    final String clusterId = "testClusterId";
    service1 =
        new NettyUnicastService(clusterId, address1, new MessagingConfig(), registry);
    service1.start().join();

    service2 =
        new NettyUnicastService(clusterId, address2, new MessagingConfig(), registry);
    service2.start().join();
  }

  @After
  public void tearDown() throws Exception {
    CloseHelper.quietCloseAll(() -> service1.stop().join(), () -> service2.stop().join());
  }
}
