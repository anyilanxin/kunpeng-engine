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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.apicommand;

import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * @author zxuanhong
 * @since
 */
public class CommandApiServiceTransitionStep implements TransitionStep<AdminTransitionContent> {

  @Override
  public String getName() {
    return "Admin Command Api Service Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(final AdminTransitionContent context, final long currentTerm) {
    return register(context);
  }

  @Override
  public ActorFuture<Void> onFollower(
      final AdminTransitionContent context, final long currentTerm) {
    return unregister(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final AdminTransitionContent context, final long currentTerm) {
    return unregister(context);
  }

  private ActorFuture<Void> register(final AdminTransitionContent context) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              if (context.getCommandApiService() != null && context.getEventLog() != null) {
                context.getCommandApiService().registerHandlers(context.getEventLog());
              }
              future.complete(null);
            });
    return future;
  }

  private ActorFuture<Void> unregister(final AdminTransitionContent context) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              if (context.getCommandApiService() != null) {
                context.getCommandApiService().unregisterHandlers();
              }
              future.complete(null);
            });
    return future;
  }
}
