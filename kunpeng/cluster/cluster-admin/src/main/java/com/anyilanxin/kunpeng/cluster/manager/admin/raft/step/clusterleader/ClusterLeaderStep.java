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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.clusterleader;

import com.anyilanxin.kunpeng.cluster.manager.admin.raft.AdminPartitionStartupContext;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;

/**
 * 分区启动流程中的加入（join）步骤：创建 Raft 分区并加入已存在的集群，关闭时释放分区。
 *
 * @author zxuanhong
 */
@SuppressWarnings("rawtypes")
public class ClusterLeaderStep implements StartupStep<AdminPartitionStartupContext> {

  @Override
  public String getName() {
    return "Cluster Leader";
  }

  @Override
  public ActorFuture<AdminPartitionStartupContext> startup(
      final AdminPartitionStartupContext context) {
    final var result = context.getConcurrencyControl().<AdminPartitionStartupContext>createFuture();
    final DefaultClusterLeaderManageService leaderManageService =
        new DefaultClusterLeaderManageService(
            context.getPartitionManagementService().getMembershipService(),
            context.getPartitionManagementService().getClusterLeaderFoundService());
    final ActorSchedulingService actorSchedulingService = context.getActorSchedulingService();
    actorSchedulingService
        .submitActor(leaderManageService)
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                result.completeExceptionally(throwable);
              } else {
                context.setClusterLeaderManageService(leaderManageService);
                context.getRaftPartition().addRoleChangeListener(leaderManageService);
                result.complete(context);
              }
            });
    return result;
  }

  @Override
  public ActorFuture<AdminPartitionStartupContext> shutdown(
      final AdminPartitionStartupContext context) {
    final DefaultClusterLeaderManageService clusterLeaderManageService =
        context.getClusterLeaderManageService();
    final var result = context.getConcurrencyControl().<AdminPartitionStartupContext>createFuture();
    if (clusterLeaderManageService != null) {
      clusterLeaderManageService
          .closeAsync()
          .onComplete(
              (_, throwable) -> {
                if (throwable != null) {
                  result.completeExceptionally(throwable);
                } else {
                  context.setClusterLeaderManageService(null);
                  result.complete(null);
                }
              });
    } else {
      result.complete(null);
    }
    return result;
  }
}
