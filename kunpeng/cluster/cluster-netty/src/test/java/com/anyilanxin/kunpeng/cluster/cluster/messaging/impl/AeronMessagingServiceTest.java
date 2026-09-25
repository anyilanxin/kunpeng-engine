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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Aeron RPC 传输语义测试：请求-回复、单向、失败语义、超时与大报文分片。 */
final class AeronMessagingServiceTest {

  private static final Address NODE1 = Address.from("127.0.0.1", 29101);
  private static final Address NODE2 = Address.from("127.0.0.1", 29102);

  private AeronMessagingService node1;
  private AeronMessagingService node2;

  @BeforeEach
  void setUp(@TempDir final File tempDir) throws Exception {
    node1 = startService(new File(tempDir, "node1"), NODE1);
    node2 = startService(new File(tempDir, "node2"), NODE2);
  }

  @AfterEach
  void tearDown() {
    stopQuietly(node1);
    stopQuietly(node2);
  }

  @Test
  void requestReplyRoundTripWithAsyncHandler() throws Exception {
    node2.registerHandler(
        "echo",
        (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
            (sender, payload) -> CompletableFuture.completedFuture(payload));

    final byte[] payload = "hello-aeron".getBytes();
    final byte[] response = node1.sendAndReceive(NODE2, "echo", payload).get(10, TimeUnit.SECONDS);

    assertArrayEquals(payload, response);
  }

  @Test
  void sendAsyncDeliversToOneWayHandler() throws Exception {
    final CountDownLatch delivered = new CountDownLatch(1);
    node2.registerHandler(
        "fire-and-forget",
        (BiConsumer<Address, byte[]>) (sender, payload) -> delivered.countDown(),
        Runnable::run);

    final byte[] payload = "one-way".getBytes();
    node1.sendAsync(NODE2, "fire-and-forget", payload).get(10, TimeUnit.SECONDS);

    assertTrue(delivered.await(10, TimeUnit.SECONDS), "单向消息应当在对端被消费");
  }

  @Test
  void noRemoteHandlerFailsWithSpecificException() {
    final ExecutionException error =
        assertThrowsExecution(
            () -> node1.sendAndReceive(NODE2, "no-such-subject", new byte[] {1}).get(10, TimeUnit.SECONDS));
    assertInstanceOf(MessagingException.NoRemoteHandler.class, error.getCause());
  }

  @Test
  void requestTimesOutWhenHandlerNeverReplies() {
    node2.registerHandler(
        "silent",
        (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
            (sender, payload) -> new CompletableFuture<>());

    final ExecutionException error =
        assertThrowsExecution(
            () ->
                node1
                    .sendAndReceive(NODE2, "silent", new byte[] {1}, true, Duration.ofMillis(500))
                    .get(10, TimeUnit.SECONDS));
    assertInstanceOf(TimeoutException.class, error.getCause());
  }

  @Test
  void handlerFailureMapsToRemoteHandlerFailure() {
    node2.registerHandler(
        "boom",
        (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
            (sender, payload) -> CompletableFuture.failedFuture(new IllegalStateException("处理失败")));

    final ExecutionException error =
        assertThrowsExecution(
            () -> node1.sendAndReceive(NODE2, "boom", new byte[] {1}).get(10, TimeUnit.SECONDS));
    assertInstanceOf(MessagingException.RemoteHandlerFailure.class, error.getCause());
  }

  @Test
  void snapshotSizedChunksStreamWithinWindow(@TempDir final File tempDir) throws Exception {
    // 回归: 驱动默认流控窗口仅 128KB, 4MiB(Raft 快照块默认尺寸)流水线传输会退化为逐块串行等待(约 1-2MiB/s)。
    // 传输层已把窗口对齐单条上限(term/8), 64MiB 总量应在秒级完成而非分钟级。
    final int termLength = 64 * 1024 * 1024;
    final AeronMessagingService chunk1 =
        startService(new File(tempDir, "chunk1"), Address.from("127.0.0.1", 29113), termLength);
    final AeronMessagingService chunk2 =
        startService(new File(tempDir, "chunk2"), Address.from("127.0.0.1", 29114), termLength);
    try {
      final AtomicLong receivedBytes = new AtomicLong();
      chunk2.registerHandler(
          "snapshot-chunk",
          (BiConsumer<Address, byte[]>) (sender, payload) -> receivedBytes.addAndGet(payload.length),
          Runnable::run);
      final byte[] chunk = new byte[4 * 1024 * 1024];
      for (int i = 0; i < chunk.length; i++) {
        chunk[i] = (byte) (i * 31);
      }
      final int totalChunks = 16; // 64MiB
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      for (int i = 0; i < totalChunks; i++) {
        chunk1
            .sendAsync(Address.from("127.0.0.1", 29114), "snapshot-chunk", chunk)
            .get(30, TimeUnit.SECONDS);
        assertTrue(System.nanoTime() < deadline, "4MiB 块流水线传输不应退化为分钟级(流控窗口回归)");
      }
      await()
          .atMost(Duration.ofSeconds(30))
          .until(() -> receivedBytes.get() >= (long) totalChunks * chunk.length);
    } finally {
      stopQuietly(chunk1);
      stopQuietly(chunk2);
    }
  }

  @Test
  void fragmentedLargePayloadRoundTrips(@TempDir final File tempDir) throws Exception {
    // aeron-all 1.48 单条上限 = min(term/8, 16MiB): 16MiB term → 2MiB 上限, 覆盖 1MiB 载荷
    final int termLength = 16 * 1024 * 1024;
    final AeronMessagingService large1 =
        startService(new File(tempDir, "large1"), Address.from("127.0.0.1", 29111), termLength);
    final AeronMessagingService large2 =
        startService(new File(tempDir, "large2"), Address.from("127.0.0.1", 29112), termLength);
    try {
      large2.registerHandler(
          "large-echo",
          (BiFunction<Address, byte[], CompletableFuture<byte[]>>)
              (sender, payload) -> CompletableFuture.completedFuture(payload));

      final byte[] payload = new byte[1024 * 1024 + 123];
      for (int i = 0; i < payload.length; i++) {
        payload[i] = (byte) (i * 31);
      }
      final byte[] response =
          large1
              .sendAndReceive(Address.from("127.0.0.1", 29112), "large-echo", payload)
              .get(30, TimeUnit.SECONDS);

      assertArrayEquals(payload, response);
    } finally {
      stopQuietly(large1);
      stopQuietly(large2);
    }
  }

  @Test
  void unreachablePeerTimesOut(@TempDir final File tempDir) throws Exception {
    // 向一个无任何服务的地址发送: 发布通道无法建立 → connectTimeout 内按连接失败完成
    final Address senderAddress = Address.from("127.0.0.1", 29103);
    final AeronMessagingService sender = startService(new File(tempDir, "sender"), senderAddress);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    try {
      sender
          .sendAndReceive(
              Address.from("127.0.0.1", 29199),
              "echo",
              new byte[] {1},
              true,
              Duration.ofSeconds(8))
          .whenComplete((ignored, error) -> failure.set(error));
      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () -> {
                final Throwable error = failure.get();
                assertNotNull(error);
                Throwable cause = error;
                while (cause.getCause() != null && !(cause instanceof java.net.ConnectException)) {
                  cause = cause.getCause();
                }
                assertInstanceOf(java.net.ConnectException.class, cause);
              });
    } finally {
      stopQuietly(sender);
    }
  }

  private AeronMessagingService startService(
      final File aeronDir, final Address address, final int termLength) throws Exception {
    final AeronMessagingService service = service(aeronDir, address, termLength);
    service.start().get(10, TimeUnit.SECONDS);
    return service;
  }

  private AeronMessagingService startService(final File aeronDir, final Address address)
      throws Exception {
    return startService(aeronDir, address, 1024 * 1024);
  }

  private AeronMessagingService service(
      final File aeronDir, final Address address, final int termLength) {
    final MessagingConfig messagingConfig = new MessagingConfig().setPort(address.port());
    final AeronMessagingConfig aeronConfig =
        new AeronMessagingConfig()
            .setAeronDir(aeronDir)
            .setTermBufferLength(termLength)
            .setDriverThreadingMode("SHARED")
            .setIdleStrategy("sleeping");
    final AeronTransport transport = new AeronTransport(address, messagingConfig, aeronConfig);
    return new AeronMessagingService(address, messagingConfig, aeronConfig, transport);
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
      throw new AssertionError("期望异常但正常返回");
    } catch (final ExecutionException error) {
      return error;
    } catch (final CompletionException error) {
      return new ExecutionException(error.getCause());
    } catch (final AssertionError error) {
      throw error;
    } catch (final Exception error) {
      throw new AssertionError("期望 ExecutionException, 实际: " + error, error);
    }
  }
}
