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
 * 客户端异常基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class KunpengError extends RuntimeException {
  protected KunpengError(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static BpmnError bpmnError(
      final String errorCode,
      final String errorMessage,
      final Object variables,
      final Throwable cause) {
    return new BpmnError(errorCode, errorMessage, variables, cause);
  }

  public static BpmnError bpmnError(
      final String errorCode, final String errorMessage, final Object variables) {
    return bpmnError(errorCode, errorMessage, variables, null);
  }

  public static BpmnError bpmnError(final String errorCode, final String errorMessage) {
    return bpmnError(errorCode, errorMessage, null, null);
  }

  public static JobError jobError(final String errorMessage) {
    return jobError(errorMessage, null, null, null, null);
  }

  public static JobError jobError(final String errorMessage, final Object variables) {
    return jobError(errorMessage, variables, null, null, null);
  }

  public static JobError jobError(
      final String errorMessage, final Object variables, final Integer retries) {
    return jobError(errorMessage, variables, retries, null, null);
  }

  public static JobError jobError(
      final String errorMessage,
      final Object variables,
      final Integer retries,
      final Duration retryBackoff) {
    return jobError(errorMessage, variables, retries, retryBackoff, null);
  }

  public static JobError jobError(
      final String errorMessage,
      final Object variables,
      final Integer retries,
      final Duration retryBackoff,
      final Throwable cause) {
    return new JobError(errorMessage, variables, retries, retryBackoff, cause);
  }
}
