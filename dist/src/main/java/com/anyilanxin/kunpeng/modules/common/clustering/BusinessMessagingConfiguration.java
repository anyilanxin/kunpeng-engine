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
package com.anyilanxin.kunpeng.modules.common.clustering;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.BusinessMessaging;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronMessagingService;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterConfigFactory;
import com.anyilanxin.kunpeng.modules.common.configuration.ClusterPropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 业务面消息服务装配。
 *
 * <p>此处仅构建 Bean；启动与端口广播由两侧各自的生命周期负责——broker 由 bootstrap 的 BusinessMessagingServiceStep
 * 启动（保证与其他启动步骤的顺序），网关在 Gateway 构造时启动。业务命令 RPC（BrokerClient、业务 CommandApiHandle）双端注入该实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
public final class BusinessMessagingConfiguration {

  private final ClusterPropertiesConfiguration.ClusterProperties clusterProperties;

  @Autowired
  public BusinessMessagingConfiguration(
      final ClusterPropertiesConfiguration.ClusterProperties clusterProperties) {
    this.clusterProperties = clusterProperties;
  }

  @Bean(destroyMethod = "stop")
  public AeronMessagingService businessMessagingService(final ClusterConfig clusterConfig) {
    // ClusterProperties 即 ClusterCfg 的 Spring 属性绑定子类
    final MessagingConfig messagingConfig =
        new ClusterConfigFactory().businessMessagingConfig(clusterProperties);
    final AeronMessagingConfig aeronConfig = clusterConfig.getAeronConfig();
    return BusinessMessaging.buildService(messagingConfig, aeronConfig);
  }
}
