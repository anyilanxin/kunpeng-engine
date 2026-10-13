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
package com.anyilanxin.kunpeng.modules.common.brokerclient;

import com.anyilanxin.kunpeng.broker.client.business.BusinessBrokerPlane;
import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.impl.DefaultBusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.impl.BrokerClientImpl;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class BrokerClientConfiguration {
  private final AtomixCluster cluster;
  private final ActorScheduler scheduler;
  private final ClusterTopologyService topologyManager;

  @Autowired
  public BrokerClientConfiguration(
      final AtomixCluster cluster,
      final ActorScheduler scheduler,
      final ClusterTopologyService topologyManager) {
    this.cluster = cluster;
    this.scheduler = scheduler;
    this.topologyManager = topologyManager;
  }

  @Bean(destroyMethod = "close")
  public BrokerClient brokerClient() {
    final var brokerClient =
        new BrokerClientImpl(
            // 发送侧直接使用集群面 Atomix 通信组件, 按分区 leader 广播的业务端口寻址(接收端为 broker 业务面独立实例)
            cluster.getMessagingService(),
            cluster.getEventService(),
            new BusinessBrokerPlane(topologyManager),
            topologyManager);
    scheduler.submitActor(brokerClient);
    return brokerClient;
  }

  /** 业务命令面客户端（按域拆分、泛型完全确定），REST service 层统一经此下发命令 */
  @Bean
  public BusinessCommandClient businessCommandClient(final BrokerClient brokerClient) {
    return new DefaultBusinessCommandClient(brokerClient);
  }
}
