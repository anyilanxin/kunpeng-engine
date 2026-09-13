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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 分区角色切换步骤接口，定义分别切换为 leader/follower/inactive 时需要执行的逻辑。
 *
 * @author zxuanhong
 * @since
 */
public interface TransitionStep<CONTENT extends TransitionContent> {

  String getName();

  ActorFuture<Void> onLeader(final CONTENT context, final long currentTerm);

  ActorFuture<Void> onFollower(final CONTENT context, final long currentTerm);

  ActorFuture<Void> onInactive(final CONTENT context, final long currentTerm);
}
