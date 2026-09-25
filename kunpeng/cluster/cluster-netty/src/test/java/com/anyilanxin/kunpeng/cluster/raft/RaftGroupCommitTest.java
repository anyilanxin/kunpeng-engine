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
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/**
 * 保序 group commit：合并窗口内多个提交目标只做一次 fsync。当前实现每次 commit 刷一次，此测试用于驱动改造。
 *
 * <p>构造说明：{@code RaftRule.appendEntries} 是同步逐条等 commit 的，无法形成提交突发；
 * 因此改用 {@link RaftRule#appendEntryAsync()}（不等 commit 的追加 API）在测试线程上
 * 不间断地投递 {@value #BURST} 个追加请求，它们在 raft 线程上构成同一合并窗口内的
 * {@value #BURST} 个提交目标，最后只等最终一条提交完成，再统计窗口内的真实 fsync 次数。
 */
public class RaftGroupCommitTest {

  /** 突发窗口内的提交目标个数。 */
  private static final int BURST = 5;

  private final RecordingFlusherConfigurator recorder = new RecordingFlusherConfigurator();

  @Rule public final RaftRule rule = RaftRule.withBootstrappedNodes(1, recorder);

  @Test
  public void shouldFlushAtMostOncePerBurstWindow() throws Exception {
    // 准备：单节点集群 READY，先提交一条预热提交管线，并记录基线 flush 计数
    final var leader = rule.getServers().iterator().next();
    awaitReady(leader);
    rule.awaitNewLeader();
    rule.appendEntries(1);
    recorder.reset();

    // 执行：不等待地连续追加 BURST 条，形成同一合并窗口内的 BURST 个提交目标
    final List<RaftRule.TestAppendListener> listeners = new ArrayList<>();
    for (int i = 0; i < BURST; i++) {
      listeners.add(rule.appendEntryAsync());
    }

    // 等最终一条提交即代表窗口内全部目标均已提交
    final long finalIndex = listeners.get(BURST - 1).awaitCommit();
    final long flushes = recorder.flushCount();

    System.out.printf(
        "TEST OUTPUT: burst=%d finalIndex=%d flushes=%d%n", BURST, finalIndex, flushes);

    // 验证：合并窗口内 flush 次数必须小于提交目标数（当前实现为逐条刷盘，恰好 == BURST）
    assertThat(flushes)
        .as("合并窗口内 %d 个提交目标应共享一次 fsync", BURST)
        .isLessThan(BURST);

    // 验证：批量指标——burst 窗口内至少出现过一次 batch > 1 的合并刷盘
    final var batchSummary =
        leader
            .getContext()
            .getMeterRegistry()
            .get("raft.commit.batch.size")
            .tag("partition", leader.getContext().getName())
            .summary();
    assertThat(batchSummary.count())
        .as("应记录到合并刷盘批量样本")
        .isGreaterThanOrEqualTo(flushes);
    assertThat(batchSummary.max())
        .as("burst 场景应出现 batch > 1 的合并窗口")
        .isGreaterThanOrEqualTo(2.0);
  }

  private static void awaitReady(final RaftServer server) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> server.getContext().getState() == State.READY);
  }
}
