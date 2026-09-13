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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.dispatch;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.dispatch.DispatchProcessService;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.google.common.collect.ImmutableSet;

public final class DispatchProcessServiceTransitionStep
    implements TransitionStep<AdminTransitionContent> {

  @Override
  public String getName() {
    return "Admin Dispatch Process Service Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(final AdminTransitionContent context, final long currentTerm) {
    return start(context);
  }

  @Override
  public ActorFuture<Void> onFollower(
      final AdminTransitionContent context, final long currentTerm) {
    return close(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final AdminTransitionContent context, final long currentTerm) {
    return close(context);
  }

  private ActorFuture<Void> close(final AdminTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        new Runnable() {
          @Override
          public void run() {
            final DispatchProcessService dispatchProcessService =
                context.getDispatchProcessService();
            if (dispatchProcessService != null) {
              dispatchProcessService.close();
            }
            context.setDispatchProcessService(null);
            future.complete(null);
          }
        });
    return future;
  }

  private ActorFuture<Void> start(final AdminTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final ActorSchedulingService schedulingService = context.getSchedulingService();
          final CommandApiHandle commandApiHandle =
              context.getCommandApiService().gettCommandApiHandle();
          final DispatchProcessService engineService =
              new DispatchProcessService(
                  context.getClusterMetaStore(),
                  context.getEventLog(),
                  context.getRepositoryFactory(),
                  commandApiHandle,
                  new PartitionSourceMetadata(1, 1, ImmutableSet.<Integer>builder().build()),
                  context.getMeterRegistry(),
                  context.getStreamClock(),
                  context.getSchedulingService(),
                  context.getMessagingService(),
                  context.getMembershipService(),
                  context.getDispatchClient(),
                  context.getBrokerCfg(),
                  context.getClusterTopologyService());
          schedulingService
              .submitActor(engineService)
              .onComplete(
                  (_, throwable) -> {
                    if (throwable != null) {
                      future.completeExceptionally(throwable);
                    } else {
                      context.setDispatchProcessService(engineService);
                      future.complete(null);
                    }
                  });
        });
    return future;
  }
}
