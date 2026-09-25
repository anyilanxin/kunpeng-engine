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
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.worker.BackoffSupplier;
import com.anyilanxin.kunpeng.client.spring.annotation.customizer.JobWorkerValueCustomizer;
import com.anyilanxin.kunpeng.client.spring.configuration.condition.ConditionalOnKunpengClientEnabled;
import com.anyilanxin.kunpeng.client.spring.jobhandling.*;
import com.anyilanxin.kunpeng.client.spring.jobhandling.CommandExceptionHandlingStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.DefaultCommandExceptionHandlingStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.DefaultParameterResolverStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolverStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.DefaultResultProcessorStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessorStrategy;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import com.anyilanxin.kunpeng.client.spring.properties.PropertyBasedJobWorkerValueCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@ConditionalOnKunpengClientEnabled
@Import({
  AnnotationProcessorConfiguration.class,
  JsonMapperConfiguration.class,
})
@EnableConfigurationProperties({KunpengClientProperties.class})
/**
 * 客户端全量自动装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientAllAutoConfiguration {

  private final KunpengClientProperties kunpengClientProperties;

  public KunpengClientAllAutoConfiguration(final KunpengClientProperties kunpengClientProperties) {
    this.kunpengClientProperties = kunpengClientProperties;
  }

  @Bean
  @ConditionalOnMissingBean
  public KunpengClientExecutorService kunpengClientExecutorService() {
    return KunpengClientExecutorService.createDefault(
        kunpengClientProperties.getExecutionThreads());
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandExceptionHandlingStrategy commandExceptionHandlingStrategy(
      final KunpengClientExecutorService scheduledExecutorService) {
    return new DefaultCommandExceptionHandlingStrategy(
        backoffSupplier(), scheduledExecutorService.get());
  }

  @Bean
  @ConditionalOnMissingBean
  public ParameterResolverStrategy parameterResolverStrategy(
      final JsonMapper jsonMapper, @Autowired(required = false) final KunpengClient kunpengClient) {
    return new DefaultParameterResolverStrategy(jsonMapper, kunpengClient);
  }

  @Bean
  @ConditionalOnMissingBean
  public ResultProcessorStrategy resultProcessorStrategy() {
    return new DefaultResultProcessorStrategy();
  }

  @Bean
  @ConditionalOnMissingBean
  public JobExceptionHandlingStrategy jobExceptionHandlingStrategy(
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder) {
    return new DefaultJobExceptionHandlingStrategy(
        commandExceptionHandlingStrategy, metricsRecorder);
  }

  @Bean
  public JobWorkerManager jobWorkerManager(
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder,
      final ParameterResolverStrategy parameterResolverStrategy,
      final ResultProcessorStrategy resultProcessorStrategy,
      final BackoffSupplier backoffSupplier,
      final JobExceptionHandlingStrategy jobExceptionHandlingStrategy) {
    return new JobWorkerManager(
        commandExceptionHandlingStrategy,
        metricsRecorder,
        parameterResolverStrategy,
        resultProcessorStrategy,
        backoffSupplier,
        jobExceptionHandlingStrategy);
  }

  @Bean
  @ConditionalOnMissingBean
  public BackoffSupplier backoffSupplier() {
    return BackoffSupplier.newBackoffBuilder().build();
  }

  @Bean("propertyBasedJobWorkerValueCustomizer")
  @ConditionalOnMissingBean(name = "propertyBasedJobWorkerValueCustomizer")
  public JobWorkerValueCustomizer propertyBasedJobWorkerValueCustomizer() {
    return new PropertyBasedJobWorkerValueCustomizer(kunpengClientProperties);
  }
}
