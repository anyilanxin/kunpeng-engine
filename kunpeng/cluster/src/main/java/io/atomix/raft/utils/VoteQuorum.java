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

/**
 * 一轮投票式确认（选举投票、强制变更确认）的计票器，最终结果只会被上报一次。
 *
 * <p>计票器契约参考自 Apache-2.0 的 SOFAJRaft BallotBox，通用时序：
 *
 * <pre>
 *  发起角色(调用方)          远端成员            实现类(本接口)
 *      |                       |                    |
 *      |----- 发起一轮计票 ------------------------>|
 *      |<-- 逐票 succeed/fail(member) -------------| 直到收敛（多数/全员）
 *      |<================= 回调(结果，仅一次) ======|
 *      |                       |                    |
 *      |----- cancel() --------------------------->| 发起方放弃本轮，不再回调
 * </pre>
 */
public interface VoteQuorum {

  /** 记录来自 {@code member} 的赞成票。 */
  void succeed(MemberId member);

  /** 记录来自 {@code member} 的反对票。 */
  void fail(MemberId member);

  /** 作废本轮计票，此后已注册的回调不会再被触发。 */
  void cancel();
}
