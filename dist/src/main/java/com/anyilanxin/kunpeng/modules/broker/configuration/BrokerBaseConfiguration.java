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
package com.anyilanxin.kunpeng.modules.broker.configuration;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterConfig;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterConfigFactory;
import com.anyilanxin.kunpeng.modules.common.actor.ActorSchedulerConfiguration;
import com.anyilanxin.kunpeng.modules.common.configuration.ClusterPropertiesConfiguration;
import com.anyilanxin.kunpeng.modules.gateway.configuration.GatewayPropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * broker configuration
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(value = {"broker", "restore"})
public class BrokerBaseConfiguration {
  private final ClusterPropertiesConfiguration.ClusterProperties clusterProperties;
  final BrokerPropertiesConfiguration.BrokerProperties brokerProperties;

  @Autowired
  public BrokerBaseConfiguration(
      final ClusterPropertiesConfiguration.ClusterProperties clusterProperties,
      final BrokerPropertiesConfiguration.BrokerProperties brokerProperties,
      final GatewayPropertiesConfiguration.GatewayProperties gatewayProperties,
      final BrokerWorkingDirectoryConfiguration.WorkingDirectory workingDirectory,
      final Environment environment) {
    this.clusterProperties = clusterProperties;
    this.brokerProperties = brokerProperties;
    final String basePath = workingDirectory.path().toAbsolutePath().toString();
    // 先初始化集群配置（start-port/通告地址等），broker 业务网络端口默认值由其派生；
    // 内嵌网关复用 kunpeng.gateway.* 配置，同样需要完成默认值初始化
    clusterProperties.init(basePath);
    brokerProperties.init(basePath, environment, clusterProperties);
    gatewayProperties.init(basePath);
  }

  @Bean
  public ClusterConfig clusterConfig() {
    final var configFactory = new ClusterConfigFactory();
    final var config = configFactory.mapConfiguration(clusterProperties, true);
    // 集群面与业务面双实例共享同一内嵌驱动(测试等多节点同 JVM 场景不经此处, 保持每实例独占)
    config.getAeronConfig().setSharedDriver(true);
    return config;
  }

  @Bean
  public ActorSchedulerConfiguration.SchedulerConfiguration schedulerConfiguration() {
    final var threadCfg = brokerProperties.getThreads();
    final var cpuThreads = threadCfg.getCpuThreadCount();
    final var ioThreads = threadCfg.getIoThreadCount();
    final var nodeId = String.valueOf(clusterProperties.getNodeId());
    return new ActorSchedulerConfiguration.SchedulerConfiguration(
        cpuThreads, ioThreads, "Broker", nodeId);
  }
}
