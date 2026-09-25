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
import com.anyilanxin.kunpeng.client.spring.configuration.condition.ConditionalOnKunpengClientEnabled;
import com.anyilanxin.kunpeng.client.spring.event.KunpengLifecycleEventProducer;
import com.anyilanxin.kunpeng.client.spring.testsupport.KunpengSpringProcessTestContext;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Enabled by META-INF of Spring Boot Starter to provide beans for kunpeng clients */
@Configuration
@ConditionalOnKunpengClientEnabled
@ImportAutoConfiguration({
  KunpengClientProdAutoConfiguration.class,
  KunpengClientAllAutoConfiguration.class,
  KunpengActuatorConfiguration.class,
  MetricsDefaultConfiguration.class,
  JsonMapperConfiguration.class,
})
// @AutoConfigureAfter(JacksonAutoConfiguration.class)
/**
 * 客户端自动装配入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(
      KunpengSpringProcessTestContext
          .class) // only run if we are not running in a test case - as otherwise the lifecycle
  // is controlled by the test
  public KunpengLifecycleEventProducer kunpengLifecycleEventProducer(
      final KunpengClient client, final ApplicationEventPublisher publisher) {
    return new KunpengLifecycleEventProducer(client, publisher);
  }
}
