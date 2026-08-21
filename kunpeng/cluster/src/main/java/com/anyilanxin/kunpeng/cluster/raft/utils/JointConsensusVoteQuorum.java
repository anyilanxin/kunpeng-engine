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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * 联合共识（两阶段配置）期间的计票器：新、旧两个成员集合各自达到多数赞成才算成功；
 * 任一集合出现多数反对即立刻失败。同时属于两个集合的成员在两侧各计一票。
 *
 * <p>双侧计票思想参考自 Apache-2.0 的 SOFAJRaft BallotBox（联合共识场景），典型时序：
 *
 * <pre>
 *  发起角色(领导者)        新配置成员          旧配置成员          本类
 *      |                      |                 |                 |
 *      |---- 配置提交请求 ---->|                 |                 |
 *      |<--- 赞成 -------------|                 |                 |
 *      |------ succeed(A) -------------------------------------->| 计入新集合一侧
 *      |---- 配置提交请求 ----------------------->|                |
 *      |<--- 赞成 --------------------------------|               |
 *      |------ succeed(B) -------------------------------------->| 计入旧集合一侧
 *      |                                       （新、旧两侧均达多数）|
 *      |<====================== 回调(true) ======================|
 *      |                      |                 |                 |
 *      |（任一侧反对先到多数，立即回调 false）                     |
 * </pre>
 */
public class JointConsensusVoteQuorum implements VoteQuorum {

  /** 单侧配置的计票结论。 */
  private enum SideResult {
    UNDECIDED,
    APPROVED,
    DECLINED
  }

  private final SimpleVoteQuorum oldSet;
  private final SimpleVoteQuorum newSet;
  private final Consumer<Boolean> onDecided;

  private SideResult oldResult = SideResult.UNDECIDED;
  private SideResult newResult = SideResult.UNDECIDED;
  private boolean finished;

  public JointConsensusVoteQuorum(
      final Consumer<Boolean> onDecided,
      final Collection<MemberId> oldVoters,
      final Collection<MemberId> newVoters) {
    this.onDecided = onDecided;
    this.oldSet = new SimpleVoteQuorum(granted -> recordSide(true, granted), oldVoters);
    this.newSet = new SimpleVoteQuorum(granted -> recordSide(false, granted), newVoters);
  }

  @Override
  public void succeed(final MemberId voter) {
    oldSet.succeed(voter);
    newSet.succeed(voter);
  }

  @Override
  public void fail(final MemberId voter) {
    oldSet.fail(voter);
    newSet.fail(voter);
  }

  @Override
  public void cancel() {
    finished = true;
    oldSet.cancel();
    newSet.cancel();
  }

  /** 收到某一侧配置的多数结论后，检查联合结果是否已可判定。 */
  private void recordSide(final boolean isOldSet, final boolean granted) {
    final SideResult result = granted ? SideResult.APPROVED : SideResult.DECLINED;
    if (isOldSet) {
      oldResult = result;
    } else {
      newResult = result;
    }
    settleIfPossible();
  }

  private void settleIfPossible() {
    if (finished) {
      return;
    }

    if (oldResult == SideResult.DECLINED || newResult == SideResult.DECLINED) {
      finish(false);
    } else if (oldResult == SideResult.APPROVED && newResult == SideResult.APPROVED) {
      finish(true);
    }
  }

  private void finish(final boolean granted) {
    finished = true;
    onDecided.accept(granted);
  }
}
