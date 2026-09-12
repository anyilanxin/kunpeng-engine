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
package com.anyilanxin.kunpeng.cluster.raft;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext.State;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * raft 日志提交/读取性能基线（保序 group commit 改造前的对照数据，改造后重跑对比）。
 *
 * <p>跑法：{@code ./gradlew :kunpeng:cluster:cluster:test --tests "*RaftJournalPerf*"}
 * （stdout 被吞时读 build/test-results/test 下的 XML system-out，或加 {@code -i}）。
 *
 * <p>方法：预热 {@value #WARMUP_ENTRIES} 条 + {@value #ROUNDS} 轮 × {@value #MEASURED_ENTRIES}
 * 条取最优轮；写入经 {@link RaftRule#appendEntry()} 同步逐条提交（1024 字节固定 payload），
 * 逐条计时得到提交延迟分布；读取用 {@link RaftLog#openCommittedReader()} 顺序读回。
 * flusher 为 {@link RecordingFlusherConfigurator} 注入的"真刷盘 + 计数"实现，flush 次数为
 * 测量窗口内的真实 fsync 次数（不刻意断言具体值，改造前后由数字自证）。所有数字以
 * TEST OUTPUT 前缀打印，便于从测试报告中提取。
 */
public class RaftJournalPerfTest {

  /** 每轮正式测量前预热的提交条数。 */
  private static final int WARMUP_ENTRIES = 2000;

  /** 每轮正式测量的提交条数。 */
  private static final int MEASURED_ENTRIES = 10_000;

  /** 写入/读取的测量轮数，取最优轮。 */
  private static final int ROUNDS = 3;

  /** 批量 pipeline 场景的单批在途提交条数（合并窗口深度）。 */
  private static final int PIPELINE_BATCH = 64;

  @Test
  public void singleNodeWriteLatencyFlushAndReadBaseline() throws Exception {
    runWithCluster(
        1,
        (rule, recorder) -> {
          final var leader = awaitReadyLeader(rule);
          final RaftLog leaderLog = leader.getContext().getLog();

          // 预热
          rule.appendEntries(WARMUP_ENTRIES);

          // 写入测量：逐条同步提交并计时
          final var write = new WriteBenchmark();
          for (int round = 0; round < ROUNDS; round++) {
            write.runRound(rule, recorder);
            printWriteRound(1, round, write.lastRoundNs(), write.lastRoundFlushes());
          }
          write.printBest(1);

          // 读取测量：openCommittedReader 顺序读回全部已提交条目，多轮取最优
          final long lastIndex = leaderLog.getLastIndex();
          long bestReadNs = Long.MAX_VALUE;
          long readCount = 0;
          for (int round = 0; round < ROUNDS; round++) {
            final long start = System.nanoTime();
            long count = 0;
            try (final var reader = leaderLog.openCommittedReader()) {
              while (reader.hasNext()) {
                reader.next();
                count++;
              }
            }
            final long elapsedNs = System.nanoTime() - start;
            if (elapsedNs < bestReadNs) {
              bestReadNs = elapsedNs;
            }
            readCount = count;
          }
          System.out.printf(
              "TEST OUTPUT: [read-rf1 BEST] totalMs=%d entries=%d throughput=%.0f entries/s (%d 轮取最优)%n",
              bestReadNs / 1_000_000,
              readCount,
              readCount * 1_000_000_000.0 / bestReadNs,
              ROUNDS);

          // sanity：写入全部已提交、可完整顺序读回、窗口内确有刷盘，防止测试空转
          assertThat(leaderLog.getCommitIndex()).as("提交索引").isEqualTo(lastIndex);
          assertThat(readCount).as("读回条数").isEqualTo(lastIndex);
          assertThat(write.bestRoundFlushes()).as("窗口内 flush 次数").isGreaterThan(0);
        });
  }

  @Test
  public void threeNodesWriteThroughputAndFlushBaseline() throws Exception {
    runWithCluster(
        3,
        (rule, recorder) -> {
          final var leader = awaitReadyLeader(rule);
          final var leaderName = leader.name();
          final var followerNames =
              rule.getNodes().stream()
                  .filter(name -> !name.equals(leaderName))
                  .sorted()
                  .collect(Collectors.joining("+"));

          // 预热
          rule.appendEntries(WARMUP_ENTRIES);

          // 写入测量：逐条同步提交（含 follower 复制与刷盘的端到端成本）
          final var write = new WriteBenchmark();
          for (int round = 0; round < ROUNDS; round++) {
            final Map<String, Long> flushesBefore = snapshotFlushCounts(recorder);
            write.runRound(rule, recorder);
            printWriteRound(3, round, write.lastRoundNs(), write.lastRoundFlushes());
            final long leaderFlushes =
                recorder.flushCount(leaderName) - flushesBefore.getOrDefault(leaderName, 0L);
            System.out.printf(
                "TEST OUTPUT: [flush-split-rf3 round %d] leader(%s)=%d followers(%s)=%d%n",
                round,
                leaderName,
                leaderFlushes,
                followerNames,
                write.lastRoundFlushes() - leaderFlushes);
          }
          write.printBest(3);

          // sanity：全部节点日志一致、leader 全部已提交，防止测试空转
          final long lastIndex = leader.getContext().getLog().getLastIndex();
          rule.awaitSameLogSizeOnAllNodes(lastIndex);
          assertThat(leader.getContext().getLog().getCommitIndex())
              .as("提交索引")
              .isEqualTo(lastIndex);
        });
  }

  /**
   * 批量 pipeline 写入吞吐：单投递线程按批（{@value #PIPELINE_BATCH} 条）连续 async 投递、等该批
   * 全部提交后再投下一批。批内的在途提交目标在 raft 线程队列中累积并共享合并 fsync（对应 jraft
   * AppendBatcher 的窗口形态），批间同步控制队列深度；单投递线程避免 {@code RaftRule} 的
   * position 字段竞态。rf1 与 rf3 各测一个集群。
   */
  @Test
  public void pipelineWriteThroughputAndFlushes() throws Exception {
    runWithCluster(1, (rule, recorder) -> runPipelineWrite(1, rule, recorder));
    runWithCluster(3, (rule, recorder) -> runPipelineWrite(3, rule, recorder));
  }

  /** 在已就绪集群上执行 {@value #ROUNDS} 轮批量 pipeline 提交，打印最优轮吞吐与 flush 次数。 */
  private static void runPipelineWrite(
      final int replicationFactor, final RaftRule rule, final RecordingFlusherConfigurator recorder)
      throws Exception {
    final var leader = awaitReadyLeader(rule);
    rule.appendEntries(500); // 预热

    final int batches = MEASURED_ENTRIES / PIPELINE_BATCH;
    double bestThroughput = 0;
    long bestMs = 0;
    long bestFlushes = 0;
    for (int round = 0; round < ROUNDS; round++) {
      final long flushesBefore = recorder.flushCount();
      final long start = System.nanoTime();
      for (int b = 0; b < batches; b++) {
        final var listeners = new ArrayList<RaftRule.TestAppendListener>(PIPELINE_BATCH);
        for (int i = 0; i < PIPELINE_BATCH; i++) {
          listeners.add(rule.appendEntryAsync());
        }
        // 最后一条提交即代表该批全部提交（日志按序提交）
        listeners.get(PIPELINE_BATCH - 1).awaitCommit();
      }
      final long elapsedNs = System.nanoTime() - start;
      final long flushes = recorder.flushCount() - flushesBefore;
      final double throughput = (double) MEASURED_ENTRIES * 1_000_000_000.0 / elapsedNs;
      System.out.printf(
          "TEST OUTPUT: [pipe-write-rf%d round %d] totalMs=%d batch=%d throughput=%.0f entries/s flushes=%d flushesPerEntry=%.2f%n",
          replicationFactor,
          round,
          elapsedNs / 1_000_000,
          PIPELINE_BATCH,
          throughput,
          flushes,
          flushes / (double) MEASURED_ENTRIES);
      if (throughput > bestThroughput) {
        bestThroughput = throughput;
        bestMs = elapsedNs / 1_000_000;
        bestFlushes = flushes;
      }
    }
    System.out.printf(
        "TEST OUTPUT: [pipe-write-rf%d BEST] totalMs=%d batch=%d throughput=%.0f entries/s flushes=%d flushesPerEntry=%.2f (%d 轮取最优)%n",
        replicationFactor,
        bestMs,
        PIPELINE_BATCH,
        bestThroughput,
        bestFlushes,
        bestFlushes / (double) MEASURED_ENTRIES,
        ROUNDS);

    // sanity：全部条目已提交且日志闭合，防止空转出假数字
    final long lastIndex = leader.getContext().getLog().getLastIndex();
    assertThat(leader.getContext().getLog().getCommitIndex())
        .as("提交索引应等于末尾索引")
        .isEqualTo(lastIndex);
    if (replicationFactor == 3) {
      rule.awaitSameLogSizeOnAllNodes(lastIndex);
    }
  }

  /** 记录各节点当前累计 flush 次数快照。 */
  private static Map<String, Long> snapshotFlushCounts(final RecordingFlusherConfigurator recorder) {
    final Map<String, Long> snapshot = new HashMap<>();
    for (final var nodeId : recorder.knownNodeIds()) {
      snapshot.put(nodeId, recorder.flushCount(nodeId));
    }
    return snapshot;
  }

  /** 打印一轮写入结果。 */
  private static void printWriteRound(
      final int replicationFactor, final int round, final long elapsedNs, final long flushes) {
    System.out.printf(
        "TEST OUTPUT: [write-rf%d round %d] totalMs=%d entries=%d throughput=%.0f entries/s flushes=%d%n",
        replicationFactor,
        round,
        elapsedNs / 1_000_000,
        MEASURED_ENTRIES,
        MEASURED_ENTRIES * 1_000_000_000.0 / elapsedNs,
        flushes);
  }

  /** 等待集群选出 leader 并达到 READY。 */
  private static RaftServer awaitReadyLeader(final RaftRule rule) {
    rule.awaitNewLeader();
    final var leader = rule.getLeader().orElseThrow();
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> leader.getContext().getState() == State.READY);
    return leader;
  }

  /**
   * 手动应用 {@link RaftRule} 生命周期：启动 nodes 个节点的集群执行场景，结束后关闭。
   * 每个测试方法只启动自己需要的集群形态。
   */
  private static void runWithCluster(final int nodes, final ClusterScenario scenario)
      throws Exception {
    final var recorder = new RecordingFlusherConfigurator();
    final var rule = RaftRule.withBootstrappedNodes(nodes, recorder);
    final Statement scenarioStatement =
        new Statement() {
          @Override
          public void evaluate() {
            try {
              scenario.run(rule, recorder);
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          }
        };
    try {
      rule.apply(
              scenarioStatement,
              Description.createSuiteDescription("RaftJournalPerfTest-cluster-of-" + nodes))
          .evaluate();
    } catch (final Exception e) {
      throw e;
    } catch (final Error e) {
      throw e;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** 写入测量器：逐轮同步提交并计时，保留最优轮的延迟样本与 flush 次数。 */
  private static final class WriteBenchmark {

    private final long[] bestLatencies = new long[MEASURED_ENTRIES];
    private long bestRoundNs = Long.MAX_VALUE;
    private long bestRoundFlushes;
    private long lastRoundNs;
    private long lastRoundFlushes;

    /** 执行一轮写入：逐条同步提交，记录每条提交延迟。 */
    void runRound(final RaftRule rule, final RecordingFlusherConfigurator recorder)
        throws Exception {
      final long flushesBefore = recorder.flushCount();
      final long[] latencies = new long[MEASURED_ENTRIES];
      final long start = System.nanoTime();
      for (int i = 0; i < MEASURED_ENTRIES; i++) {
        final long entryStart = System.nanoTime();
        rule.appendEntry();
        latencies[i] = System.nanoTime() - entryStart;
      }
      lastRoundNs = System.nanoTime() - start;
      lastRoundFlushes = recorder.flushCount() - flushesBefore;
      if (lastRoundNs < bestRoundNs) {
        bestRoundNs = lastRoundNs;
        bestRoundFlushes = lastRoundFlushes;
        System.arraycopy(latencies, 0, bestLatencies, 0, MEASURED_ENTRIES);
      }
    }

    /** 最优轮总耗时（纳秒）。 */
    long bestRoundNs() {
      return bestRoundNs;
    }

    /** 最优轮窗口内 flush 次数。 */
    long bestRoundFlushes() {
      return bestRoundFlushes;
    }

    /** 最近一轮总耗时（纳秒）。 */
    long lastRoundNs() {
      return lastRoundNs;
    }

    /** 最近一轮窗口内 flush 次数。 */
    long lastRoundFlushes() {
      return lastRoundFlushes;
    }

    /** 打印最优轮的吞吐、延迟分布与刷盘次数。 */
    void printBest(final int replicationFactor) {
      final long[] sorted = bestLatencies.clone();
      Arrays.sort(sorted);
      final int p99Index =
          Math.max(0, Math.min(sorted.length - 1, (int) Math.ceil(0.99 * sorted.length) - 1));
      System.out.printf(
          "TEST OUTPUT: [write-rf%d BEST] totalMs=%d throughput=%.0f entries/s avgCommitLatencyUs=%.1f p99CommitLatencyUs=%.1f flushes=%d flushesPerEntry=%.2f (%d 轮取最优)%n",
          replicationFactor,
          bestRoundNs / 1_000_000,
          MEASURED_ENTRIES * 1_000_000_000.0 / bestRoundNs,
          bestRoundNs / 1000.0 / MEASURED_ENTRIES,
          sorted[p99Index] / 1000.0,
          bestRoundFlushes,
          bestRoundFlushes / (double) MEASURED_ENTRIES,
          ROUNDS);
    }
  }

  /** 单个集群场景：在已启动的集群上执行一段测量逻辑。 */
  @FunctionalInterface
  private interface ClusterScenario {
    void run(RaftRule rule, RecordingFlusherConfigurator recorder) throws Exception;
  }
}
