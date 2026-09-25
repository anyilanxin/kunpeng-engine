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

import com.anyilanxin.kunpeng.client.ClientLoggers;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.CompleteJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.CompleteJobResponse;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobHandler;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.jobhandling.JobExceptionHandlingStrategy.ExceptionHandlingContext;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolver;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessor;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessorContext;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import java.util.List;
import org.slf4j.Logger;

/**
 * 调用 Spring bean 的 JobHandler 适配器
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobHandlerInvokingSpringBeans implements JobHandler {

  private static final Logger LOG = ClientLoggers.JOB_WORKER_LOGGER;
  private final JobWorkerValue workerValue;
  private final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
  private final MetricsRecorder metricsRecorder;
  private final List<ParameterResolver> parameterResolvers;
  private final ResultProcessor resultProcessor;
  private final JobExceptionHandlingStrategy jobExceptionHandlingStrategy;

  public JobHandlerInvokingSpringBeans(
      final JobWorkerValue workerValue,
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder,
      final List<ParameterResolver> parameterResolvers,
      final ResultProcessor resultProcessor,
      final JobExceptionHandlingStrategy jobExceptionHandlingStrategy) {
    this.workerValue = workerValue;
    this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    this.metricsRecorder = metricsRecorder;
    this.parameterResolvers = parameterResolvers;
    this.resultProcessor = resultProcessor;
    this.jobExceptionHandlingStrategy = jobExceptionHandlingStrategy;
  }

  @Override
  public void handle(final JobClient jobClient, final ActivatedJob job) throws Exception {
    final List<Object> args = createParameters(jobClient, job);
    LOG.trace("Handle {} and invoke worker {}", job, workerValue);
    metricsRecorder.increase(
        MetricsRecorder.METRIC_NAME_JOB, MetricsRecorder.ACTION_ACTIVATED, job.getType());
    try {
      final Object methodInvocationResult = workerValue.getMethodInfo().invoke(args.toArray());
      final Object result =
          resultProcessor.process(new ResultProcessorContext(methodInvocationResult, job));
      if (workerValue.getAutoComplete()) {
        LOG.trace("Auto completing {}", job);
        final CommandWrapper command =
            createCommandWrapper(createCompleteCommand(jobClient, job, result), job);
        command.executeAsyncWithMetrics(
            MetricsRecorder.METRIC_NAME_JOB, MetricsRecorder.ACTION_COMPLETED, job.getType());
      } else {
        if (result != null) {
          LOG.warn("Result provided but auto complete disabled for job {}", job);
        }
      }
    } catch (final Exception e) {
      jobExceptionHandlingStrategy.handleException(
          e, new ExceptionHandlingContext(jobClient, job, workerValue));
    }
  }

  private CommandWrapper createCommandWrapper(
      final FinalCommandStep<?> command, final ActivatedJob job) {
    return new CommandWrapper(
        command,
        job,
        commandExceptionHandlingStrategy,
        metricsRecorder,
        workerValue.getMaxRetries());
  }

  private List<Object> createParameters(final JobClient jobClient, final ActivatedJob job) {
    return parameterResolvers.stream().map(resolver -> resolver.resolve(jobClient, job)).toList();
  }

  private FinalCommandStep<CompleteJobResponse> createCompleteCommand(
      final JobClient jobClient, final ActivatedJob job, final Object result) {
    final CompleteJobCommandStep1 completeCommand = jobClient.newCompleteCommand(job.getKey());
    return JobHandlingUtil.applyVariables(result, completeCommand);
  }
}
