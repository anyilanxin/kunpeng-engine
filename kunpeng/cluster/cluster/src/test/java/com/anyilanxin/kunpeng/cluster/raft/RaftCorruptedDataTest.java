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

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据损坏场景：多数节点数据丢失后组成 quorum 时，日志最新的少数派节点重新加入集群
 * 不应删除自己的数据，而应进入 INACTIVE 保护自身日志。
 */
public class RaftCorruptedDataTest {
  private static final Logger LOG = LoggerFactory.getLogger(RaftCorruptedDataTest.class);
  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(3);

  /**
   * 构造场景：两台节点数据全损并自行组队形成 quorum（新任期可选为 1 或更高），
   * 再让日志最新的第三台节点重新加入，验证它转为 INACTIVE 而不是被新 quorum 覆盖。
   *
   * @param initialEntries 先提交的日志条数，用于拉开第三台节点与损坏节点的日志差距
   * @param raiseTermInQuorum true 时在损坏 quorum 中追加条目并重启领导者，把任期推高到 1 以上
   */
  private void dataLossQuorumMustNotOverwriteIntactNode(
      final int initialEntries, final boolean raiseTermInQuorum) throws Exception {
    // 准备：取三台节点（未指定名称时 name 即 memberId）
    final var originals = raftRule.getServers().toArray(RaftServer[]::new);
    final var victimA = MemberId.from(originals[0].name());
    final var victimB = MemberId.from(originals[1].name());
    final var intact = MemberId.from(originals[2].name());

    raftRule.appendEntries(initialEntries);
    raftRule.appendEntries(1);
    Awaitility.await("所有节点提交初始日志")
        .until(
            () ->
                Arrays.stream(originals)
                    .allMatch(s -> s.getContext().getCommitIndex() >= initialEntries));

    for (final var node : originals) {
      try {
        raftRule.shutdownServer(node);
      } catch (final Exception shutdownFailure) {
        throw new RuntimeException(shutdownFailure);
      }
    }
    LOG.debug("三台节点全部关闭");

    // 执行：两台节点发生数据丢失，随后以同名节点从零重建并组成 quorum
    raftRule.triggerDataLossOnNode(victimA.id());
    raftRule.triggerDataLossOnNode(victimB.id());

    final var wipedA = raftRule.createServer(victimA);
    final var wipedB = raftRule.createServer(victimB);
    CompletableFuture.allOf(
            wipedA.bootstrap(victimA, victimB, intact), wipedB.bootstrap(victimA, victimB, intact))
        .join();

    Awaitility.await("数据丢失的两台节点形成 quorum")
        .until(() -> wipedA.isLeader() || wipedB.isLeader());

    if (raiseTermInQuorum) {
      // 推高任期：先持续追加（可能伴随选举），再重启领导者
      for (int i = 0; i < 100; i++) {
        Awaitility.await().untilAsserted(
            () -> assertThatNoException().isThrownBy(raftRule::appendEntry));
      }
      raftRule.restartLeader();
      Awaitility.await("quorum 的任期被推高到 1 以上")
          .until(() -> raftRule.getLeader().orElseThrow().getContext().getTerm() > 1);
    }

    // 执行：日志最新的第三台节点以同名重新加入
    final var rejoined = raftRule.createServer(intact);
    rejoined.bootstrap(victimA, victimB, intact);
    if (raiseTermInQuorum) {
      raftRule.appendEntries(1);
    }

    // 验证：它不会退化成 FOLLOWER 去接受更短的日志，而是保持 INACTIVE 以保住自身数据
    Awaitility.await("数据完好的节点转为 INACTIVE")
        .atMost(Duration.ofSeconds(30))
        .until(() -> rejoined.getRole() == Role.INACTIVE);
  }

  @Test
  public void intactNodeGoesInactiveWhenCorruptedQuorumFormsAtTermOne() throws Exception {
    dataLossQuorumMustNotOverwriteIntactNode(100, false);
  }

  @Test
  public void intactNodeGoesInactiveWhenCorruptedQuorumRaisedItsTerm() throws Exception {
    dataLossQuorumMustNotOverwriteIntactNode(200, true);
  }
}
