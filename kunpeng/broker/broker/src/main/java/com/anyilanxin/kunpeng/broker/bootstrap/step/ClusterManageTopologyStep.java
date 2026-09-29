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
import com.anyilanxin.kunpeng.broker.topology.TopologyManagerImpl;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 集群分区拓扑管理（Cluster Manage Topology）相关的 broker 启动步骤：创建拓扑管理服务并注册到上下文。
 *
 * <p>拓扑收集服务（ClusterTopologyService）为双边共用的 dist bean，经 Broker 构造参数注入上下文常驻，此处不再装配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ClusterManageTopologyStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Cluster Manage Topology";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    // 拓扑管理服务（broker 单边）：持有本进程唯一的 broker 传播实体，监听本节点数据变化写入成员属性（不做收集）
    final TopologyManagerImpl topologyManager =
        new TopologyManagerImpl(
            brokerStartupContext.getAtomixCluster().getMembershipService(),
            brokerStartupContext.getMeterRegistry(),
            new BrokerInfo());
    brokerStartupContext
        .getActorSchedulingService()
        .submitActor(topologyManager)
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                startupFuture.completeExceptionally(throwable);
              } else {
                brokerStartupContext.setClusterPartitionTopology(topologyManager);
                startupFuture.complete(brokerStartupContext);
              }
            });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final TopologyManagerImpl topologyManager =
        (TopologyManagerImpl) brokerShutdownContext.getClusterPartitionTopology();
    if (topologyManager != null) {
      topologyManager
          .closeAsync()
          .onComplete(
              (_, throwable) -> {
                if (throwable != null) {
                  shutdownFuture.completeExceptionally(throwable);
                } else {
                  brokerShutdownContext.setClusterPartitionTopology(null);
                  shutdownFuture.complete(brokerShutdownContext);
                }
              });
    } else {
      shutdownFuture.complete(brokerShutdownContext);
    }
  }
}
