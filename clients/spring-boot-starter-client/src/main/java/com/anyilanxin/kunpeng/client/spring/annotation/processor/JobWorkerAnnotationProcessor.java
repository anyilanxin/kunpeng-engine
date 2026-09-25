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
package com.anyilanxin.kunpeng.client.spring.annotation.processor;

import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.getJobWorkerValue;
import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.isJobWorker;
import static org.springframework.util.ReflectionUtils.doWithMethods;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.annotation.JobWorker;
import com.anyilanxin.kunpeng.client.spring.annotation.customizer.JobWorkerValueCustomizer;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import com.anyilanxin.kunpeng.client.spring.configuration.AnnotationProcessorConfiguration;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobWorkerManager;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * Always created by {@link AnnotationProcessorConfiguration}
 *
 * <p>Triggered by {@link AbstractKunpengAnnotationProcessor#onStart(KunpengClient)} to add Handler
 * subscriptions for {@link JobWorker} method-annotations.
 *
 * <p>Triggered by {@link AbstractKunpengAnnotationProcessor#onStop(KunpengClient)} to remove all
 * Handler subscriptions.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobWorkerAnnotationProcessor extends AbstractKunpengAnnotationProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobWorkerAnnotationProcessor.class);

  private final JobWorkerManager jobWorkerManager;

  private final List<JobWorkerValue> jobWorkerValues = new ArrayList<>();
  private final List<JobWorkerValueCustomizer> jobWorkerValueCustomizers;

  public JobWorkerAnnotationProcessor(
      final JobWorkerManager jobWorkerFactory,
      final List<JobWorkerValueCustomizer> jobWorkerValueCustomizers) {
    jobWorkerManager = jobWorkerFactory;
    this.jobWorkerValueCustomizers = jobWorkerValueCustomizers;
  }

  @Override
  public boolean isApplicableFor(final ClassInfo beanInfo) {
    return isJobWorker(beanInfo);
  }

  @Override
  public void configureFor(final ClassInfo beanInfo) {
    final List<JobWorkerValue> newJobWorkerValues = new ArrayList<>();

    doWithMethods(
        beanInfo.getTargetClass(),
        method ->
            getJobWorkerValue(beanInfo.toMethodInfo(method)).ifPresent(newJobWorkerValues::add),
        ReflectionUtils.USER_DECLARED_METHODS);

    LOGGER.info(
        "Configuring {} Job worker(s) of bean '{}': {}",
        newJobWorkerValues.size(),
        beanInfo.getBeanName(),
        newJobWorkerValues);
    jobWorkerValues.addAll(newJobWorkerValues);
  }

  @Override
  public void start(final KunpengClient client) {
    jobWorkerValues.stream()
        .peek(
            jobWorkerValue ->
                jobWorkerValueCustomizers.forEach(
                    customizer -> customizer.customize(jobWorkerValue)))
        .filter(JobWorkerValue::getEnabled)
        .forEach(
            jobWorkerValue -> {
              jobWorkerManager.openWorker(client, jobWorkerValue);
            });
  }

  @Override
  public void stop(final KunpengClient kunpengClient) {
    jobWorkerManager.closeAllOpenWorkers();
  }
}
