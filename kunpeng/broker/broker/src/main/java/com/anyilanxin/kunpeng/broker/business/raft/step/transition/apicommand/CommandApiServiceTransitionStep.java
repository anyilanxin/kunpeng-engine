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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apicommand;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 业务命令 API 的分区 transition 步骤：leader 分区把命令接收 handler 绑定到本分区事件日志（client 命令经 commandapi 直写 leader 日志）；
 * 非 leader 角色解绑。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandApiServiceTransitionStep implements TransitionStep<BusinessTransitionContent> {

  @Override
  public ActorFuture<Void> onLeader(
      final BusinessTransitionContent context, final long currentTerm) {
    final CommandApiServiceImpl commandApiService = context.getCommandApiService();
    if (commandApiService == null) {
      return null;
    }
    return commandApiService.registerHandlers(
        context.getRaftPartitionSource(), context.getEventLog());
  }

  @Override
  public ActorFuture<Void> onFollower(
      final BusinessTransitionContent context, final long currentTerm) {
    return unregister(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final BusinessTransitionContent context, final long currentTerm) {
    return unregister(context);
  }

  private ActorFuture<Void> unregister(final BusinessTransitionContent context) {
    final CommandApiServiceImpl commandApiService = context.getCommandApiService();
    if (commandApiService == null) {
      return null;
    }
    return commandApiService.unregisterHandlers(context.getRaftPartitionSource());
  }

  @Override
  public String getName() {
    return "Command Api Service Transition";
  }
}
