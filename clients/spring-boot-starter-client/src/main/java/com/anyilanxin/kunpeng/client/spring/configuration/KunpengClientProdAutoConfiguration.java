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

import com.anyilanxin.kunpeng.client.ExecutorResource;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.KunpengClientImpl;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.spring.configuration.condition.ConditionalOnKunpengClientEnabled;
import com.anyilanxin.kunpeng.client.spring.jobhandling.KunpengClientExecutorService;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import com.anyilanxin.kunpeng.client.spring.testsupport.KunpengSpringProcessTestContext;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/*
 * All configurations that will only be used in production code - meaning NO TEST cases
 */
@ConditionalOnKunpengClientEnabled
@ConditionalOnMissingBean(KunpengSpringProcessTestContext.class)
@ImportAutoConfiguration({
  ExecutorServiceConfiguration.class,
  KunpengActuatorConfiguration.class,
  JsonMapperConfiguration.class,
  CredentialsProviderConfiguration.class
})
@AutoConfigureBefore(KunpengClientAllAutoConfiguration.class)
@EnableConfigurationProperties(KunpengClientProperties.class)
/**
 * 客户端生产环境自动装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientProdAutoConfiguration {

  private static final Logger LOG =
      LoggerFactory.getLogger(KunpengClientProdAutoConfiguration.class);

  @Bean
  public KunpengClientConfiguration kunpengClientConfiguration(
      final KunpengClientProperties kunpengClientProperties,
      @Qualifier(value = "kunpengJsonMapper") final JsonMapper jsonMapper,
      final List<ClientInterceptor> interceptors,
      final KunpengClientExecutorService kunpengClientExecutorService,
      final CredentialsProvider kunpengClientCredentialsProvider) {
    return new SpringKunpengClientConfiguration(
        kunpengClientProperties,
        jsonMapper,
        interceptors,
        kunpengClientExecutorService,
        kunpengClientCredentialsProvider);
  }

  @Bean(destroyMethod = "close")
  public KunpengClient kunpengClient(final KunpengClientConfiguration configuration) {
    LOG.debug("Creating kunpengClient using {}", configuration);
    final ScheduledExecutorService jobWorkerExecutor = configuration.jobWorkerExecutor();
    if (jobWorkerExecutor != null) {
      final ManagedChannel managedChannel = KunpengClientImpl.buildChannel(configuration);
      final ExecutorResource executorResource =
          new ExecutorResource(jobWorkerExecutor, configuration.ownsJobWorkerExecutor());
      return new KunpengClientImpl(configuration, managedChannel, executorResource);
    } else {
      return new KunpengClientImpl(configuration);
    }
  }
}
