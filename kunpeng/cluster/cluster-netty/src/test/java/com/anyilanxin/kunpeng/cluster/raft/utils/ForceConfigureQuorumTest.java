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
package com.anyilanxin.kunpeng.cluster.raft.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** 强制配置（force configure）阶段的多数派判定行为。 */
class ForceConfigureQuorumTest {

  // 用不同编号的四人集合验证多数派语义（quorum=3），对应"四副本掉线一个"的场景
  private final MemberId nodeA = MemberId.from("31");
  private final MemberId nodeB = MemberId.from("32");
  private final MemberId nodeC = MemberId.from("33");
  private final MemberId nodeD = MemberId.from("34");
  private final Set<MemberId> voters = Set.of(nodeA, nodeB, nodeC, nodeD);

  @Test
  void allMembersAckTriggersSuccessCallback() {
    final var outcome = new AtomicBoolean(true);
    final var quorum = new ForceConfigureQuorum(b -> outcome.set(b), voters);

    voters.forEach(quorum::succeed);

    assertThat(outcome).isTrue();
  }

  @Test
  void majoritySucceedsEvenWithOneMemberOffline() {
    // 场景：四副本中 nodeD 掉线（超时记反对），其余三人（多数派）确认即成功
    final var outcome = new AtomicBoolean(true);
    final var quorum = new ForceConfigureQuorum(b -> outcome.set(b), voters);

    quorum.fail(nodeD);
    quorum.succeed(nodeA);
    quorum.succeed(nodeB);
    quorum.succeed(nodeC);

    assertThat(outcome).isTrue();
  }

  @Test
  void impossibleQuorumFailsImmediately() {
    // 场景：两人反对后，赞成 + 剩余未决（2）已低于多数（3），立即失败
    final var decision = new CompletableFuture<Boolean>();
    final var quorum = new ForceConfigureQuorum(decision::complete, voters);

    quorum.fail(nodeC);
    quorum.fail(nodeD);

    assertThat(decision).isCompletedWithValue(false);
  }

  @Test
  void duplicateVotesAreCountedOnce() {
    final var decision = new CompletableFuture<Boolean>();
    final var quorum = new ForceConfigureQuorum(decision::complete, voters);

    quorum.succeed(nodeA);
    quorum.succeed(nodeA);
    quorum.succeed(nodeB);
    // 若重复票被计入，此时已凑满 3 票并错误地完成
    assertThat(decision).isNotDone();

    quorum.succeed(nodeC);
    assertThat(decision).isCompletedWithValue(true);
  }
}
