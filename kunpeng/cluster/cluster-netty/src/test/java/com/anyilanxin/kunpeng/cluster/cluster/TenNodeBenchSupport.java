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
package com.anyilanxin.kunpeng.cluster.cluster;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 10 节点通信能力基准的共享测量层：Aeron 底座与 Netty(旧模块) 底座各自实现 {@link Harness}，跑完全相同的
 * 五个阶段，保证对比公平。
 *
 * <p>阶段：① RPC 往返延迟（128B, 逐条串行）② 单向吞吐（sendAsync 256B 流水线）③ 单播吞吐（unicast 256B，
 * 统计投递/丢失）④ 事件广播扇出（9 订阅者 × 500 条）⑤ 大报文往返（1MiB echo）。
 */
final class TenNodeBenchSupport {

  /** 节点数可由 -Dbench.nodes 覆盖(默认 10); 6 核本机跑 10 节点会 CPU 超订, 大文件阶段建议 3。 */
  static final int NODES = Integer.getInteger("bench.nodes", 10);
  static final String ECHO_SUBJECT = "bench-echo";
  static final String ONE_WAY_SUBJECT = "bench-oneway";
  static final String UNICAST_SUBJECT = "bench-unicast";
  static final String FILE_SUBJECT = "bench-file";
  static final String TOPIC = "bench-topic";

  /** 底座无关的集群门面；由两个模块各自的适配器实现。 */
  interface Harness {
    /** 启动全部节点并阻塞直到 SWIM 成员表收敛（返回收敛耗时 ms）。 */
    long startAndConverge() throws Exception;

    /** RPC 往返（echo 已注册）。 */
    CompletableFuture<byte[]> rpc(int from, int to, byte[] payload);

    /** 单向消息（sendAsync，完成即入队/投递）。 */
    CompletableFuture<Void> oneWay(int from, int to, byte[] payload);

    /** 大文件块（sendAsync 到 FILE_SUBJECT，接收侧按字节计数）。 */
    CompletableFuture<Void> fileChunk(int from, int to, byte[] payload);

    /** 不可靠单播（尽力而为）。 */
    void unicast(int from, int to, byte[] payload);

    /** 目标节点的单向/单播接收计数。 */
    long deliveredCount(int node);

    /** 目标节点大文件传输阶段的已收字节数。 */
    long deliveredBytes(int node);

    /** 订阅主题的远端订阅者数量（等待订阅关系传播）。 */
    int subscriberCount();

    /** 重置广播投递 latch（期望送达条数），随后广播消费者在新 latch 上递减。 */
    void armTopicLatch(int expectedDeliveries);

    /** 当前广播 latch。 */
    CountDownLatch topicLatch();

    /** 广播一条主题事件。 */
    void broadcast(byte[] payload);

    void stopAll() throws Exception;
  }

  private TenNodeBenchSupport() {}

  /** RPC 往返延迟：串行逐条，采集 p50/p90/p99/mean。 */
  static void rpcLatency(final String module, final Harness harness) throws Exception {
    final byte[] payload = new byte[128];
    Arrays.fill(payload, (byte) 7);
    for (int i = 0; i < 500; i++) {
      harness.rpc(0, NODES - 1, payload).get(10, TimeUnit.SECONDS);
    }
    final int iterations = 5_000;
    final long[] samples = new long[iterations];
    for (int i = 0; i < iterations; i++) {
      final long t0 = System.nanoTime();
      final byte[] response = harness.rpc(0, NODES - 1, payload).get(10, TimeUnit.SECONDS);
      samples[i] = System.nanoTime() - t0;
      if (response.length != payload.length) {
        throw new IllegalStateException("echo 载荷不一致");
      }
    }
    reportLatency(module, "rpc-latency(128B 串行往返)", samples);
  }

  /** 单向吞吐：sendAsync 流水线发送，统计入队完成与对端实际接收两个口径。 */
  static void oneWayThroughput(final String module, final Harness harness) throws Exception {
    final int count = 20_000;
    final byte[] payload = new byte[256];
    Arrays.fill(payload, (byte) 9);
    for (int i = 0; i < 200; i++) {
      harness.oneWay(0, NODES - 1, payload).get(10, TimeUnit.SECONDS);
    }
    awaitDeliveries(harness, 200, 30);
    final long base = harness.deliveredCount(NODES - 1);
    final CompletableFuture<?>[] futures = new CompletableFuture<?>[count];
    final long t0 = System.nanoTime();
    for (int i = 0; i < count; i++) {
      futures[i] = harness.oneWay(0, NODES - 1, payload);
    }
    CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
    final long offeredNs = System.nanoTime() - t0;
    awaitDeliveries(harness, base + count, 60);
    final long deliveredNs = System.nanoTime() - t0;
    final long delivered = harness.deliveredCount(NODES - 1) - base;
    System.out.printf(
        "[bench10n] module=%s phase=oneway(256B) offered=%.0f msg/s, delivered=%.0f msg/s "
            + "(%.1f MiB/s), 送达 %d/%d%n",
        module,
        count / (offeredNs / 1e9),
        delivered / (deliveredNs / 1e9),
        delivered * 256.0 / 1024 / 1024 / (deliveredNs / 1e9),
        delivered,
        count);
  }

  /** 单播吞吐：尽力而为连发，统计对端实际收到数（旧 UDP 可能丢、Aeron 可靠）。 */
  static void unicastThroughput(final String module, final Harness harness) throws Exception {
    final int count = 20_000;
    final byte[] payload = new byte[256];
    Arrays.fill(payload, (byte) 11);
    for (int i = 0; i < 200; i++) {
      harness.unicast(0, NODES - 1, payload);
    }
    awaitDeliveries(harness, 200, 30);
    final long base = harness.deliveredCount(NODES - 1);
    final long t0 = System.nanoTime();
    for (int i = 0; i < count; i++) {
      harness.unicast(0, NODES - 1, payload);
    }
    final long sentNs = System.nanoTime() - t0;
    awaitDeliveries(harness, base + count, 60);
    final long deliveredNs = System.nanoTime() - t0;
    final long delivered = Math.min(harness.deliveredCount(NODES - 1) - base, count);
    System.out.printf(
        "[bench10n] module=%s phase=unicast(256B) 发送 %.0f msg/s, 对端收到 %d/%d "
            + "(%.0f msg/s)%n",
        module,
        count / (sentNs / 1e9),
        delivered,
        count,
        delivered / (deliveredNs / 1e9));
  }

  /** 事件广播扇出：9 个订阅者，500 条事件，统计端到端全部送达速率。 */
  static void broadcastFanout(final String module, final Harness harness) throws Exception {
    final int broadcasts = 500;
    final byte[] payload = new byte[100];
    Arrays.fill(payload, (byte) 13);
    // 等待订阅关系传播到全部 9 个远端订阅者
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
    while (harness.subscriberCount() < NODES - 1) {
      if (System.nanoTime() > deadline) {
        throw new IllegalStateException("订阅关系未传播: " + harness.subscriberCount());
      }
      Thread.sleep(200);
    }
    // 预热一轮
    harness.armTopicLatch(NODES - 1);
    harness.broadcast(payload);
    if (!harness.topicLatch().await(30, TimeUnit.SECONDS)) {
      throw new IllegalStateException("广播预热未送达");
    }
    harness.armTopicLatch(broadcasts * (NODES - 1));
    final CountDownLatch latch = harness.topicLatch();
    final long t0 = System.nanoTime();
    for (int i = 0; i < broadcasts; i++) {
      harness.broadcast(payload);
    }
    final long enqueuedNs = System.nanoTime() - t0;
    if (!latch.await(120, TimeUnit.SECONDS)) {
      throw new IllegalStateException(
          "广播未全部送达: " + latch.getCount() + "/" + (broadcasts * (NODES - 1)));
    }
    final long deliveredNs = System.nanoTime() - t0;
    System.out.printf(
        "[bench10n] module=%s phase=broadcast(100B×9订阅者) 入队 %.0f ev/s, 端到端全部送达 "
            + "%.0f ev/s (平均扇出时延 %.1f µs/ev)%n",
        module,
        broadcasts / (enqueuedNs / 1e9),
        broadcasts / (deliveredNs / 1e9),
        deliveredNs / 1e3 / broadcasts);
  }

  /** 大报文往返：echo，统计有效吞吐（每 RTT 双向 2×payload）。尺寸可用 -Dbench.large.payload.bytes 覆盖。 */
  static void largePayload(final String module, final Harness harness) throws Exception {
    final int count = 20;
    final int payloadBytes = Integer.getInteger("bench.large.payload.bytes", 1024 * 1024);
    final byte[] payload = new byte[payloadBytes];
    Arrays.fill(payload, (byte) 17);
    for (int i = 0; i < 3; i++) {
      harness.rpc(0, NODES - 1, payload).get(30, TimeUnit.SECONDS);
    }
    final long[] samples = new long[count];
    final long t0 = System.nanoTime();
    for (int i = 0; i < count; i++) {
      final long s0 = System.nanoTime();
      harness.rpc(0, NODES - 1, payload).get(30, TimeUnit.SECONDS);
      samples[i] = System.nanoTime() - s0;
    }
    final long elapsed = System.nanoTime() - t0;
    System.out.printf(
        "[bench10n] module=%s phase=large(%dKiB echo×%d) RTT p50=%.2f ms, 有效吞吐 %.1f MiB/s%n",
        module,
        payloadBytes / 1024,
        count,
        percentile(samples, 0.50) / 1e6,
        count * 2.0 * payloadBytes / 1024 / 1024 / (elapsed / 1e9));
  }

  static void awaitDeliveries(final Harness harness, final long expected, final int timeoutSeconds)
      throws InterruptedException {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (harness.deliveredCount(NODES - 1) < expected) {
      if (System.nanoTime() > deadline) {
        return; // 超时按实际收到数统计
      }
      Thread.sleep(20);
    }
  }

  /**
   * 大文件传输：固定总量按块流水线推送（单向、管道深度 16），统计端到端吞吐。 块尺寸扫描 4MiB（kunpeng 快照块默认）/
   * 1MiB / 256KiB，用于回答「大文件传输选多大块」。
   */
  static void fileTransfer(final String module, final Harness harness) throws Exception {
    final long totalBytes = Integer.getInteger("bench.file.total.mib", 128) * 1024L * 1024;
    final int pipeline = 16;
    final int[] chunkKiBs =
        java.util.Arrays.stream(System.getProperty("bench.file.chunks", "4096,1024,256").split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    for (final int chunkKiB : chunkKiBs) {
      final int chunkBytes = chunkKiB * 1024;
      final byte[] chunk = new byte[chunkBytes];
      Arrays.fill(chunk, (byte) 19);
      for (int i = 0; i < pipeline; i++) {
        harness.fileChunk(0, NODES - 1, chunk).get(30, TimeUnit.SECONDS);
      }
      awaitBytes(harness, pipeline * (long) chunkBytes, 30);
      final long base = harness.deliveredBytes(NODES - 1);
      final ArrayDeque<CompletableFuture<Void>> window = new ArrayDeque<>();
      final long t0 = System.nanoTime();
      long sent = 0;
      while (sent < totalBytes) {
        while (window.size() >= pipeline) {
          window.poll().get(30, TimeUnit.SECONDS);
        }
        window.offer(harness.fileChunk(0, NODES - 1, chunk));
        sent += chunkBytes;
      }
      while (!window.isEmpty()) {
        window.poll().get(30, TimeUnit.SECONDS);
      }
      awaitBytes(harness, base + totalBytes, 180);
      final long ns = System.nanoTime() - t0;
      final long received = harness.deliveredBytes(NODES - 1) - base;
      System.out.printf(
          "[bench10n] module=%s phase=file(块=%dKiB,总量=%dMiB,管道=%d) 端到端 %.1f MiB/s "
              + "(%.0f 块/s), 送达 %d/%d MiB%n",
          module,
          chunkKiB,
          totalBytes / 1024 / 1024,
          pipeline,
          received / 1024.0 / 1024.0 / (ns / 1e9),
          received / (double) chunkBytes / (ns / 1e9),
          received / 1024 / 1024,
          totalBytes / 1024 / 1024);
    }
  }

  static void awaitBytes(
      final Harness harness, final long expectedBytes, final int timeoutSeconds)
      throws InterruptedException {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (harness.deliveredBytes(NODES - 1) < expectedBytes) {
      if (System.nanoTime() > deadline) {
        return;
      }
      Thread.sleep(20);
    }
  }

  static void reportLatency(final String module, final String phase, final long[] samplesNs) {
    final long[] sorted = samplesNs.clone();
    Arrays.sort(sorted);
    final double mean = Arrays.stream(sorted).average().orElse(0) / 1e3;
    System.out.printf(
        "[bench10n] module=%s phase=%s n=%d, p50=%.1f µs, p90=%.1f µs, p99=%.1f µs, "
            + "mean=%.1f µs, max=%.1f µs%n",
        module,
        phase,
        sorted.length,
        percentile(sorted, 0.50) / 1e3,
        percentile(sorted, 0.90) / 1e3,
        percentile(sorted, 0.99) / 1e3,
        mean,
        sorted[sorted.length - 1] / 1e3);
  }

  private static long percentile(final long[] sortedSamples, final double quantile) {
    final int index =
        (int) Math.min(sortedSamples.length - 1, quantile * (sortedSamples.length - 1));
    return sortedSamples[index];
  }
}
