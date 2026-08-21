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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 单一成员集合上的多数派计票器：每个成员只有一票，赞成数率先达到严格多数即判定成功
 * （回调收到 {@code true}），反对数率先达到多数则判定失败（回调收到 {@code false}）。
 *
 * <p>多数派计票思想参考自 Apache-2.0 的 SOFAJRaft BallotBox，典型选举时序：
 *
 * <pre>
 *  发起角色(候选者)         远端成员A          远端成员B          本类
 *      |                      |                 |                 |
 *      |---- 投票请求(A) ----->|                 |                 |
 *      |<--- 赞成 -------------|                 |                 |
 *      |------ succeed(A) -------------------------------------->|
 *      |---- 投票请求(B) ----------------------->|                |
 *      |<--- 赞成 --------------------------------|               |
 *      |------ succeed(B) -------------------------------------->| 赞成=2，达到多数
 *      |<====================== 回调(true) ======================|
 *      |                      |                 |                 |
 *      |（任一成员改为 fail 累计反对，反对先到多数则回调 false）    |
 * </pre>
 */
public class SimpleVoteQuorum implements VoteQuorum {

  private final Set<MemberId> outstanding;
  private final int quorumSize;

  private Consumer<Boolean> onDecided;
  private int yes;
  private int no;
  private boolean finished;

  /**
   * @param onDecided 本轮出结果时回调一次；若先被取消则永不回调
   * @param voters 全部有投票资格的成员（含本节点）
   */
  public SimpleVoteQuorum(final Consumer<Boolean> onDecided, final Collection<MemberId> voters) {
    this.onDecided = onDecided;
    this.outstanding = new HashSet<>(voters);
    this.quorumSize = Quorum.majorityOf(voters.size());
  }

  @Override
  public void succeed(final MemberId voter) {
    countVote(voter, true);
  }

  @Override
  public void fail(final MemberId voter) {
    countVote(voter, false);
  }

  @Override
  public void cancel() {
    // 置 finished 只是兜底；清空回调保证之后任何路径都不会触发结果上报。
    onDecided = null;
    finished = true;
  }

  /** 只统计尚在等待中的成员的一票，统计后立即尝试收敛出结果。 */
  private void countVote(final MemberId voter, final boolean inFavor) {
    if (!outstanding.remove(voter)) {
      return;
    }

    if (inFavor) {
      yes++;
    } else {
      no++;
    }
    settleIfPossible();
  }

  private void settleIfPossible() {
    if (finished || onDecided == null) {
      return;
    }

    final Boolean outcome = outcomeOf(yes, no);
    if (outcome != null) {
      finished = true;
      onDecided.accept(outcome);
    }
  }

  /** 任一阵营先到多数即分出胜负，否则本轮尚未定论。 */
  private Boolean outcomeOf(final int yes, final int no) {
    if (yes >= quorumSize) {
      return Boolean.TRUE;
    }
    if (no >= quorumSize) {
      return Boolean.FALSE;
    }
    return null;
  }
}
