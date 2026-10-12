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
package com.anyilanxin.kunpeng.modules.broker;

import com.anyilanxin.kunpeng.broker.Broker;
import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.impl.DefaultBusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.configuration.broker.EmbeddedGatewayCfg;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.gateway.Gateway;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.gateway.topology.GatewayDiscoverTopologyManager;
import com.anyilanxin.kunpeng.modules.gateway.configuration.GatewayPropertiesConfiguration;
import com.anyilanxin.kunpeng.protocol.common.encoding.GatewayInfo;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * broker 内嵌网关装配：将 broker 引导链启动的内嵌网关实例透出为 bean，REST 面一并加载。
 *
 * <p>条件启用：{@code kunpeng.broker.gateway.enable}（缺省 true，对应 {@link
 * EmbeddedGatewayCfg#isEnable()}）；网关配置取 {@code kunpeng.gateway.*}（与独立网关同一份）。置于 broker 包内由 broker
 * 装配扫描拾取， gateway 独立形态的扫描范围不含本包，两种形态天然互斥。内嵌网关实例由 {@code EmbeddedGatewayServiceStep} 创建，生命周期归 broker
 * 关闭链，此处仅透出不重建。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "kunpeng.broker.gateway.enable",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan(basePackages = "com.anyilanxin.kunpeng.gateway.rest")
public class EmbeddedGatewayConfiguration {
  private final ActorSchedulingService schedulingService;
  private final GatewayCfg gatewayCfg;
  private final AtomixCluster atomixCluster;

  @Autowired
  public EmbeddedGatewayConfiguration(
      final ActorSchedulingService schedulingService,
      final GatewayPropertiesConfiguration.GatewayProperties gatewayProperties,
      final AtomixCluster atomixCluster) {
    this.schedulingService = schedulingService;
    gatewayCfg = gatewayProperties;
    this.atomixCluster = atomixCluster;
  }

  /** 内嵌网关实例（引导链创建，不托管销毁；{@code Broker.start()} 幂等，join 等待引导完成） */
  @Bean(destroyMethod = "")
  public Gateway embeddedGateway(final Broker broker) {
    broker.start().join();
    // 网关发现管理: 内嵌网关 client 接入地址写入 GatewayInfo 实体属性(引擎引导链不负责通告, 装配侧补齐)
    GatewayInfo.advertise(
        atomixCluster.getMembershipService().getLocalMember().properties(),
        gatewayCfg.getNetwork().getGrpcHost(),
        gatewayCfg.getNetwork().getGrpcPort());
    return broker
        .getEmbeddedGateway()
        .orElseThrow(() -> new IllegalStateException("Embedded gateway is not available"));
  }

  /** gateway 地址发现收集服务(gateway 单边): 解析各网关成员的 GatewayInfo 实体汇总 client 接入地址 */
  @Bean(destroyMethod = "close")
  public GatewayDiscoverTopologyManager gatewayTopologyManager() {
    final GatewayDiscoverTopologyManager manager =
        new GatewayDiscoverTopologyManager(atomixCluster.getMembershipService());
    schedulingService.submitActor(manager);
    return manager;
  }

  /** REST 面(SSE 流订阅/长轮询)复用的 job 消费枢纽，与 gRPC 面同一实例 */
  @Bean
  public GatewayJobHub gatewayJobHub(final Gateway embeddedGateway) {
    return embeddedGateway.jobHub();
  }

  /** grpc 面网关拓扑视图透出（REST 集群查询域复用同一实例；生命周期归 Gateway 关闭链，不托管销毁） */
  @Bean(destroyMethod = "")
  public com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager
      grpcGatewayTopologyManager(final Gateway embeddedGateway) {
    return embeddedGateway.gatewayTopologyManager();
  }

  /** 业务命令面客户端（按域拆分、泛型完全确定），REST service 层统一经此下发命令 */
  @Bean
  public BusinessCommandClient businessCommandClient(final BrokerClient brokerClient) {
    return new DefaultBusinessCommandClient(brokerClient);
  }
}
