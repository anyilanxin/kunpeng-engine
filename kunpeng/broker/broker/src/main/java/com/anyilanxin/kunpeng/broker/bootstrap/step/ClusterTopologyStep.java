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
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.DefaultClusterTopologyService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/** 集群分区拓扑（Cluster Partition Topology）相关的 broker 启动步骤：创建拓扑服务并注册到上下文。 */
public final class ClusterTopologyStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Cluster Partition Topology";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final DefaultClusterSwimTopologyService partitionTopology =
        new DefaultClusterSwimTopologyService(
            brokerStartupContext.getAtomixCluster().getMembershipService(),
            brokerStartupContext.getMeterRegistry());
    // 只读视图：汇聚集群各成员广播的分区拓扑，供调度子系统等查询使用
    final DefaultClusterTopologyService clusterTopologyService =
        new DefaultClusterTopologyService(
            brokerStartupContext.getAtomixCluster().getMembershipService());
    brokerStartupContext
        .getActorSchedulingService()
        .submitActor(partitionTopology)
        .thenApply(
            ignored ->
                brokerStartupContext
                    .getActorSchedulingService()
                    .submitActor(clusterTopologyService))
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                startupFuture.completeExceptionally(throwable);
              } else {
                brokerStartupContext.setClusterPartitionTopology(partitionTopology);
                brokerStartupContext.setClusterTopologyService(clusterTopologyService);
                startupFuture.complete(brokerStartupContext);
              }
            });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final DefaultClusterSwimTopologyService partitionTopology =
        brokerShutdownContext.getClusterPartitionTopology();
    final DefaultClusterTopologyService clusterTopologyService =
        (DefaultClusterTopologyService) brokerShutdownContext.getClusterTopologyService();
    if (clusterTopologyService != null) {
      clusterTopologyService.closeAsync();
    }
    if (partitionTopology != null) {
      partitionTopology
          .closeAsync()
          .onComplete(
              (_, throwable) -> {
                if (throwable != null) {
                  shutdownFuture.completeExceptionally(throwable);
                } else {
                  brokerShutdownContext.setClusterPartitionTopology(null);
                  brokerShutdownContext.setClusterTopologyService(null);
                  shutdownFuture.complete(brokerShutdownContext);
                }
              });
    } else {
      shutdownFuture.complete(brokerShutdownContext);
    }
  }
}
