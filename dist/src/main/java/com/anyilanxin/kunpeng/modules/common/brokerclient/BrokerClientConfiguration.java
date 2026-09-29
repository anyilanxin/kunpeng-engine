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

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.impl.BrokerClientImpl;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronMessagingService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class BrokerClientConfiguration {
  private final AtomixCluster cluster;
  private final ActorScheduler scheduler;
  private final ClusterTopologyService topologyManager;
  private final BrokerClientTimeoutConfiguration config;
  private final AeronMessagingService businessMessagingService;

  @Autowired
  public BrokerClientConfiguration(
      final BrokerClientTimeoutConfiguration config,
      final AtomixCluster cluster,
      final ActorScheduler scheduler,
      final ClusterTopologyService topologyManager,
      final AeronMessagingService businessMessagingService) {
    this.cluster = cluster;
    this.scheduler = scheduler;
    this.topologyManager = topologyManager;
    this.config = config;
    this.businessMessagingService = businessMessagingService;
  }

  @Bean(destroyMethod = "close")
  public BrokerClient brokerClient() {
    final var brokerClient =
        new BrokerClientImpl(
            // 业务命令走业务面消息服务(独立端口), 集群面只承载 SWIM/Raft/快照等集群流量
            businessMessagingService,
            cluster.getEventService(),
            topologyManager,
            cluster.getMembershipService(),
            cluster.getLeaderFoundService(),
            config.requestTimeout);
    scheduler.submitActor(brokerClient);
    return brokerClient;
  }

  public record BrokerClientTimeoutConfiguration(Duration requestTimeout) {}
}
