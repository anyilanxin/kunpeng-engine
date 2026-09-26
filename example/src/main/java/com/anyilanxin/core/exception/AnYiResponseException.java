/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.core.exception;

import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.AnYiResultStatus;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.StringWriter;

/**
 * 响应异常
 *
 * @author zxh
 * @date 2020-06-22 16:24
 * @since 1.0.0
 */
public class AnYiResponseException extends RuntimeException {
  @Serial private static final long serialVersionUID = 7207809155561786625L;

  /** 错误异常结果 */
  private final AnYiResult<Object> result;

  public AnYiResponseException() {
    super(AnYiResultStatus.ERROR.getMessage());
    result = new AnYiResult<>(AnYiResultStatus.ERROR);
  }

  /**
   * 构造函数
   *
   * @param status 响应代码
   */
  public AnYiResponseException(final AnYiResultStatus status) {
    super(status.getMessage());
    result = new AnYiResult<>(status);
  }

  /**
   * 构造函数
   *
   * @param status 响应代码
   * @param data 业务数据
   */
  public AnYiResponseException(final AnYiResultStatus status, final Object data) {
    super(status.getMessage());
    result = new AnYiResult<>(status, data);
  }

  /**
   * 构造函数
   *
   * @param status 响应代码
   * @param message 异常消息
   */
  public AnYiResponseException(final AnYiResultStatus status, final String message) {
    super(message);
    result = new AnYiResult<>(status, message);
  }

  /**
   * 构造函数
   *
   * @param message 消息提示
   */
  public AnYiResponseException(final String message) {
    super(message);
    result = new AnYiResult<>(AnYiResultStatus.ERROR, message);
  }

  /**
   * 构造函数
   *
   * @param code 响应代码
   * @param message 消息提示
   */
  public AnYiResponseException(final int code, final String message) {
    super(message);
    result = new AnYiResult<>(code, message);
  }

  /**
   * 构造函数
   *
   * @param result HTTP响应接口输出结果实体
   */
  public AnYiResponseException(final AnYiResult<Object> result) {
    super(result.getMessage());
    this.result = result;
  }

  /**
   * 获取错误堆栈信息
   *
   * @param throwable
   * @return
   */
  public static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    try (final PrintWriter pw = new PrintWriter(sw)) {
      throwable.printStackTrace(pw);
      return sw.toString();
    }
  }

  public AnYiResult<Object> getResult() {
    return result;
  }

  /**
   * 获取错误堆栈信息
   *
   * @return
   */
  public String getStackTraceString() {
    return getStackTrace(this);
  }
}
