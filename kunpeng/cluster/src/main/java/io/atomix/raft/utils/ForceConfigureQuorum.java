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
package io.atomix.raft.utils;

import io.atomix.cluster.MemberId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 强制配置变更的多数派确认计票器。
 *
 * <p>语义参考 Apache-2.0 的 SOFAJRaft Ballot（quorum = n/2 + 1）而非全员确认：新配置的多数派
 * 成员确认即判定成功，掉线或拒绝的少数派不阻塞强制变更；一旦"已赞成 + 剩余未决"不可能达到
 * 多数（如过半成员不可达）则立即判定失败，避免整轮悬挂。被强制丢弃的成员随后在 poll/vote
 * 阶段会被按新配置拒绝。
 *
 * <pre>
 *  发起角色(本节点)        成员A            成员B            成员C(掉线)        本类
 *      |                    |                |                 |                |
 *      |（本节点本地已应用强制配置，构造后先计入一票赞成）       |                | 赞成=1
 *      |-- 强制配置请求 --->|                |                 |                |
 *      |<----- 同意 --------|                |                 |                |
 *      |---- succeed(A) --------------------------------------- ----------->| 赞成=2
 *      |-- 强制配置请求 ----------------------->|              ×  超时           |
 *      |---- fail(C) -------------------------------------------------->| 反对=1（少数派不阻塞）
 *      |<----- 同意 -------------------------|                 |                |
 *      |---- succeed(B) ------------------------------------------------>| 赞成=3 ≥ 多数(3)
 *      |<=================== 回调(true) =====================|
 *      |                    |                |                 |                |
 *      |（若 赞成 + 剩余未决 < 多数，例如过半掉线，立即回调 false）            |
 * </pre>
 */
public final class ForceConfigureQuorum {

  private final Set<MemberId> outstanding;
  private final int quorumSize;
  private Consumer<Boolean> onDecided;
  private int grants;
  private boolean finished;

  /**
   * @param onDecided 多数派确认时回调 {@code true}；确认不可达多数时回调 {@code false}
   * @param voters 全部有确认资格的成员（应含本节点，本节点在构造后先计入一票赞成）
   */
  public ForceConfigureQuorum(
      final Consumer<Boolean> onDecided, final Collection<MemberId> voters) {
    this.onDecided = onDecided;
    this.outstanding = new HashSet<>(voters);
    this.quorumSize = Quorum.majorityOf(voters.size());
  }

  /** 记录来自 {@code member} 的确认；赞成数达到多数即成功收敛。 */
  public void succeed(final MemberId member) {
    if (!outstanding.remove(member)) {
      return;
    }

    grants++;
    if (grants >= quorumSize) {
      decide(true);
    }
  }

  /**
   * 记录来自 {@code member} 的拒绝/不可达。少数派失败不阻塞；当已赞成与剩余未决之和不再可能
   * 达到多数时，立即判定整轮失败。
   */
  public void fail(final MemberId member) {
    if (!outstanding.remove(member)) {
      return;
    }

    if (grants + outstanding.size() < quorumSize) {
      decide(false);
    }
  }

  /** 作废本轮计票，此后回调不会再被触发。 */
  public void cancel() {
    onDecided = null;
    finished = true;
  }

  private void decide(final boolean success) {
    if (finished || onDecided == null) {
      return;
    }

    finished = true;
    onDecided.accept(success);
  }
}
