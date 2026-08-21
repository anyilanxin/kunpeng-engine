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
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftRule.Configurator;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Builder;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TestRaftServerProtocol;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TestSnapshotStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.agrona.LangUtil;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/** 节点重启时的一致性检查与快照安装重试。 */
public class RaftStartupConsistencyCheckTest {

  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  /** 覆盖 camunda/issues/10451：跟随者在提交快照前“崩溃”，重启后一致性检查不应失败。 */
  @Test
  public void restartSurvivesCrashBetweenSnapshotReceiveAndCommit() throws Exception {
    // 准备：隔离一台跟随者，让领导者产生快照并降低阈值强制走快照复制
    final var isolatedFollower = raftRule.getFollower().orElseThrow();
    raftRule.partition(isolatedFollower);

    final var committedIndex = raftRule.appendEntries(10);
    final var leader = raftRule.getLeader().orElseThrow();
    leader.getContext().setPreferSnapshotReplicationThreshold(1);
    final long snapshotIndex = committedIndex - 1;
    raftRule.takeSnapshot(leader, snapshotIndex, 1);

    // 在跟随者持久化新快照之前触发关闭，模拟崩溃
    final var snapshotStore =
        (TestSnapshotStore) isolatedFollower.getContext().getPersistedSnapshotStore();
    final CompletableFuture<Void> crashed = new CompletableFuture<>();
    snapshotStore.interceptOnNewSnapshot(
        () -> {
          isolatedFollower
              .shutdown()
              .thenApply(ignored -> crashed.complete(null))
              .exceptionally(crashed::completeExceptionally);
          throw new RuntimeException("模拟持久化快照时崩溃");
        });

    // 执行：恢复连接，让跟随者收到快照并“崩溃”，随后重启重新加入
    raftRule.reconnect(isolatedFollower);
    crashed.join();

    final var followerId = isolatedFollower.cluster().getLocalMember().memberId().id();
    assertThatNoException()
        .as("重启时的一致性检查不应抛出异常")
        .isThrownBy(() -> raftRule.joinCluster(followerId));

    // 验证：重启后的跟随者最终收到并提交了快照
    Awaitility.await("重启后的跟随者收到新快照")
        .untilAsserted(
            () ->
                assertThat(
                        raftRule
                            .getPersistedSnapshotStore(followerId)
                            .getLatestSnapshot()
                            .map(PersistedSnapshot::getIndex)
                            .orElse(0L))
                    .isEqualTo(snapshotIndex));
  }

  /** 覆盖 camunda/issues/14367：快照持久化过慢导致 install 请求重试时，重连节点仍能追上。 */
  @Test
  public void retriedInstallRequestsDoNotBreakCatchUpAfterSnapshotPersist() throws Exception {
    // 准备：关闭一台跟随者，让领导者做压缩快照并追加后续日志
    final var persistStarted = new AtomicBoolean(false);
    final var persistBarrier = new CountDownLatch(1);
    final var leader = raftRule.getLeader().orElseThrow();
    final var follower = raftRule.getFollower().orElseThrow();
    raftRule.shutdownServer(follower);

    final var snapshotIndex = raftRule.appendEntries(500);
    raftRule.takeCompactingSnapshot(leader, snapshotIndex - 1, 3);
    final var lastLogIndex = raftRule.appendEntries(10);
    leader.getContext().setPreferSnapshotReplicationThreshold(1);

    // 执行：跟随者重连时，第一次快照持久化被人为阻塞，逼迫领导者重试 install 请求；
    // 只有在持久化已经开始阻塞后到达的重试请求才放行
    raftRule.bootstrapNode(
        follower.name(),
        new Configurator() {
          @Override
          public void configure(final MemberId id, final Builder builder) {
            final var protocol = (TestRaftServerProtocol) builder.protocol;
            protocol.interceptRequest(
                InstallRequest.class,
                request -> {
                  if (persistStarted.get()) {
                    persistBarrier.countDown();
                  }
                });
          }

          @Override
          public void configure(final TestSnapshotStore snapshotStore) {
            snapshotStore.interceptOnNewSnapshot(
                () -> {
                  persistStarted.set(true);
                  try {
                    persistBarrier.await();
                  } catch (final InterruptedException e) {
                    LangUtil.rethrowUnchecked(e);
                  }
                });
          }
        });

    // 验证：重连的跟随者拿到快照并把日志追平
    raftRule.allNodesHaveSnapshotWithIndex(snapshotIndex);
    raftRule.awaitSameLogSizeOnAllNodes(lastLogIndex);
  }
}
