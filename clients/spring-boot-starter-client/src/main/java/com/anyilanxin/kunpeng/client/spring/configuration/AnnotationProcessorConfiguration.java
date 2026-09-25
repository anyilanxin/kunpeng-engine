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

import com.anyilanxin.kunpeng.client.spring.annotation.customizer.JobWorkerValueCustomizer;
import com.anyilanxin.kunpeng.client.spring.annotation.processor.DeploymentAnnotationProcessor;
import com.anyilanxin.kunpeng.client.spring.annotation.processor.JobWorkerAnnotationProcessor;
import com.anyilanxin.kunpeng.client.spring.annotation.processor.KunpengClientLifecycleAware;
import com.anyilanxin.kunpeng.client.spring.event.KunpengClientEventListener;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 注解处理器装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AnnotationProcessorConfiguration {

  @Bean
  public KunpengClientEventListener kunpengClientEventListener(
      final Set<KunpengClientLifecycleAware> kunpengClientLifecycleAwareSet) {
    return new KunpengClientEventListener(kunpengClientLifecycleAwareSet);
  }

  @Bean
  @ConditionalOnProperty(value = "kunpeng.client.deployment.enabled", matchIfMissing = true)
  public DeploymentAnnotationProcessor deploymentPostProcessor(
      final ApplicationEventPublisher publisher) {
    return new DeploymentAnnotationProcessor(publisher);
  }

  @Bean
  public JobWorkerAnnotationProcessor jobWorkerPostProcessor(
      final JobWorkerManager jobWorkerManager,
      final List<JobWorkerValueCustomizer> jobWorkerValueCustomizers) {
    return new JobWorkerAnnotationProcessor(jobWorkerManager, jobWorkerValueCustomizers);
  }
}
