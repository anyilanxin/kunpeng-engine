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
package com.anyilanxin.kunpeng.modules.broker;

import com.anyilanxin.kunpeng.broker.Broker;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.modules.broker.configuration.BrokerPropertiesConfiguration;
import com.anyilanxin.kunpeng.modules.common.configuration.ClusterPropertiesConfiguration;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * broker module
 *
 * @author zxuanhong
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = {
      "com.anyilanxin.kunpeng.modules.broker",
      "com.anyilanxin.kunpeng.modules.common",
    })
@EnableAutoConfiguration
@Profile("broker")
public class BrokerModuleConfiguration {
  private final ActorSchedulingService schedulingService;
  private final BrokerPropertiesConfiguration.BrokerProperties brokerProperties;
  private final ClusterPropertiesConfiguration.ClusterProperties clusterProperties;
  private final AtomixCluster atomixCluster;
  private final MeterRegistry meterRegistry;
  private final ApplicationContext applicationContext;

  @Autowired
  public BrokerModuleConfiguration(
      final BrokerPropertiesConfiguration.BrokerProperties brokerProperties,
      final ClusterPropertiesConfiguration.ClusterProperties clusterProperties,
      final ActorSchedulingService schedulingService,
      final AtomixCluster atomixCluster,
      final MeterRegistry meterRegistry,
      final ApplicationContext applicationContext) {
    this.brokerProperties = brokerProperties;
    this.clusterProperties = clusterProperties;
    this.atomixCluster = atomixCluster;
    this.schedulingService = schedulingService;
    this.meterRegistry = meterRegistry;
    this.applicationContext = applicationContext;
  }

  @Bean(destroyMethod = "close")
  public Broker broker() {
    final Broker broker =
        new Broker(
            applicationContext,
            schedulingService,
            clusterProperties,
            brokerProperties,
            atomixCluster,
            meterRegistry);
    broker.start();
    return broker;
  }
}
