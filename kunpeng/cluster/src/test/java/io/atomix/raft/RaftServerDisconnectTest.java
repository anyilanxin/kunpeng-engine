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
package io.atomix.raft;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.MemberId;
import io.atomix.raft.RaftServer.Role;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

/** 网络隔离场景：领导者退位、故障转移与恢复连接后的追赶。 */
public class RaftServerDisconnectTest {

  @Rule @Parameter public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  /** 阻塞直到 server 的角色变为 role（通过角色监听器触发 latch）。 */
  private static CountDownLatch listenForRole(
      final RaftServer server, final Role role, final RaftServer exclude) {
    final var reached = new CountDownLatch(1);
    server.addRoleChangeListener(
        (newRole, term) -> {
          if (newRole == role && !server.equals(exclude)) {
            reached.countDown();
          }
        });
    return reached;
  }

  @Test
  public void isolatedLeaderStepsDown() throws Throwable {
    final RaftServer leader = raftRule.getLeader().orElseThrow();
    final var becameFollower = listenForRole(leader, Role.FOLLOWER, null);

    raftRule.partition(leader);

    assertThat(becameFollower.await(30, TimeUnit.SECONDS))
        .as("被隔离的领导者应退位为 FOLLOWER")
        .isTrue();
    assertThat(leader.isLeader()).isFalse();
  }

  @Test
  public void reconnectingOldLeaderCatchesUpOnMissedEntry() throws Throwable {
    // 准备：记录提交索引，隔离旧领导者并等待新领导者产生
    final RaftServer oldLeader = raftRule.getLeader().orElseThrow();
    final var observedCommitIndex = new AtomicLong();
    oldLeader.getContext().addCommitListener(observedCommitIndex::set);
    raftRule.appendEntry();
    raftRule.partition(oldLeader);
    Awaitility.await().until(() -> !oldLeader.isLeader());

    raftRule.awaitNewLeader();
    final var newLeader = raftRule.getLeader().orElseThrow();
    assertThat(oldLeader).isNotSameAs(newLeader);

    // 执行：新领导者提交一条日志后恢复旧领导者的连接
    final long missedIndex = raftRule.appendEntry();
    raftRule.reconnect(oldLeader);

    // 验证：旧领导者收到并提交了隔离期间漏掉的条目
    Awaitility.await()
        .until(() -> observedCommitIndex.get() >= missedIndex);
  }

  @Test
  public void remainingNodesElectNewLeaderWhenLeaderIsCutOff() throws Throwable {
    final RaftServer leader = raftRule.getLeader().orElseThrow();
    final MemberId oldLeaderId =
        leader.getContext().getCluster().getLocalMember().memberId();

    // 在除旧领导者之外的每个节点上等待 LEADER 角色
    final var otherServers = List.copyOf(raftRule.getServers());
    final var elected = new CountDownLatch(1);
    final var electedLeaderId = new AtomicReference<MemberId>();
    otherServers.forEach(
        server -> {
          if (server.equals(leader)) {
            return;
          }
          server.addRoleChangeListener(
              (role, term) -> {
                if (role == Role.LEADER) {
                  electedLeaderId.set(
                      server.getContext().getCluster().getLocalMember().memberId());
                  elected.countDown();
                }
              });
        });

    raftRule.partition(leader);

    assertThat(elected.await(30, TimeUnit.SECONDS)).as("其余节点应选出新领导者").isTrue();
    assertThat(oldLeaderId).isNotEqualTo(electedLeaderId.get());
  }
}
