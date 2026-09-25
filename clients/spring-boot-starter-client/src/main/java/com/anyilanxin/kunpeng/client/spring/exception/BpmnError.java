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

/**
 * Indicates an error in sense of BPMN occured, that should be handled by the BPMN process, see <a
 * href="https://docs.camunda.io/docs/reference/bpmn-processes/error-events/error-events/">...</a>
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnError extends KunpengError {
  private final String errorCode;
  private final String errorMessage;
  private final Object variables;

  public BpmnError(
      final String errorCode,
      final String errorMessage,
      final Object variables,
      final Throwable cause) {
    super("[" + errorCode + "] " + errorMessage, cause);
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.variables = variables;
  }

  public BpmnError(final String errorCode, final String errorMessage) {
    this(errorCode, errorMessage, null, null);
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Object getVariables() {
    return variables;
  }
}
