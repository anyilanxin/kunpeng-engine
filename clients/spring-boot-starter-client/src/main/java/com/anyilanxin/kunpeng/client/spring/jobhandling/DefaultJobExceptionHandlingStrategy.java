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

import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.FailJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.FailJobResponse;
import com.anyilanxin.kunpeng.client.command.job.ThrowErrorCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.exception.BpmnError;
import com.anyilanxin.kunpeng.client.spring.exception.JobError;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 job 异常处理策略。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultJobExceptionHandlingStrategy implements JobExceptionHandlingStrategy {
  private static final Logger LOG =
      LoggerFactory.getLogger(DefaultJobExceptionHandlingStrategy.class);
  private final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
  private final MetricsRecorder metricsRecorder;

  public DefaultJobExceptionHandlingStrategy(
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder) {
    this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    this.metricsRecorder = metricsRecorder;
  }

  private CommandWrapper createCommandWrapper(
      final FinalCommandStep<?> command, final ActivatedJob job, final JobWorkerValue workerValue) {
    return new CommandWrapper(
        command,
        job,
        commandExceptionHandlingStrategy,
        metricsRecorder,
        workerValue.getMaxRetries());
  }

  @Override
  public void handleException(final Exception exception, final ExceptionHandlingContext context)
      throws Exception {
    if (exception instanceof final JobError jobError) {
      LOG.trace("Caught job error on {}", context.job());
      final CommandWrapper command =
          createCommandWrapper(
              createFailJobCommand(context.jobClient(), context.job(), jobError),
              context.job(),
              context.jobWorkerValue());
      command.executeAsyncWithMetrics(
          MetricsRecorder.METRIC_NAME_JOB, MetricsRecorder.ACTION_FAILED, context.job().getType());
    } else if (exception instanceof final BpmnError bpmnError) {
      LOG.trace("Caught BPMN error on {}", context.job());
      final CommandWrapper command =
          createCommandWrapper(
              createThrowErrorCommand(context.jobClient(), context.job(), bpmnError),
              context.job(),
              context.jobWorkerValue());
      command.executeAsyncWithMetrics(
          MetricsRecorder.METRIC_NAME_JOB,
          MetricsRecorder.ACTION_BPMN_ERROR,
          context.job().getType());
    } else {
      metricsRecorder.increase(
          MetricsRecorder.METRIC_NAME_JOB, MetricsRecorder.ACTION_FAILED, context.job().getType());
      throw exception;
    }
  }

  private FinalCommandStep<Void> createThrowErrorCommand(
      final JobClient jobClient, final ActivatedJob job, final BpmnError bpmnError) {
    final ThrowErrorCommandStep1.ThrowErrorCommandStep2 command =
        jobClient
            .newThrowErrorCommand(job.getKey())
            .errorCode(bpmnError.getErrorCode())
            .errorMessage(bpmnError.getErrorMessage());
    return JobHandlingUtil.applyVariables(bpmnError.getVariables(), command);
  }

  private FinalCommandStep<FailJobResponse> createFailJobCommand(
      final JobClient jobClient, final ActivatedJob job, final JobError jobError) {
    final int retries =
        jobError.getRetries() == null ? (job.getRetries() - 1) : jobError.getRetries();
    final String errorMessage = JobHandlingUtil.createErrorMessage(jobError);
    final Duration backoff =
        jobError.getRetryBackoff() == null ? Duration.ZERO : jobError.getRetryBackoff();
    final FailJobCommandStep1.FailJobCommandStep2 command =
        jobClient
            .newFailCommand(job.getKey())
            .retries(retries)
            .errorMessage(errorMessage)
            .retryBackoff(backoff);
    return JobHandlingUtil.applyVariables(jobError.getVariables(), command);
  }
}
