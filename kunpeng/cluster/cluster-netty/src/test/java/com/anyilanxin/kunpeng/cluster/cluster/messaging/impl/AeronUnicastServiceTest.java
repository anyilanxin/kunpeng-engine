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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Aeron 单播（UnicastService 替身）语义测试。 */
final class AeronUnicastServiceTest {

  private static final Address NODE1 = Address.from("127.0.0.1", 29151);
  private static final Address NODE2 = Address.from("127.0.0.1", 29152);

  private AeronUnicastService node1;
  private AeronUnicastService node2;

  @BeforeEach
  void setUp(@TempDir final File tempDir) throws Exception {
    node1 = start(new File(tempDir, "node1"), NODE1);
    node2 = start(new File(tempDir, "node2"), NODE2);
  }

  @AfterEach
  void tearDown() {
    stopQuietly(node1);
    stopQuietly(node2);
  }

  @Test
  void unicastDeliversToListener() throws Exception {
    final CountDownLatch delivered = new CountDownLatch(1);
    final AtomicReference<byte[]> received = new AtomicReference<>();
    node2.addListener(
        "gossip-topic",
        (sender, payload) -> {
          received.set(payload);
          delivered.countDown();
        },
        Runnable::run);

    final byte[] payload = "unicast-body".getBytes();
    node1.unicast(NODE2, "gossip-topic", payload);

    assertTrue(delivered.await(10, TimeUnit.SECONDS), "单播消息应当被对端监听器消费");
    assertArrayEquals(payload, received.get());
  }

  @Test
  void unicastWorksInBothDirections() throws Exception {
    final CountDownLatch deliveredToNode1 = new CountDownLatch(1);
    node1.addListener(
        "reverse", (sender, payload) -> deliveredToNode1.countDown(), Runnable::run);

    node2.unicast(NODE1, "reverse", new byte[] {9});

    assertTrue(deliveredToNode1.await(10, TimeUnit.SECONDS), "反向单播消息应当被消费");
  }

  private AeronUnicastService start(final File aeronDir, final Address address) throws Exception {
    final var aeronConfig =
        new AeronMessagingConfig()
            .setAeronDir(aeronDir)
            .setTermBufferLength(1024 * 1024)
            .setDriverThreadingMode("SHARED")
            .setIdleStrategy("sleeping");
    final var transport =
        new AeronTransport(address, new MessagingConfig().setPort(address.port()), aeronConfig);
    final var service = new AeronUnicastService(address, aeronConfig, transport);
    service.start().get(10, TimeUnit.SECONDS);
    return service;
  }

  private static void stopQuietly(final AeronUnicastService service) {
    if (service != null) {
      try {
        service.stop().get(10, TimeUnit.SECONDS);
      } catch (final Exception error) {
        // 关闭失败不掩盖测试断言
      }
    }
  }
}
