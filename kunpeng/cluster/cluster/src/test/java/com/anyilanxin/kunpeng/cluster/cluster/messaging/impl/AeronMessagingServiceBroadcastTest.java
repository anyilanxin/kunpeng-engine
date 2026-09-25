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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * MDC 多目的地广播扇出测试：一次群发（组帧/加密一次，驱动扇出）到达全部订阅者。
 *
 * <p>对照逐地址 sendAsync 的行为：载荷一致、每个目的地各收到一份。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class AeronMessagingServiceBroadcastTest {

  private static final String KEY = "0123456789abcdef".repeat(4);
  private static final int TERM_LENGTH = 1024 * 1024;

  private AeronMessagingService nodeA;
  private AeronMessagingService nodeB;
  private AeronMessagingService nodeC;
  private Address addressB;
  private Address addressC;

  @BeforeEach
  void setUp(@TempDir final File tempDir) throws Exception {
    nodeA = startService(new File(tempDir, "a"), Address.from("127.0.0.1", 29131), false);
    addressB = Address.from("127.0.0.1", 29132);
    addressC = Address.from("127.0.0.1", 29133);
    nodeB = startService(new File(tempDir, "b"), addressB, false);
    nodeC = startService(new File(tempDir, "c"), addressC, false);
  }

  @AfterEach
  void tearDown() {
    stopQuietly(nodeA);
    stopQuietly(nodeB);
    stopQuietly(nodeC);
  }

  @Test
  void broadcastReachesAllDestinationsWithSingleOffer() throws Exception {
    final ConcurrentLinkedQueue<byte[]> receivedB = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<byte[]> receivedC = new ConcurrentLinkedQueue<>();
    final CountDownLatch delivered = new CountDownLatch(2);
    nodeB.registerHandler(
        "fanout",
        (BiConsumer<Address, byte[]>)
            (sender, payload) -> {
              receivedB.add(payload);
              delivered.countDown();
            },
        Runnable::run);
    nodeC.registerHandler(
        "fanout",
        (BiConsumer<Address, byte[]>)
            (sender, payload) -> {
              receivedC.add(payload);
              delivered.countDown();
            },
        Runnable::run);

    final byte[] payload = "broadcast-payload".getBytes();
    nodeA
        .sendAsync(List.of(addressB, addressC), "fanout", payload)
        .get(10, TimeUnit.SECONDS);

    assertTrue(delivered.await(10, TimeUnit.SECONDS), "两个目的地都应收到广播");
    assertArrayEquals(payload, receivedB.poll());
    assertArrayEquals(payload, receivedC.poll());
  }

  @Test
  void encryptedBroadcastReachesAllDestinations() throws Exception {
    try (final EncryptedPair pair = encryptedPair()) {
      final CountDownLatch delivered = new CountDownLatch(2);
      pair.nodeB.registerHandler(
          "secret-fanout",
          (BiConsumer<Address, byte[]>)
              (sender, payload) -> delivered.countDown(),
          Runnable::run);
      pair.nodeC.registerHandler(
          "secret-fanout",
          (BiConsumer<Address, byte[]>)
              (sender, payload) -> delivered.countDown(),
          Runnable::run);

      pair.nodeA
          .sendAsync(List.of(pair.addressB, pair.addressC), "secret-fanout", "cipher".getBytes())
          .get(10, TimeUnit.SECONDS);
      assertTrue(delivered.await(10, TimeUnit.SECONDS), "加密广播应到达全部目的地");
    }
  }

  @Test
  void singleDestinationCollectionFallsBackCorrectly() throws Exception {
    final CountDownLatch delivered = new CountDownLatch(1);
    nodeB.registerHandler(
        "single", (BiConsumer<Address, byte[]>) (sender, payload) -> delivered.countDown(),
        Runnable::run);
    nodeA.sendAsync(List.of(addressB), "single", "one".getBytes()).get(10, TimeUnit.SECONDS);
    assertTrue(delivered.await(10, TimeUnit.SECONDS), "单地址集合应正常送达");
  }

  /** 加密开启的 A/B/C 三节点（端口 29134-29136），用于验证一次加密 + MDC 扇出。 */
  private static final class EncryptedPair implements AutoCloseable {
    private final AeronMessagingService nodeA;
    private final AeronMessagingService nodeB;
    private final AeronMessagingService nodeC;
    private final Address addressB;
    private final Address addressC;

    private EncryptedPair(
        final AeronMessagingService nodeA,
        final Address addressB,
        final AeronMessagingService nodeB,
        final Address addressC,
        final AeronMessagingService nodeC) {
      this.nodeA = nodeA;
      this.addressB = addressB;
      this.nodeB = nodeB;
      this.addressC = addressC;
      this.nodeC = nodeC;
    }

    @Override
    public void close() {
      stopQuietly(nodeA);
      stopQuietly(nodeB);
      stopQuietly(nodeC);
    }
  }

  private EncryptedPair encryptedPair() throws Exception {
    final File dir = java.nio.file.Files.createTempDirectory("aeron-broadcast-crypto").toFile();
    final Address a = Address.from("127.0.0.1", 29134);
    final Address b = Address.from("127.0.0.1", 29135);
    final Address c = Address.from("127.0.0.1", 29136);
    return new EncryptedPair(
        startService(new File(dir, "a"), a, true),
        b,
        startService(new File(dir, "b"), b, true),
        c,
        startService(new File(dir, "c"), c, true));
  }

  private AeronMessagingService startService(
      final File aeronDir, final Address address, final boolean crypto) throws Exception {
    final MessagingConfig messagingConfig = new MessagingConfig().setPort(address.port());
    final AeronMessagingConfig aeronConfig =
        new AeronMessagingConfig()
            .setAeronDir(aeronDir)
            .setTermBufferLength(TERM_LENGTH)
            .setDriverThreadingMode("SHARED")
            .setIdleStrategy("sleeping");
    if (crypto) {
      aeronConfig.setCryptoEnabled(true).addCryptoKey(1, KEY);
    }
    final AeronTransport transport = new AeronTransport(address, messagingConfig, aeronConfig);
    final AeronMessagingService service =
        new AeronMessagingService(address, messagingConfig, aeronConfig, transport);
    service.start().get(10, TimeUnit.SECONDS);
    return service;
  }

  private static void stopQuietly(final AeronMessagingService service) {
    if (service != null) {
      final CompletableFuture<Void> stop = service.stop();
      try {
        stop.get(10, TimeUnit.SECONDS);
      } catch (final Exception error) {
        // 关闭失败不掩盖测试断言
      }
    }
  }
}
