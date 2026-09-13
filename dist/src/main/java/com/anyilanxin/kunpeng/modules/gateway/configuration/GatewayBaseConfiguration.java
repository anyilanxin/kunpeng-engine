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
package com.anyilanxin.kunpeng.modules.gateway.configuration;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterConfig;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterConfigFactory;
import com.anyilanxin.kunpeng.modules.common.actor.ActorSchedulerConfiguration;
import com.anyilanxin.kunpeng.modules.common.configuration.ClusterPropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * broker configuration
 *
 * @author zxuanhong
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(value = {"gateway", "restore"})
public class GatewayBaseConfiguration {
  private final GatewayPropertiesConfiguration.GatewayProperties gatewayProperties;
  private final ClusterPropertiesConfiguration.ClusterProperties clusterProperties;

  @Autowired
  public GatewayBaseConfiguration(
      final ClusterPropertiesConfiguration.ClusterProperties clusterProperties,
      final GatewayPropertiesConfiguration.GatewayProperties gatewayProperties,
      final GatewayWorkingDirectoryConfiguration.WorkingDirectory workingDirectory) {
    this.gatewayProperties = gatewayProperties;
    this.clusterProperties = clusterProperties;
    gatewayProperties.init(workingDirectory.path().toAbsolutePath().toString());
    clusterProperties.init(workingDirectory.path().toAbsolutePath().toString());
  }

  @Bean
  public ClusterConfig clusterConfig() {
    final var configFactory = new ClusterConfigFactory();
    return configFactory.mapConfiguration(clusterProperties, false);
  }

  @Bean
  public ActorSchedulerConfiguration.SchedulerConfiguration schedulerConfiguration() {
    final var threadCfg = gatewayProperties.getThreads();
    final var cpuThreads = threadCfg.getManagementThreads();
    final var ioThreads = 0;
    final var nodeId = String.valueOf(clusterProperties.getNodeId());
    return new ActorSchedulerConfiguration.SchedulerConfiguration(
        cpuThreads, ioThreads, "Gateway", nodeId);
  }
}
