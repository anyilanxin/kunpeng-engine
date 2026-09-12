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

import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newFory;
import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newKryoNamespace;
import static com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.newMessage;

import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.benchmark.BenchmarkSupport.PartitionMessage;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.fory.Fory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基准一：Fory 与 Kryo 单线程序列化/反序列化延迟对比。
 *
 * <p>计时方法：先预热触发 JIT，再多轮计时取最优（减少 JIT/GC 抖动），输出字节数、单次耗时与
 * 吞吐对比。覆盖 64B ~ 1MB 消息规模梯度，结果打印到标准输出供人工选型。
 */
final class ForyVsKryoSerializationBenchmarkTest {

  private static final int WARMUP_ITERATIONS = 10_000;
  private static final int MEASURE_ROUNDS = 3;

  /**
   * 消息规模梯度：payload 大小、集合元素数与对应测量迭代数。
   *
   * <p>大消息单次耗时高，按档位降低迭代数以控制总时长（最佳轮次已过滤 GC 抖动）。
   */
  private static final Scenario[] SCENARIOS = {
    new Scenario("64B payload", 64, 3, 8, 30_000),
    new Scenario("4KB payload", 4096, 32, 64, 30_000),
    new Scenario("64KB payload", 64 * 1024, 64, 128, 5_000),
    new Scenario("256KB payload", 256 * 1024, 128, 256, 1_000),
    new Scenario("1MB payload", 1024 * 1024, 256, 512, 300),
  };

  /** 防止 JIT 死代码消除的累加汇。 */
  private static volatile long benchmarkSink;

  @Test
  void shouldBenchmarkSerializationLatency() {
    for (final Scenario scenario : SCENARIOS) {
      benchmark(
          scenario,
          newMessage(scenario.payloadSize(), scenario.memberCount(), scenario.progressSize()));
    }
  }

  /** 单场景基准：先校验往返正确性，再预热计时，打印 Kryo 与 Fory 对比表。 */
  private static void benchmark(final Scenario scenario, final PartitionMessage message) {
    final Namespace kryoNamespace = newKryoNamespace();
    final Fory fory = newFory();

    // 正确性前置：往返一致才参与计时
    assertThat((PartitionMessage) kryoNamespace.deserialize(kryoNamespace.serialize(message)))
        .isEqualTo(message);
    assertThat((PartitionMessage) fory.deserialize(fory.serialize(message))).isEqualTo(message);

    final var kryoBytes = kryoNamespace.serialize(message);
    final var foryBytes = fory.serialize(message);

    final int iterations = scenario.measureIterations();
    warmUp(Math.min(WARMUP_ITERATIONS, iterations), kryoNamespace, fory, message);

    final var kryoResult =
        new BenchmarkResult(
            "Kryo",
            kryoBytes.length,
            bestSerializeNanos(kryoNamespace::serialize, message, iterations),
            bestDeserializeNanos(() -> kryoNamespace.deserialize(kryoBytes), iterations));
    final var foryResult =
        new BenchmarkResult(
            "Fory",
            foryBytes.length,
            bestSerializeNanos(fory::serialize, message, iterations),
            bestDeserializeNanos(() -> fory.deserialize(foryBytes), iterations));

    printTable(scenario.name(), kryoResult, foryResult);
  }

  /** 预热：触发 JIT 编译与池化缓冲扩容。 */
  private static void warmUp(
      final int iterations,
      final Namespace kryoNamespace,
      final Fory fory,
      final PartitionMessage message) {
    long sink = 0;
    for (int i = 0; i < iterations; i++) {
      sink += kryoNamespace.serialize(message).length;
      sink += System.identityHashCode(kryoNamespace.deserialize(kryoNamespace.serialize(message)));
      sink += fory.serialize(message).length;
      sink += System.identityHashCode(fory.deserialize(fory.serialize(message)));
    }
    benchmarkSink = sink;
  }

  /** 序列化计时：多轮取最优，返回单次平均耗时（纳秒）。 */
  private static double bestSerializeNanos(
      final Function<Object, byte[]> serializer, final Object message, final int iterations) {
    double best = Double.MAX_VALUE;
    for (int round = 0; round < MEASURE_ROUNDS; round++) {
      long sink = 0;
      final long start = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        sink += serializer.apply(message).length;
      }
      final long elapsedNanos = System.nanoTime() - start;
      benchmarkSink = sink;
      best = Math.min(best, (double) elapsedNanos / iterations);
    }
    return best;
  }

  /** 反序列化计时：多轮取最优，返回单次平均耗时（纳秒）。 */
  private static double bestDeserializeNanos(
      final Supplier<Object> deserializer, final int iterations) {
    double best = Double.MAX_VALUE;
    for (int round = 0; round < MEASURE_ROUNDS; round++) {
      long sink = 0;
      final long start = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        sink += System.identityHashCode(deserializer.get());
      }
      final long elapsedNanos = System.nanoTime() - start;
      benchmarkSink = sink;
      best = Math.min(best, (double) elapsedNanos / iterations);
    }
    return best;
  }

  /** 打印对比表与相对倍数。 */
  private static void printTable(
      final String scenario, final BenchmarkResult kryo, final BenchmarkResult fory) {
    final String rowFormat = "%-6s %10d %16s %16s %16s %16s%n";
    final String headerFormat = "%-6s %10s %16s %16s %16s %16s%n";
    System.out.printf("%n=== 基准一：单线程序列化/反序列化（%s） ===%n", scenario);
    System.out.printf(
        headerFormat, "框架", "字节数", "序列化ns/次", "反序列化ns/次", "序列化ops/s", "反序列化ops/s");
    printRow(rowFormat, kryo);
    printRow(rowFormat, fory);
    System.out.printf(
        "相对 Kryo：Fory 序列化 %.2fx，反序列化 %.2fx，体积 %.2fx（>1 表示更快/更小）%n",
        kryo.serializeNanos / fory.serializeNanos,
        kryo.deserializeNanos / fory.deserializeNanos,
        (double) kryo.bytes / fory.bytes);
  }

  private static void printRow(final String rowFormat, final BenchmarkResult result) {
    System.out.printf(
        rowFormat,
        result.framework(),
        result.bytes(),
        String.format("%.0f", result.serializeNanos()),
        String.format("%.0f", result.deserializeNanos()),
        toOpsPerSecond(result.serializeNanos()),
        toOpsPerSecond(result.deserializeNanos()));
  }

  private static String toOpsPerSecond(final double nanosPerOp) {
    return String.format("%,.0f", 1e9 / nanosPerOp);
  }

  /** 单框架基准结果。 */
  private record BenchmarkResult(
      String framework, int bytes, double serializeNanos, double deserializeNanos) {}

  /** 消息规模场景：名称、payload 大小、集合元素数与测量迭代数。 */
  private record Scenario(
      String name, int payloadSize, int memberCount, int progressSize, int measureIterations) {}
}
