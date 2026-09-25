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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.repository;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryProcessService;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.google.common.collect.ImmutableSet;

/**
 * 业务面仓储服务分区迁移步骤。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RepositoryProcessServiceTransitionStep
    implements TransitionStep<BusinessTransitionContent> {
  @Override
  public String getName() {
    return "Business BusinessRepository ProcessService Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  @Override
  public ActorFuture<Void> onFollower(
      final BusinessTransitionContent context, final long currentTerm) {
    return start(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  private ActorFuture<Void> close(final BusinessTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final BusinessRepositoryProcessService repositoryProcessService =
              context.getRepositoryProcessService();
          if (repositoryProcessService != null) {
            repositoryProcessService.close();
          }
          future.complete(null);
        });
    return future;
  }

  private ActorFuture<Void> start(final BusinessTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          if (context.getRepositoryProcessService() == null) {
            final EventLog logStream = context.getEventLog();
            final ActorSchedulingService schedulingService = context.getSchedulingService();
            final BusinessRepositoryProcessService applierService =
                new BusinessRepositoryProcessService(
                    logStream,
                    context.getRepositoryFactory().create(),
                    new PartitionSourceMetadata(1, 1, ImmutableSet.<Integer>builder().build()),
                    context.getMeterRegistry());
            schedulingService
                .submitActor(applierService)
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        future.completeExceptionally(throwable);
                      } else {
                        context.setRepositoryProcessService(applierService);
                        future.complete(null);
                      }
                    });
          } else {
            future.complete(null);
          }
        });
    return future;
  }
}
