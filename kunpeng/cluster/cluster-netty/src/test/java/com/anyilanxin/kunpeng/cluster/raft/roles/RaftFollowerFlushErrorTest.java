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

import com.anyilanxin.kunpeng.cluster.raft.FaultyFlusherConfigurator;
import com.anyilanxin.kunpeng.cluster.raft.RaftRule;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/**
 * 跟随者刷盘失败时的行为：故障节点数不足半数时，提交不受影响；flusher 恢复后日志收敛，
 * 全部成员保持在 LEADER/FOLLOWER 正常角色。
 */
public class RaftFollowerFlushErrorTest {

  private static final int CLUSTER_SIZE = 3;

  private final AtomicBoolean failureSwitch = new AtomicBoolean(false);
  private final AtomicInteger failureCounter = new AtomicInteger(0);

  @Rule
  public RaftRule raftRule =
      RaftRule.withBootstrappedNodes(
          CLUSTER_SIZE,
          new FaultyFlusherConfigurator(
              (CLUSTER_SIZE - 1) / 2,
              failureSwitch::get,
              failureCounter::incrementAndGet,
              false,
              false));

  @Test
  public void followerFlushFailureDoesNotBlockCommits() throws Throwable {
    // 准备：优先选举保证领导者不在故障节点之列
    final var leader = raftRule.getLeader().orElseThrow();
    final int faultyNodes = (CLUSTER_SIZE - 1) / 2;
    assertThat(Integer.parseInt(leader.name()))
        .as("领导者的编号应大于故障节点数")
        .isGreaterThan(faultyNodes);

    // 先同步一条日志，避免故障开关打开时该条目尚未到达故障节点
    final var firstIndex = raftRule.appendEntry();
    raftRule.awaitSameLogSizeOnAllNodes(firstIndex);

    // 执行：打开故障开关后写入新条目
    failureSwitch.set(true);
    final var appendDuringFailure = raftRule.appendEntryAsync();
    final var committedIndex = appendDuringFailure.awaitCommit(Duration.ofSeconds(5));

    // 验证：故障节点至少各失败一次，但提交照常完成
    Awaitility.await("每个故障节点至少失败一次")
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> failureCounter.get() > faultyNodes);

    // 执行：恢复 flusher
    failureSwitch.set(false);

    // 验证：日志最终收敛，成员一个不少，且都处于正常角色
    raftRule.awaitSameLogSizeOnAllNodes(committedIndex);
    assertThat(raftRule.getMemberLogs()).hasSize(CLUSTER_SIZE);
    Awaitility.await("所有成员回到 FOLLOWER/LEADER")
        .untilAsserted(
            () -> {
              final var roles =
                  raftRule.getServers().stream().map(RaftServer::getRole).toList();
              assertThat(roles)
                  .withFailMessage("期望所有成员为 FOLLOWER 或 LEADER，实际为 %s", roles)
                  .allMatch(r -> r == Role.FOLLOWER || r == Role.LEADER);
            });
  }
}
