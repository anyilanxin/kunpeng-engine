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
package com.anyilanxin.kunpeng.modules.broker.health;

import java.util.HashMap;
import java.util.List;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class BrokerHealthConfigurationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  public static final String INDICATOR_BROKER_LIVENESS = "brokerLiveness";
  public static final String INDICATOR_BROKER_READINESS = "brokerReadiness";
  public static final String INDICATOR_BROKER_STATUS = "brokerStatus";
  private static final String SPRING_READINESS_PROPERTY =
      "management.health.readinessstate.enabled";
  private static final String SPRING_PROBES_PROPERTY = "management.endpoint.health.probes.enabled";
  private static final String SPRING_DISKSPACE_PROPERTY = "management.health.diskSpace.enabled";
  private static final String SPRING_PING_PROPERTY = "management.health.ping.enabled";
  private static final String SPRING_SSL_PROPERTY = "management.health.ssl.enabled";
  private static final String SPRING_LIVENESS_GROUP_PROPERTY =
      "management.endpoint.health.group.liveness.include";
  private static final String SPRING_READINESS_GROUP_PROPERTY =
      "management.endpoint.health.group.readiness.include";
  private static final String SPRING_STATUS_GROUP_PROPERTY =
      "management.endpoint.health.group.status.include";

  @Override
  public void initialize(final ConfigurableApplicationContext context) {
    final var environment = context.getEnvironment();
    final var propertySources = environment.getPropertySources();
    final var propertyMap = new HashMap<String, Object>();
    propertyMap.put(SPRING_READINESS_PROPERTY, false);
    propertyMap.put(SPRING_PROBES_PROPERTY, false);
    propertyMap.put(SPRING_DISKSPACE_PROPERTY, false);
    propertyMap.put(SPRING_PING_PROPERTY, false);
    propertyMap.put(SPRING_SSL_PROPERTY, false);
    propertyMap.put(SPRING_LIVENESS_GROUP_PROPERTY, List.of(INDICATOR_BROKER_LIVENESS));
    propertyMap.put(SPRING_READINESS_GROUP_PROPERTY, List.of(INDICATOR_BROKER_READINESS));
    propertyMap.put(SPRING_STATUS_GROUP_PROPERTY, List.of(INDICATOR_BROKER_STATUS));
    DefaultPropertiesPropertySource.addOrMerge(propertyMap, propertySources);
  }
}
