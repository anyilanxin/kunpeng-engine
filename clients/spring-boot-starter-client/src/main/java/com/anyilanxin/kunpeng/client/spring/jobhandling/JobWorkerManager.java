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
package com.anyilanxin.kunpeng.client.spring.jobhandling;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.job.worker.BackoffSupplier;
import com.anyilanxin.kunpeng.client.command.job.worker.JobHandler;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorker;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerBuilderStep1;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolver;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolverStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessor;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessorStrategy;
import com.anyilanxin.kunpeng.client.spring.metrics.KunpengClientMetricsBridge;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job worker 管理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobWorkerManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobWorkerManager.class);

  private final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
  private final MetricsRecorder metricsRecorder;
  private final ParameterResolverStrategy parameterResolverStrategy;
  private final ResultProcessorStrategy resultProcessorStrategy;
  private final BackoffSupplier backoffSupplier;
  private final JobExceptionHandlingStrategy jobExceptionHandlingStrategy;

  private final List<JobWorker> openedWorkers = new ArrayList<>();
  private final List<JobWorkerValue> workerValues = new ArrayList<>();

  public JobWorkerManager(
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder,
      final ParameterResolverStrategy parameterResolverStrategy,
      final ResultProcessorStrategy resultProcessorStrategy,
      final BackoffSupplier backoffSupplier,
      final JobExceptionHandlingStrategy jobExceptionHandlingStrategy) {
    this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    this.metricsRecorder = metricsRecorder;
    this.parameterResolverStrategy = parameterResolverStrategy;
    this.resultProcessorStrategy = resultProcessorStrategy;
    this.backoffSupplier = backoffSupplier;
    this.jobExceptionHandlingStrategy = jobExceptionHandlingStrategy;
  }

  public JobWorker openWorker(final KunpengClient client, final JobWorkerValue jobWorkerValue) {
    final List<ParameterResolver> parameterResolvers =
        JobHandlingUtil.createParameterResolvers(parameterResolverStrategy, jobWorkerValue);
    final ResultProcessor resultProcessor =
        JobHandlingUtil.createResultProcessor(resultProcessorStrategy, jobWorkerValue);
    return openWorker(
        client,
        jobWorkerValue,
        new JobHandlerInvokingSpringBeans(
            jobWorkerValue,
            commandExceptionHandlingStrategy,
            metricsRecorder,
            parameterResolvers,
            resultProcessor,
            jobExceptionHandlingStrategy));
  }

  public JobWorker openWorker(
      final KunpengClient client, final JobWorkerValue jobWorkerValue, final JobHandler handler) {

    final JobWorkerBuilderStep1.JobWorkerBuilderStep3 builder =
        client
            .newWorker()
            .jobType(jobWorkerValue.getType())
            .handler(handler)
            .name(jobWorkerValue.getName())
            .backoffSupplier(backoffSupplier)
            .metrics(new KunpengClientMetricsBridge(metricsRecorder, jobWorkerValue.getType()));

    if (jobWorkerValue.getMaxJobsActive() != null && jobWorkerValue.getMaxJobsActive() > 0) {
      builder.maxJobsActive(jobWorkerValue.getMaxJobsActive());
    }
    if (isValidDuration(jobWorkerValue.getTimeout())) {
      builder.timeout(jobWorkerValue.getTimeout());
    }
    if (isValidDuration(jobWorkerValue.getPollInterval())) {
      builder.pollInterval(jobWorkerValue.getPollInterval());
    }
    if (isValidDuration(jobWorkerValue.getRequestTimeout())) {
      builder.requestTimeout(jobWorkerValue.getRequestTimeout());
    }
    if (jobWorkerValue.getFetchVariables() != null
        && !jobWorkerValue.getFetchVariables().isEmpty()) {
      builder.fetchVariables(jobWorkerValue.getFetchVariables());
    }
    if (jobWorkerValue.getTenantIds() != null && !jobWorkerValue.getTenantIds().isEmpty()) {
      builder.tenantIds(jobWorkerValue.getTenantIds());
    }
    if (jobWorkerValue.getStreamEnabled() != null) {
      builder.streamEnabled(jobWorkerValue.getStreamEnabled());
    }
    if (isValidDuration(jobWorkerValue.getStreamTimeout())) {
      builder.streamTimeout(jobWorkerValue.getStreamTimeout());
    }

    final JobWorker jobWorker = builder.open();
    openedWorkers.add(jobWorker);
    workerValues.add(jobWorkerValue);
    LOGGER.info(". Starting job worker: {}", jobWorkerValue);
    return jobWorker;
  }

  private boolean isValidDuration(final Duration duration) {
    return duration != null && !duration.isNegative();
  }

  public void closeAllOpenWorkers() {
    openedWorkers.forEach(JobWorker::close);
    openedWorkers.clear();
    workerValues.clear();
  }

  public void closeWorker(final JobWorker worker) {
    worker.close();
    final int i = openedWorkers.indexOf(worker);
    openedWorkers.remove(i);
    workerValues.remove(i);
  }
}
