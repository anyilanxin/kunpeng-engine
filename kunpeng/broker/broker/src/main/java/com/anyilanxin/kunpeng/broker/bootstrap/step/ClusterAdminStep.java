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
package com.anyilanxin.kunpeng.broker.bootstrap.step;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.manager.admin.ClusterAdminService;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/** 集群管理（Cluster Admin）相关的 broker 启动步骤。 */
public final class ClusterAdminStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Cluster Admin Service";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final AtomixCluster atomixCluster = brokerStartupContext.getAtomixCluster();
    final ActorSchedulingService actorSchedulingService =
        brokerStartupContext.getActorSchedulingService();
    final ClusterAdminService clusterManagerService =
        new ClusterAdminService(
            brokerStartupContext.getTimerClock(),
            brokerStartupContext.getBrokerConfiguration(),
            atomixCluster,
            atomixCluster.getMessagingService(),
            actorSchedulingService,
            brokerStartupContext.getMeterRegistry(),
            brokerStartupContext.getCommandApiService(),
            brokerStartupContext.getClusterMetaStore(),
            brokerStartupContext.getClusterDispatchClient(),
            brokerStartupContext.getClusterPartitionTopology(),
            brokerStartupContext.getClusterTopologyService());
    brokerStartupContext.getActorSchedulingService().submitActor(clusterManagerService);
    clusterManagerService
        .start()
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                startupFuture.completeExceptionally(throwable);
              } else {
                brokerStartupContext.setClusterManagerService(clusterManagerService);
                startupFuture.complete(brokerStartupContext);
              }
            });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final ClusterAdminService clusterManagerService =
        brokerShutdownContext.getClusterManagerService();
    if (clusterManagerService != null) {
      clusterManagerService
          .stop()
          .onComplete(
              (_, throwable) -> {
                if (throwable != null) {
                  shutdownFuture.completeExceptionally(throwable);
                } else {
                  brokerShutdownContext.setClusterManagerService(null);
                  shutdownFuture.complete(brokerShutdownContext);
                }
              });
    } else {
      shutdownFuture.complete(brokerShutdownContext);
    }
  }
}
