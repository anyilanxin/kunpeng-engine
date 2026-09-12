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
package com.anyilanxin.kunpeng.cluster.benchmark;

import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newKryoNamespace;
import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newMessage;
import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newThreadSafeFory;

import com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.PartitionMessage;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.fory.ThreadSafeFory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基准二：Fory 与 Kryo 多线程并发序列化吞吐对比。
 *
 * <p>模拟集群消息收发场景：Kryo 侧使用项目真实用法（{@link Namespace} 内部为线程安全池化实例），
 * Fory 侧使用官方推荐的 {@link ThreadSafeFory}。多线程各执行"1 次序列化 + 1 次反序列化"的往返，
 * 统计总吞吐（往返次数/秒）。
 */
final class ForyVsKryoConcurrentBenchmarkTest {

  private static final int THREADS = 4;
  private static final int PAYLOAD_SIZE = 1024;
  private static final int WARMUP_OPS_PER_THREAD = 5_000;
  private static final int MEASURE_OPS_PER_THREAD = 15_000;

  /** 防止 JIT 死代码消除的累加汇。 */
  private static volatile long benchmarkSink;

  /** 序列化往返抽象：屏蔽 Kryo/Fory 实例差异。 */
  private interface RoundTrip {

    byte[] serialize(Object message);

    Object deserialize(byte[] bytes);
  }

  @Test
  void shouldBenchmarkConcurrentThroughput() throws Exception {
    final PartitionMessage message = newMessage(PAYLOAD_SIZE, 8, 16);

    final Namespace kryoNamespace = newKryoNamespace();
    final ThreadSafeFory fory = newThreadSafeFory();

    // 正确性前置：往返一致才参与计时
    assertThat((PartitionMessage) kryoNamespace.deserialize(kryoNamespace.serialize(message)))
        .isEqualTo(message);
    assertThat((PartitionMessage) fory.deserialize(fory.serialize(message))).isEqualTo(message);

    final var kryoRoundTrip =
        new RoundTrip() {
          @Override
          public byte[] serialize(final Object msg) {
            return kryoNamespace.serialize(msg);
          }

          @Override
          public Object deserialize(final byte[] bytes) {
            return kryoNamespace.deserialize(bytes);
          }
        };
    final var foryRoundTrip =
        new RoundTrip() {
          @Override
          public byte[] serialize(final Object msg) {
            return fory.serialize(msg);
          }

          @Override
          public Object deserialize(final byte[] bytes) {
            return fory.deserialize(bytes);
          }
        };

    // 预热
    runConcurrentRoundTrips(kryoRoundTrip, message, WARMUP_OPS_PER_THREAD);
    runConcurrentRoundTrips(foryRoundTrip, message, WARMUP_OPS_PER_THREAD);

    // 各跑两轮取最优
    final double kryoOpsPerSecond =
        Math.max(
            concurrentOpsPerSecond(kryoRoundTrip, message),
            concurrentOpsPerSecond(kryoRoundTrip, message));
    final double foryOpsPerSecond =
        Math.max(
            concurrentOpsPerSecond(foryRoundTrip, message),
            concurrentOpsPerSecond(foryRoundTrip, message));

    System.out.printf("%n=== 基准二：多线程并发往返吞吐（%d 线程，%dB 消息） ===%n", THREADS, PAYLOAD_SIZE);
    System.out.printf("%-6s %18s%n", "框架", "往返ops/s");
    System.out.printf("%-6s %18s%n", "Kryo", String.format("%,.0f", kryoOpsPerSecond));
    System.out.printf("%-6s %18s%n", "Fory", String.format("%,.0f", foryOpsPerSecond));
    System.out.printf("相对 Kryo：Fory 并发吞吐 %.2fx（>1 表示更快）%n", foryOpsPerSecond / kryoOpsPerSecond);
  }

  /** 计时一轮并发往返，返回每秒往返次数。 */
  private static double concurrentOpsPerSecond(
      final RoundTrip roundTrip, final PartitionMessage message) throws InterruptedException {
    final long elapsedNanos = runConcurrentRoundTrips(roundTrip, message, MEASURE_OPS_PER_THREAD);
    return (double) THREADS * MEASURE_OPS_PER_THREAD / (elapsedNanos / 1e9);
  }

  /**
   * 多线程执行"1 次序列化 + 1 次反序列化"往返。
   *
   * @return 总耗时（纳秒）
   */
  private static long runConcurrentRoundTrips(
      final RoundTrip roundTrip, final PartitionMessage message, final int opsPerThread)
      throws InterruptedException {
    final var startGate = new CountDownLatch(1);
    final var doneGate = new CountDownLatch(THREADS);
    final AtomicLong sink = new AtomicLong();
    final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    try {
      for (int t = 0; t < THREADS; t++) {
        final byte[] bytes = roundTrip.serialize(message);
        executor.submit(
            () -> {
              try {
                startGate.await();
                long local = 0;
                for (int i = 0; i < opsPerThread; i++) {
                  local += roundTrip.serialize(message).length;
                  local += System.identityHashCode(roundTrip.deserialize(bytes));
                }
                sink.addAndGet(local);
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
              } finally {
                doneGate.countDown();
              }
            });
      }
      startGate.countDown();
      final long start = System.nanoTime();
      if (!doneGate.await(2, TimeUnit.MINUTES)) {
        throw new IllegalStateException("Concurrent benchmark timed out");
      }
      final long elapsedNanos = System.nanoTime() - start;
      benchmarkSink = sink.get();
      return elapsedNanos;
    } finally {
      executor.shutdownNow();
    }
  }
}
