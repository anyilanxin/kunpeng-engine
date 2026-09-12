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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/**
 * follower 侧保序 group commit：合并窗口内多个 AppendRequest 共享一次 fsync，且 ack 发出时必已持久化。
 *
 * <p>持久化先于确认的可检测构造：leader 推进 commit 必须收到 quorum（leader + ≥1 follower）的 ack，
 * 而 follower ack 只能在其 forceFlush 完成后发出。因此在 leader 的 commit 事件回调里（与
 * applyCommitAdvance 同栈执行）同步快照各 follower 的刷盘计数——若实现退化为"先 ack 后刷盘"，
 * 该快照会观察到 0 次 follower 刷盘，断言即失败。合批断言：突发投递下真实 fsync 的耗时会迫使
 * 后续 AppendRequest 在同一合并窗口内排队，follower 的 fsync 次数应显著少于突发条目数。
 */
public class RaftFollowerFlushCoalescingTest {

  /** 集群节点数。 */
  private static final int NODE_COUNT = 3;

  /** 突发窗口内投递的条目数。 */
  private static final int BURST = 10;

  private final RecordingFlusherConfigurator recorder = new RecordingFlusherConfigurator();

  @Rule public final RaftRule rule = RaftRule.withBootstrappedNodes(NODE_COUNT, recorder);

  @Test
  public void shouldFlushOnFollowerBeforeLeaderCommits() throws Exception {
    // 准备：3 节点集群 READY，先提交一条预热提交管线，并清零计数以隔离本次断言
    awaitReadyAll();
    rule.awaitNewLeader();
    final var leaderId = rule.getLeader().orElseThrow().name();
    final var followerIds = followerNodeIds(leaderId);
    final long warmupIndex = rule.appendEntries(1);
    recorder.reset();

    // 在 leader 的 commit 事件点同步快照各 follower 刷盘计数（leader 线程上先到先记）
    final var followerFlushesAtLeaderCommit = new ConcurrentHashMap<String, Long>();
    final var captured = new AtomicBoolean(false);
    rule.addCommitListener(
        index -> {
          if (index > warmupIndex && captured.compareAndSet(false, true)) {
            for (final var followerId : followerIds) {
              followerFlushesAtLeaderCommit.put(followerId, recorder.flushCount(followerId));
            }
          }
        });

    // 执行：追加 1 条并等 leader 提交完成
    final long committedIndex = rule.appendEntries(1);

    // 快照与 client future 完成在 leader 提交事件的同一调用链内，最多晚微秒级，等待其落定
    Awaitility.await("follower flush snapshot at leader commit")
        .atMost(30, TimeUnit.SECONDS)
        .until(captured::get);

    final long flushesAtCommit =
        followerFlushesAtLeaderCommit.values().stream().mapToLong(Long::longValue).sum();
    System.out.printf(
        "TEST OUTPUT: warmupIndex=%d committedIndex=%d followerFlushesAtLeaderCommit=%s%n",
        warmupIndex, committedIndex, followerFlushesAtLeaderCommit);

    // 验证：leader 提交时刻，参与 quorum 的 follower 必须已刷盘（先 ack 后刷盘的实现此处为 0）
    assertThat(flushesAtCommit)
        .as("leader 提交前，参与 quorum 的 follower 必须已持久化（持久化先于确认）")
        .isGreaterThanOrEqualTo(1L);

    // 最终每个 follower 都要把这批条目落盘
    for (final var followerId : followerIds) {
      Awaitility.await("follower " + followerId + " flushed the entry")
          .atMost(30, TimeUnit.SECONDS)
          .until(() -> recorder.flushCount(followerId) >= 1L);
    }
  }

  @Test
  public void shouldCoalesceFlushesAcrossBurstAppendsOnFollower() throws Exception {
    // 准备：3 节点集群 READY，先提交一条预热提交管线，并清零计数
    awaitReadyAll();
    rule.awaitNewLeader();
    final var leaderId = rule.getLeader().orElseThrow().name();
    final var followerIds = followerNodeIds(leaderId);
    rule.appendEntries(1);
    recorder.reset();

    // 执行：不等待地连续投递 BURST 条，形成 AppendRequest 突发
    final List<RaftRule.TestAppendListener> listeners = new ArrayList<>();
    for (int i = 0; i < BURST; i++) {
      listeners.add(rule.appendEntryAsync());
    }
    final long finalIndex = listeners.get(BURST - 1).awaitCommit();

    final var perNodeFlushes = new ConcurrentHashMap<String, Long>();
    for (final var nodeId : rule.getNodes()) {
      perNodeFlushes.put(nodeId, recorder.flushCount(nodeId));
    }
    System.out.printf(
        "TEST OUTPUT: burst=%d finalIndex=%d perNodeFlushes=%s%n", BURST, finalIndex, perNodeFlushes);

    // 验证：follower 的 fsync 次数应少于突发条目数（真刷盘耗时迫使后续请求并入同一合并窗口）；
    // leader 侧 in-flight 门控 + 批量构建通常还会把多条目并入同一 AppendRequest，进一步压低次数
    for (final var followerId : followerIds) {
      assertThat(recorder.flushCount(followerId))
          .as("follower %s 在 %d 条突发下应共享 fsync", followerId, BURST)
          .isGreaterThanOrEqualTo(1L)
          .isLessThan((long) BURST);
    }
  }

  @Test
  public void shouldKeepMemberLogsConsistentAfterBurst() throws Exception {
    // 准备：3 节点集群 READY 并选出 leader
    awaitReadyAll();
    rule.awaitNewLeader();
    rule.appendEntries(1);

    // 执行：突发投递 BURST 条并等最终一条提交
    final List<RaftRule.TestAppendListener> listeners = new ArrayList<>();
    for (int i = 0; i < BURST; i++) {
      listeners.add(rule.appendEntryAsync());
    }
    final long finalIndex = listeners.get(BURST - 1).awaitCommit();

    // 验证：全部节点提交水位一致，且日志的 (index, term) 序列逐条一致。
    // 不走 getMemberLogs 的条目深拷贝：follower 段文件读回的条目数据是 direct buffer，
    // 该拷贝会 NPE（既有基建限制），而 (index, term) 比较不触碰条目数据
    rule.awaitSameLogSizeOnAllNodes(finalIndex);
    final var leaderLogShape = logShape(rule.getLeader().orElseThrow());
    assertThat(leaderLogShape).isNotEmpty();
    for (final var server : rule.getServers()) {
      assertThat(logShape(server))
          .as("节点 %s 的日志应与 leader 一致", server.name())
          .containsExactlyElementsOf(leaderLogShape);
    }
  }

  /** 读取节点日志的 (index, term) 序列，不反序列化条目数据。 */
  private List<String> logShape(final RaftServer server) {
    final List<String> shape = new ArrayList<>();
    try (final var reader = server.getContext().getLog().openUncommittedReader()) {
      while (reader.hasNext()) {
        final var entry = reader.next();
        shape.add(entry.index() + ":" + entry.term());
      }
    }
    return shape;
  }

  /** 全部节点到达 READY 状态。 */
  private void awaitReadyAll() {
    for (final var server : rule.getServers()) {
      Awaitility.await("server " + server.name() + " ready")
          .atMost(30, TimeUnit.SECONDS)
          .until(() -> server.getContext().getState() == State.READY);
    }
  }

  /** 除 leader 外的节点 id（升序，便于稳定打印）。 */
  private List<String> followerNodeIds(final String leaderId) {
    return rule.getNodes().stream()
        .filter(id -> !id.equals(leaderId))
        .sorted()
        .collect(java.util.stream.Collectors.toList());
  }
}
