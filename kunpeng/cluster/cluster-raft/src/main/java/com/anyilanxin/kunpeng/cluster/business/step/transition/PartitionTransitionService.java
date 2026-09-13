/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.business.step.transition;

import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 分区角色切换服务接口，定义按 term 触发 leader/follower/inactive 切换及更新切换上下文的能力。
 *
 * @author zxuanhong
 * @since
 */
public interface PartitionTransitionService<CONTENT extends TransitionContent> {
  ActorFuture<Void> toFollower(final long currentTerm);

  ActorFuture<Void> toLeader(final long currentTerm);

  ActorFuture<Void> toInactive(final long term);

  /** 等待当前进行中的角色切换完成（含排队中的切换）；无切换进行中时返回已完成的 future，切换失败原样透传异常。 */
  ActorFuture<Void> awaitTransition();

  void setConcurrencyControl(ConcurrencyControl concurrencyControl);

  void updateTransitionContext(CONTENT transitionContext);
}
