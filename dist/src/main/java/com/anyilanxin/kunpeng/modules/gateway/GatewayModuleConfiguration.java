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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.modules.gateway;

import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.gateway.Gateway;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManagerImpl;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.modules.gateway.configuration.GatewayPropertiesConfiguration;
import com.anyilanxin.kunpeng.protocol.common.encoding.GatewayInfo;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 网关模块装配配置。job 中枢与拓扑视图来自 modules/common 的 GatewayFaceConfiguration（共享 bean 注入）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = {
      "com.anyilanxin.kunpeng.modules.gateway",
      "com.anyilanxin.kunpeng.modules.common",
      "com.anyilanxin.kunpeng.gateway.rest",
    })
@EnableAutoConfiguration
@Profile("gateway")
public class GatewayModuleConfiguration {

  private final ActorSchedulingService schedulingService;
  private final GatewayCfg gatewayCfg;
  private final AtomixCluster atomixCluster;
  private final ClusterLeaderFoundService leaderService;
  private final BrokerClient brokerClient;
  private final MeterRegistry meterRegistry;
  private final ClusterTopologyService topologyManager;

  @Autowired
  public GatewayModuleConfiguration(
      final ActorSchedulingService schedulingService,
      final GatewayPropertiesConfiguration.GatewayProperties gatewayCfg,
      final AtomixCluster atomixCluster,
      final ClusterLeaderFoundService leaderService,
      final BrokerClient brokerClient,
      final MeterRegistry meterRegistry,
      final ClusterTopologyService topologyManager) {
    this.schedulingService = schedulingService;
    this.gatewayCfg = gatewayCfg;
    this.atomixCluster = atomixCluster;
    this.leaderService = leaderService;
    this.brokerClient = brokerClient;
    this.meterRegistry = meterRegistry;
    this.topologyManager = topologyManager;
  }

  @Bean(destroyMethod = "close")
  public Gateway gateway(
      final GatewayJobHub jobHub, final GatewayTopologyManagerImpl gatewayTopologyManager) {
    atomixCluster.start();
    // 网关发现管理: client 接入地址写入 GatewayInfo 实体属性(未显式通告主机时空串回落成员主机, 收集侧解析)
    GatewayInfo.advertise(
        atomixCluster.getMembershipService().getLocalMember().properties(),
        gatewayCfg.getNetwork().getGrpcHost(),
        gatewayCfg.getNetwork().getGrpcPort());
    final Gateway gateway =
        new Gateway(
            schedulingService,
            gatewayCfg,
            atomixCluster,
            meterRegistry,
            leaderService,
            brokerClient,
            topologyManager,
            gatewayTopologyManager,
            jobHub);
    gateway.start().join(30, TimeUnit.SECONDS);
    return gateway;
  }
}
