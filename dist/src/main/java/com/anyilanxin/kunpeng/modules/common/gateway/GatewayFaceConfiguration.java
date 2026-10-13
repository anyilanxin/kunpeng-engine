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
package com.anyilanxin.kunpeng.modules.common.gateway;

import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.gateway.Gateway;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManagerImpl;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关面共享件装配：job 中枢与 grpc 面拓扑视图定义为 bean，外部传入 Gateway（独立网关装配与 broker 内嵌网关启动链共用）。
 *
 * <p>条件启用：{@code kunpeng.broker.gateway.enable}（缺省 true）。broker 形态下即内嵌网关开关，关闭时本配置整体不生效；
 * 独立网关形态无此属性，恒生效。网关配置按形态注入对应 GatewayCfg bean（broker 取 {@code kunpeng.broker.gateway.*}，gateway 取
 * {@code kunpeng.gateway.*}）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "kunpeng.broker.gateway.enable",
    havingValue = "true",
    matchIfMissing = true)
public class GatewayFaceConfiguration {

  @Bean(destroyMethod = "shutdownNow")
  public ScheduledExecutorService gatewayJobHubScheduler() {
    return Executors.newSingleThreadScheduledExecutor(
        r -> {
          final Thread thread = new Thread(r, "gateway-job-hub");
          thread.setDaemon(true);
          return thread;
        });
  }

  @Bean
  public GatewayJobHub gatewayJobHub(
      final ScheduledExecutorService gatewayJobHubScheduler,
      final GatewayCfg gatewayCfg,
      final BrokerClient brokerClient) {
    final GatewayJobHub jobHub =
        new GatewayJobHub(gatewayJobHubScheduler, gatewayCfg.getLongPolling().getTimeout());
    jobHub.setBrokerClient(brokerClient);
    return jobHub;
  }

  @Bean
  public GatewayTopologyManagerImpl gatewayTopologyManager(
      final GatewayCfg gatewayCfg, final AtomixCluster atomixCluster) {
    return new GatewayTopologyManagerImpl(
        atomixCluster.getMembershipService(),
        atomixCluster.getCommunicationService(),
        Gateway.localGatewayInfo(gatewayCfg, atomixCluster));
  }
}
