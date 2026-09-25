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
import com.anyilanxin.kunpeng.client.command.job.worker.BackoffSupplier;
import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 命令包装器：命令执行的重试与超时控制。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandWrapper {

  private final FinalCommandStep<?> command;
  private final ActivatedJob job;
  private final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
  private final MetricsRecorder metricsRecorder;

  private long currentRetryDelay = 50L;
  private int invocationCounter = 0;
  private final int maxRetries;

  public CommandWrapper(
      final FinalCommandStep<?> command,
      final ActivatedJob job,
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder,
      final int maxRetries) {
    this.command = command;
    this.job = job;
    this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
    this.metricsRecorder = metricsRecorder;
    this.maxRetries = maxRetries;
  }

  public void executeAsync() {
    invocationCounter++;
    command
        .send()
        .exceptionally(
            t -> {
              commandExceptionHandlingStrategy.handleCommandError(this, t);
              return null;
            });
  }

  public void executeAsyncWithMetrics(
      final String metricName, final String action, final String type) {
    invocationCounter++;
    command
        .send()
        .thenApply(
            result -> {
              metricsRecorder.increase(metricName, action, type);
              return result;
            })
        .exceptionally(
            t -> {
              commandExceptionHandlingStrategy.handleCommandError(this, t);
              return null;
            });
  }

  public void increaseBackoffUsing(final BackoffSupplier backoffSupplier) {
    currentRetryDelay = backoffSupplier.supplyRetryDelay(currentRetryDelay);
  }

  public void scheduleExecutionUsing(final ScheduledExecutorService scheduledExecutorService) {
    scheduledExecutorService.schedule(this::executeAsync, currentRetryDelay, TimeUnit.MILLISECONDS);
  }

  @Override
  public String toString() {
    return "{"
        + "command="
        + command.getClass()
        + ", job="
        + job
        + ", currentRetryDelay="
        + currentRetryDelay
        + '}';
  }

  public boolean hasMoreRetries() {
    if (jobDeadlineExceeded()) {
      // it does not make much sense to retry if the deadline is over, the job will be assigned to
      // another worker anyway
      return false;
    }
    return (invocationCounter < maxRetries);
  }

  private boolean jobDeadlineExceeded() {
    return (Instant.now().getEpochSecond() > job.getDeadline());
  }
}
