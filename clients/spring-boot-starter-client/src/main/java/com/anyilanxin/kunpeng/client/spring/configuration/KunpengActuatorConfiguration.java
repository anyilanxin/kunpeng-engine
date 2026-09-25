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
package com.anyilanxin.kunpeng.client.spring.configuration;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.actuator.KunpengClientHealthIndicator;
import com.anyilanxin.kunpeng.client.spring.actuator.MicrometerMetricsRecorder;
import com.anyilanxin.kunpeng.client.spring.configuration.condition.ConditionalOnKunpengClientEnabled;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * actuator 指标装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoConfigureBefore(MetricsDefaultConfiguration.class)
@ConditionalOnKunpengClientEnabled
@ConditionalOnClass({EndpointAutoConfiguration.class, MeterRegistry.class})
public class KunpengActuatorConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public MetricsRecorder micrometerMetricsRecorder(@Lazy final MeterRegistry meterRegistry) {
    return new MicrometerMetricsRecorder(meterRegistry);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "management.health.kunpeng",
      name = "enabled",
      matchIfMissing = true)
  @ConditionalOnClass(HealthIndicator.class)
  @ConditionalOnMissingBean(name = "kunpengClientHealthIndicator")
  public KunpengClientHealthIndicator kunpengClientHealthIndicator(final KunpengClient client) {
    return new KunpengClientHealthIndicator(client);
  }
}
