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
package com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition;

import com.anyilanxin.kunpeng.cluster.business.step.transition.AbstractPartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.business.step.transition.DefaultPartitionTransitionService;
import com.anyilanxin.kunpeng.cluster.business.step.transition.PartitionTransition;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessPartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition.logstorage.BusinessLogStoragePartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition.logstream.BusinessLogStreamPartitionTransitionStep;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.List;

/** 业务分区启动流程中的分区状态迁移（transition）步骤，负责构建并提交分区迁移服务，关闭时释放相应资源。 */
public final class BusinessPartitionTransitionStep
    extends AbstractPartitionTransitionStep<BusinessPartitionStartupContext> {
  private static final List<TransitionStep<BusinessTransitionContent>> TRANSITION_STEPS =
      List.of(
          new BusinessLogStoragePartitionTransitionStep(),
          new BusinessLogStreamPartitionTransitionStep());

  @Override
  public String getName() {
    return "Business Partition Transition";
  }

  @Override
  public ActorFuture<BusinessPartitionStartupContext> startup(
      final BusinessPartitionStartupContext context) {
    final var result =
        context.getConcurrencyControl().<BusinessPartitionStartupContext>createFuture();
    final DefaultPartitionTransitionService<BusinessTransitionContent> transitionService =
        new DefaultPartitionTransitionService<>(TRANSITION_STEPS);
    final BusinessTransitionContent transitionContent =
        new BusinessTransitionContent(
            context.getSnapshotProvider(),
            context.getMeterRegistry(),
            context.getRaftPartition(),
            context.getBrokerCfg(),
            context.getActorSchedulingService(),
            context.getTimerClock(),
            context.getPartitionManagementService().getMessagingService());
    final PartitionTransition<BusinessTransitionContent> transition =
        new PartitionTransition<>(transitionService, context.getRaftPartition(), transitionContent);
    context.getActorSchedulingService().submitActor(transition);
    context
        .getConcurrencyControl()
        .runOnCompletion(
            transition.start(),
            (ignored, failure) -> {
              if (failure == null) {
                context.setPartitionTransition(transition);
                result.complete(context);
              } else {
                result.completeExceptionally(failure);
              }
            });

    return result;
  }
}
