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
package com.anyilanxin.kunpeng.client.spring.exception;

import java.time.Duration;

/**
 * job 处理异常。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobError extends KunpengError {
  private final String errorMessage;
  private final Object variables;
  private final Integer retries;
  private final Duration retryBackoff;

  public JobError(
      final String errorMessage,
      final Object variables,
      final Integer retries,
      final Duration retryBackoff,
      final Throwable cause) {
    super(errorMessage, cause);
    this.errorMessage = errorMessage;
    this.variables = variables;
    this.retries = retries;
    this.retryBackoff = retryBackoff;
  }

  public JobError(final String errorMessage) {
    this(errorMessage, null, null, null, null);
  }

  public Object getVariables() {
    return variables;
  }

  public Integer getRetries() {
    return retries;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Duration getRetryBackoff() {
    return retryBackoff;
  }
}
