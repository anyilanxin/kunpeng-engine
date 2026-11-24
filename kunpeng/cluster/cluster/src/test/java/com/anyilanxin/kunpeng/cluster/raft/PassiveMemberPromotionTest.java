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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * 被动成员追平后的晋升。
 *
 * <p>晋升的判断与重配置请求的提交不在同一个任务里：请求是投到 raft 线程队列里延后提交的。两个成员的确认追加响应挨着处理时，两次晋升都能通过
 * “leader 此刻能否接受重配置”的判断，随后必有一个请求被以“另一个重配置在进行”拒绝。成员侧对这类拒绝不重试，被拒的那次晋升必须由 leader 清掉
 * 已触发标记、重新触发，否则该成员永远停在 PASSIVE。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("被动成员晋升")
class PassiveMemberPromotionTest {

  /** 推进消息与时钟的步数上限：正常几十步内收敛。 */
  private static final int SETTLE_BUDGET = 2_000;

  private final MemberId leaderId = MemberId.from("0");
  private final MemberId first = MemberId.from("1");
  private final MemberId second = MemberId.from("2");

  private ControllableRaftContexts raftContexts;

  @AfterEach
  void teardown() throws IOException {
    if (raftContexts != null) {
      raftContexts.shutdown();
    }
  }

  @Test
  @Timeout(120)
  @DisplayName("两个被动成员的确认追加响应挨着处理时，两个都晋升为投票成员")
  void bothPassiveMembersArePromotedWhenTheirAcknowledgementsAreHandledBackToBack(
      @TempDir final Path tmp) throws Exception {
    // 单投票成员的集群，领导者是 0；成员 1、2 以被动身份加入
    raftContexts = new ControllableRaftContexts(3);
    raftContexts.setup(Files.createDirectories(tmp.resolve("raft")), new Random(0), Set.of(0));
    final List<CompletableFuture<Void>> joins = new ArrayList<>();
    joins.add(joinAsPassive(first));
    joins.add(joinAsPassive(second));

    // 两个成员处理加入应答和追加请求，各回一条“已追平”的确认追加（都还在领导者的接收队列里）
    raftContexts.runUntilDone(1);
    raftContexts.runUntilDone(2);
    // 两条确认追加先后入队，中间不执行任何任务，再由领导者连续处理
    raftContexts.processNextMessage(leaderId);
    raftContexts.processNextMessage(leaderId);
    raftContexts.runUntilDone(leaderId);

    settle();
    assertThat(joins).allSatisfy(join -> assertThat(join).isCompleted());
    assertThat(typeOf(first)).as("成员 %s 的类型", first).isEqualTo(RaftMember.Type.ACTIVE);
    assertThat(typeOf(second)).as("成员 %s 的类型", second).isEqualTo(RaftMember.Type.ACTIVE);
  }

  /** 成员发起加入，领导者处理完加入请求；加入应答与追加请求留在成员的接收队列里不处理。 */
  private CompletableFuture<Void> joinAsPassive(final MemberId member) {
    // 集群里只有 0 一个投票成员，加入的重配置不经消息往返即提交
    final var join = raftContexts.join(member, Set.of(leaderId, member));
    raftContexts.runUntilDone(member);
    raftContexts.processNextMessage(leaderId);
    raftContexts.runUntilDone(leaderId);
    assertThat(raftContexts.getRaftContext(leaderId).getCluster().getMember(member).getType())
        .as("加入完成后成员 %s 的类型", member)
        .isEqualTo(RaftMember.Type.PASSIVE);
    return join;
  }

  /** 推进任务、消息与时钟，直到两个成员都成为投票成员（或步数用尽）。 */
  private void settle() {
    int budget = SETTLE_BUDGET;
    while (!(typeOf(first) == RaftMember.Type.ACTIVE && typeOf(second) == RaftMember.Type.ACTIVE)
        && budget-- > 0) {
      raftContexts.runUntilDone();
      raftContexts.processAllMessage();
      raftContexts.tickHeartbeatTimeout();
    }
  }

  private RaftMember.Type typeOf(final MemberId member) {
    return raftContexts.getRaftContext(leaderId).getCluster().getMember(member).getType();
  }
}
