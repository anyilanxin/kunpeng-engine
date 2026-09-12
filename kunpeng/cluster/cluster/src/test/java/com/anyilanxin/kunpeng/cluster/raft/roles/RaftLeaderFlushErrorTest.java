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
package com.anyilanxin.kunpeng.cluster.raft.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.cluster.raft.FaultyFlusherConfigurator;
import com.anyilanxin.kunpeng.cluster.raft.RaftException.AppendFailureException;
import com.anyilanxin.kunpeng.cluster.raft.RaftRule;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * 领导者刷盘失败时的行为：写入失败要让对应追加请求以 AppendFailureException 失败，
 * 且失败不可恢复（同一个请求不能在 flusher 恢复后成功）；随后集群应选出新领导并正常写入。
 */
@RunWith(Parameterized.class)
public class RaftLeaderFlushErrorTest {

  private static final int CLUSTER_SIZE = 3;
  // 故障节点数量：不足半数 (N-1)/2
  private static final int FAULTY_FLUSHERS = (CLUSTER_SIZE - 1) / 2;

  @Parameter public boolean discardUnflushedData;

  @Rule
  @Parameter(1)
  public RaftRule raftRule;

  @Parameter(2)
  public AtomicBoolean failureSwitch;

  @Parameter(3)
  public AtomicInteger failureCounter;

  @Parameters(name = "{index}: 数据丢失 = {0}")
  public static Collection<Object[]> dataLossVariants() {
    return Stream.of(false, true)
        .map(
            discardData -> {
              final var failureSwitch = new AtomicBoolean(false);
              final var failureCounter = new AtomicInteger(0);
              return new Object[] {
                discardData,
                RaftRule.withBootstrappedNodes(
                    CLUSTER_SIZE,
                    new FaultyFlusherConfigurator(
                        FAULTY_FLUSHERS,
                        failureSwitch::get,
                        failureCounter::incrementAndGet,
                        true,
                        discardData)),
                failureSwitch,
                failureCounter
              };
            })
        .toList();
  }

  @Test
  public void leaderFlushFailureForcesFollowerTransitionAndReelection() throws Exception {
    // 准备：定位领导者，其 flusher 被配置为可故障（优先选举保证领导者是 "1"）
    final var leader = raftRule.getLeader().orElseThrow();
    assertThat(leader.name()).as("可故障的 flusher 应挂在领导者上").isEqualTo("1");
    raftRule.appendEntry();

    // 执行：让领导者的 flusher 开始失败并写入新条目
    failureSwitch.set(true);
    final var pendingAppend = raftRule.appendEntryAsync();

    // 验证：该条目以 AppendFailureException 失败
    assertThatThrownBy(() -> pendingAppend.awaitCommit(Duration.ofSeconds(2)))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(AppendFailureException.class);

    // 执行：flusher 恢复正常后，同一请求依然失败（结果已被固定）
    failureSwitch.set(false);
    assertThatThrownBy(() -> pendingAppend.awaitCommit(Duration.ofSeconds(2)))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(AppendFailureException.class);

    // 验证：集群选出新领导者，且新条目可正常复制到所有节点
    raftRule.awaitNewLeader();
    final var lastIndex = raftRule.appendEntry();
    raftRule.awaitSameLogSizeOnAllNodes(lastIndex);
  }
}
