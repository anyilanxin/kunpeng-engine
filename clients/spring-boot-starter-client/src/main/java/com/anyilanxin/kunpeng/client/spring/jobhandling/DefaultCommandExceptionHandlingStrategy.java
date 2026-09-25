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

import com.anyilanxin.kunpeng.client.command.job.worker.BackoffSupplier;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认命令异常处理策略。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultCommandExceptionHandlingStrategy implements CommandExceptionHandlingStrategy {

  public static final Set<Status.Code> RETRIABLE_CODES =
      EnumSet.of(
          Status.Code.CANCELLED,
          Status.Code.DEADLINE_EXCEEDED,
          Status.Code.RESOURCE_EXHAUSTED,
          Status.Code.ABORTED,
          Status.Code.UNAVAILABLE,
          Status.Code.DATA_LOSS);
  public static final Set<Status.Code> IGNORABLE_FAILURE_CODES = EnumSet.of(Status.Code.NOT_FOUND);
  public static final Set<Status.Code> FAILURE_CODES =
      EnumSet.of(
          Status.Code.INVALID_ARGUMENT,
          Status.Code.PERMISSION_DENIED,
          Status.Code.FAILED_PRECONDITION,
          Status.Code.OUT_OF_RANGE,
          Status.Code.UNIMPLEMENTED,
          Status.Code.INTERNAL,
          Status.Code.UNAUTHENTICATED);
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final BackoffSupplier backoffSupplier;
  private final ScheduledExecutorService scheduledExecutorService;

  public DefaultCommandExceptionHandlingStrategy(
      final BackoffSupplier backoffSupplier,
      final ScheduledExecutorService scheduledExecutorService) {
    this.backoffSupplier = backoffSupplier;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void handleCommandError(final CommandWrapper command, final Throwable throwable) {
    if (StatusRuntimeException.class.isAssignableFrom(throwable.getClass())) {
      final StatusRuntimeException exception = (StatusRuntimeException) throwable;
      final Status.Code code = exception.getStatus().getCode();

      if (!RETRIABLE_CODES.contains(code)
          || !IGNORABLE_FAILURE_CODES.contains(code)
          || FAILURE_CODES.contains(code)) {
        throw new RuntimeException(
            "Could not execute " + command + " due to exception: " + throwable.getMessage(),
            throwable);
      }

      if (RETRIABLE_CODES.contains(code)) {
        if (!command.hasMoreRetries()) {
          throw new RuntimeException(
              "Could not execute "
                  + command
                  + " due to error of type '"
                  + code
                  + "' and no retries are left",
              throwable);
        }
        command.increaseBackoffUsing(backoffSupplier);
        LOG.warn("Retrying {} after error of type '{}' with backoff", command, code);
        command.scheduleExecutionUsing(scheduledExecutorService);
        return;
      }

      throw new RuntimeException(
          "Could not execute " + command + " due to error of type '" + code + "'", throwable);
    }
  }
}
