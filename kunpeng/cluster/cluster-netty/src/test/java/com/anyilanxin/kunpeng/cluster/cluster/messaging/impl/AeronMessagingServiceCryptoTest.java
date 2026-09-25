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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 传输加密开启后的 RPC 语义：正常往返、无密钥对端不可读（密文即丢弃）、密钥轮换窗口兼容。
 *
 * <p>加密位于 {@link AeronTransport} 收发咽喉，对上层服务完全透明——本测试不改动任何业务调用方式。
 */
final class AeronMessagingServiceCryptoTest {

  private static final String KEY_V1 = "0123456789abcdef".repeat(4);
  private static final String KEY_V2 = "fedcba9876543210".repeat(4);
  /** 256KiB 大帧需要 term ≥ 2MB(1.48+ 单条上限 = term/8), 取 4MB(上限 512KiB)。 */
  private static final int TERM_LENGTH = 4 * 1024 * 1024;

  private AeronMessagingService nodeA;
  private AeronMessagingService nodeB;

  @BeforeEach
  void setUp(@TempDir final File tempDir) throws Exception {
    // nodeA/nodeB 均持有两个密钥世代; nodeA 显式固定用世代 1 发送(轮换分发期), nodeB 默认取最大世代 2
    nodeA =
        startService(
            new File(tempDir, "a"),
            Address.from("127.0.0.1", 29121),
            cryptoConfig().addCryptoKey(2, KEY_V2).setSendCryptoKeyId(1));
    nodeB =
        startService(
            new File(tempDir, "b"),
            Address.from("127.0.0.1", 29122),
            cryptoConfig().addCryptoKey(2, KEY_V2));
  }

  @AfterEach
  void tearDown() {
    stopQuietly(nodeA);
    stopQuietly(nodeB);
  }

  @Test
  void encryptedRoundTripPreservesRpcSemantics(@TempDir final File tempDir) throws Exception {
    nodeB.registerHandler(
        "echo",
        (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
            (sender, payload) -> CompletableFuture.completedFuture(payload));

    final byte[] payload = new byte[256 * 1024];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i * 31);
    }
    final byte[] response =
        nodeA
            .sendAndReceive(Address.from("127.0.0.1", 29122), "echo", payload)
            .get(30, TimeUnit.SECONDS);
    assertArrayEquals(payload, response, "加密往返(含 256KiB 大帧)应保持字节一致");
  }

  @Test
  void encryptedOneWayIsDelivered() throws Exception {
    final CountDownLatch delivered = new CountDownLatch(1);
    nodeB.registerHandler(
        "one-way",
        (BiConsumer<Address, byte[]>) (sender, payload) -> delivered.countDown(),
        Runnable::run);
    nodeA
        .sendAsync(Address.from("127.0.0.1", 29122), "one-way", "fire".getBytes())
        .get(10, TimeUnit.SECONDS);
    assertTrue(delivered.await(10, TimeUnit.SECONDS), "加密单向消息应当在对端被消费");
  }

  @Test
  void peerWithoutKeyCannotReadAndSenderTimesOut(@TempDir final File tempDir) throws Exception {
    // 无密钥节点: 收到 0xAE 信封按未知帧丢弃, 处理器永不被调用
    final AeronMessagingService plainNode =
        startService(
            new File(tempDir, "plain"),
            Address.from("127.0.0.1", 29123),
            new AeronMessagingConfig()
                .setAeronDir(new File(tempDir, "plain"))
                .setTermBufferLength(TERM_LENGTH)
                .setDriverThreadingMode("SHARED")
                .setIdleStrategy("sleeping"));
    final CountDownLatch leaked = new CountDownLatch(1);
    try {
      plainNode.registerHandler(
          "echo",
          (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
              (sender, payload) -> {
                leaked.countDown();
                return CompletableFuture.completedFuture(payload);
              });

      final ExecutionException error =
          assertThrowsExecution(
              () ->
                  nodeA
                      .sendAndReceive(
                          Address.from("127.0.0.1", 29123),
                          "echo",
                          "secret".getBytes(),
                          true,
                          Duration.ofSeconds(2))
                      .get(10, TimeUnit.SECONDS));
      assertInstanceOf(TimeoutException.class, error.getCause(), "无密钥对端无法应答, 应按超时失败");
      assertFalse(leaked.await(1, TimeUnit.SECONDS), "无密钥节点的处理器不应被触发");
    } finally {
      stopQuietly(plainNode);
    }
  }

  @Test
  void rotationWindowSupportsMixedGenerations(@TempDir final File tempDir) throws Exception {
    // upgraded 持有 {1,2} 且默认用世代 2 发送(已切换); lagging 只有世代 1(未完成轮换)
    final AeronMessagingService upgraded =
        startService(
            new File(tempDir, "up"),
            Address.from("127.0.0.1", 29124),
            cryptoConfig().addCryptoKey(2, KEY_V2));
    final AeronMessagingService lagging =
        startService(
            new File(tempDir, "lag"),
            Address.from("127.0.0.1", 29125),
            cryptoConfig()); // 仅世代 1
    try {
      upgraded.registerHandler(
          "echo",
          (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
              (sender, payload) -> CompletableFuture.completedFuture(payload));
      lagging.registerHandler(
          "echo",
          (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
              (sender, payload) -> CompletableFuture.completedFuture(payload));
      nodeA.registerHandler(
          "echo",
          (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
              (sender, payload) -> CompletableFuture.completedFuture(payload));

      // 分发期: nodeA 仍以世代 1 发送 → 持有两世代的 upgraded 与仅世代 1 的 lagging 都可读
      assertArrayEquals(
          "ok".getBytes(),
          nodeA
              .sendAndReceive(Address.from("127.0.0.1", 29124), "echo", "ok".getBytes())
              .get(10, TimeUnit.SECONDS));
      assertArrayEquals(
          "legacy".getBytes(),
          nodeA
              .sendAndReceive(Address.from("127.0.0.1", 29125), "echo", "legacy".getBytes())
              .get(10, TimeUnit.SECONDS));

      // 切换后: upgraded 以世代 2 发送 → 已持有新世代的 nodeA 正常互通
      assertArrayEquals(
          "new-gen".getBytes(),
          upgraded
              .sendAndReceive(Address.from("127.0.0.1", 29121), "echo", "new-gen".getBytes())
              .get(10, TimeUnit.SECONDS));

      // 切换后: upgraded(世代 2) → lagging(仅世代 1) 不可读
      final ExecutionException error =
          assertThrowsExecution(
              () ->
                  upgraded
                      .sendAndReceive(
                          Address.from("127.0.0.1", 29125),
                          "echo",
                          "drifted".getBytes(),
                          true,
                          Duration.ofSeconds(2))
                      .get(10, TimeUnit.SECONDS));
      assertInstanceOf(TimeoutException.class, error.getCause(), "滞后节点无法解密新世代密文");
    } finally {
      stopQuietly(upgraded);
      stopQuietly(lagging);
    }
  }

  private static AeronMessagingConfig cryptoConfig() {
    return new AeronMessagingConfig()
        .setTermBufferLength(TERM_LENGTH)
        .setDriverThreadingMode("SHARED")
        .setIdleStrategy("sleeping")
        .setCryptoEnabled(true)
        .addCryptoKey(1, KEY_V1);
  }

  private AeronMessagingService startService(
      final File aeronDir, final Address address, final AeronMessagingConfig aeronConfig)
      throws Exception {
    final MessagingConfig messagingConfig = new MessagingConfig().setPort(address.port());
    aeronConfig.setAeronDir(aeronDir);
    final AeronTransport transport = new AeronTransport(address, messagingConfig, aeronConfig);
    final AeronMessagingService service =
        new AeronMessagingService(address, messagingConfig, aeronConfig, transport);
    service.start().get(10, TimeUnit.SECONDS);
    return service;
  }

  private static void stopQuietly(final AeronMessagingService service) {
    if (service != null) {
      try {
        service.stop().get(10, TimeUnit.SECONDS);
      } catch (final Exception error) {
        // 关闭失败不掩盖测试断言
      }
    }
  }

  private static ExecutionException assertThrowsExecution(
      final java.util.concurrent.Callable<?> action) {
    try {
      action.call();
    } catch (final ExecutionException error) {
      return error;
    } catch (final Exception error) {
      throw new AssertionError("应当以 ExecutionException 失败, 实际: " + error, error);
    }
    throw new AssertionError("应当以 ExecutionException 失败");
  }
}
