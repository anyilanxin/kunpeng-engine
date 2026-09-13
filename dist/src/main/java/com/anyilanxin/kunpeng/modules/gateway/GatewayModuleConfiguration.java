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
package com.anyilanxin.kunpeng.modules.gateway;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.gateway.Gateway;
import com.anyilanxin.kunpeng.modules.gateway.configuration.GatewayPropertiesConfiguration;
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
 * @author zxuanhong
 * @since
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = {
      "com.anyilanxin.kunpeng.modules.gateway",
      "com.anyilanxin.kunpeng.modules.common",
    })
@EnableAutoConfiguration
@Profile("gateway")
public class GatewayModuleConfiguration {

  private final ActorSchedulingService schedulingService;
  private final GatewayCfg gatewayCfg;
  private final AtomixCluster atomixCluster;
  private final MeterRegistry meterRegistry;

  @Autowired
  public GatewayModuleConfiguration(
      final ActorSchedulingService schedulingService,
      final GatewayPropertiesConfiguration.GatewayProperties gatewayCfg,
      final AtomixCluster atomixCluster,
      final MeterRegistry meterRegistry) {
    this.schedulingService = schedulingService;
    this.gatewayCfg = gatewayCfg;
    this.atomixCluster = atomixCluster;
    this.meterRegistry = meterRegistry;
  }

  @Bean(destroyMethod = "close")
  public Gateway gateway() {
    atomixCluster.start();
    final Gateway gateway =
        new Gateway(schedulingService, gatewayCfg, atomixCluster, meterRegistry);
    gateway.start().join(30, TimeUnit.SECONDS);
    return gateway;
  }
}
