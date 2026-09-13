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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition;

import com.anyilanxin.kunpeng.cluster.business.step.transition.AbstractPartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.business.step.transition.DefaultPartitionTransitionService;
import com.anyilanxin.kunpeng.cluster.business.step.transition.PartitionTransition;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.AdminPartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.apicommand.CommandApiServiceTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.dispatch.DispatchProcessServiceTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstorage.AdminLogStoragePartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstream.AdminLogStreamPartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.repository.RepositoryProcessServiceTransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.rocksdb.RocksdbPartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.List;

/** 管理分区启动流程中的分区转换（transition）步骤，启动时创建并提交管理分区转换，关闭时将其释放。 */
public final class AdminPartitionTransitionStep
    extends AbstractPartitionTransitionStep<AdminPartitionStartupContext> {
  private static final List<TransitionStep<AdminTransitionContent>> TRANSITION_STEPS =
      List.of(
          new AdminLogStoragePartitionTransitionStep(),
          new AdminLogStreamPartitionTransitionStep(),
          new RocksdbPartitionTransitionStep(),
          new RepositoryProcessServiceTransitionStep(),
          new DispatchProcessServiceTransitionStep(),
          new CommandApiServiceTransitionStep());

  @Override
  public String getName() {
    return "Admin Partition Transition";
  }

  @Override
  public ActorFuture<AdminPartitionStartupContext> startup(
      final AdminPartitionStartupContext context) {
    final var result = context.getConcurrencyControl().<AdminPartitionStartupContext>createFuture();
    final DefaultPartitionTransitionService<AdminTransitionContent> transitionService =
        new DefaultPartitionTransitionService<>(TRANSITION_STEPS);
    final PartitionManagementService partitionManagementService =
        context.getPartitionManagementService();
    final MessagingService messagingService = partitionManagementService.getMessagingService();
    final ClusterMembershipService membershipService =
        partitionManagementService.getMembershipService();
    final AdminTransitionContent transitionContent =
        new AdminTransitionContent(
            context.getClusterMetaStore(),
            context.getSnapshotProvider(),
            context.getMeterRegistry(),
            context.getRaftPartition(),
            context.getBrokerCfg(),
            context.getActorSchedulingService(),
            context.getCommandApiHandle(),
            context.getTimerClock(),
            messagingService,
            membershipService,
            context.getDispatchClient(),
            context.getClusterTopologyService());
    final PartitionTransition<AdminTransitionContent> transition =
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
