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
package com.anyilanxin.core;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller基类
 *
 * @author zxh
 * @date 2020-06-22 17:19
 * @since 1.0.0
 */
@Slf4j
@ApiResponses({@ApiResponse(responseCode = "200", description = "成功")})
public class AnYiBaseController {

  /**
   * 成功响应
   *
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:16
   */
  public static <T> AnYiResult<T> ok() {
    return new AnYiResult<>(AnYiResultStatus.SUCCESS);
  }

  /**
   * 成功响应
   *
   * @return AnYiResult<String> ${@link AnYiResult <String>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:16
   */
  public static AnYiResult<String> ok(final String message) {
    return new AnYiResult<>(AnYiResultStatus.SUCCESS, message);
  }

  /**
   * 成功响应
   *
   * @param data ${@link Object} 成功响应数据
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:16
   */
  public static <T> AnYiResult<T> ok(final T data) {
    return new AnYiResult<>(AnYiResultStatus.SUCCESS, data);
  }

  /**
   * 成功响应
   *
   * @param status ${@link Object} 成功响应数据
   * @return AnYiResult<T> ${@link AnYiResult < T >} 响应信息
   * @author zxh
   * @date 2020-06-22 17:16
   */
  public static AnYiResult<Object> ok(final AnYiResultStatus status) {
    return new AnYiResult<>(status);
  }

  /**
   * 成功响应
   *
   * @param data ${@link Object} 成功响应数据
   * @param message ${@link String} 成功响应消息
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:16
   */
  public static <T> AnYiResult<T> ok(final T data, final String message) {
    final AnYiResult<T> result = new AnYiResult<>(AnYiResultStatus.SUCCESS, data);
    result.setMessage(message);
    return result;
  }

  /**
   * 响应失败
   *
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:17
   */
  public static <T> AnYiResult<T> fail() {
    return new AnYiResult<>(AnYiResultStatus.ERROR);
  }

  /**
   * 响应失败
   *
   * @param status ${@link AnYiResultStatus} 失败状态
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:17
   */
  public static <T> AnYiResult<T> fail(final AnYiResultStatus status) {
    return new AnYiResult<>(status);
  }

  /**
   * 响应失败
   *
   * @param message ${@link String} 失败消息
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:17
   */
  public static <T> AnYiResult<T> fail(final String message) {
    return new AnYiResult<>(AnYiResultStatus.ERROR, message);
  }

  /**
   * 响应失败
   *
   * @param status ${@link AnYiResultStatus} 失败状态
   * @param message ${@link String} 失败消息
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:17
   */
  public static <T> AnYiResult<T> fail(final AnYiResultStatus status, final String message) {
    final AnYiResult<T> result = new AnYiResult<>(status);
    result.setMessage(message);
    return result;
  }

  /**
   * 响应失败
   *
   * @param code ${@link Integer} 失败状态码
   * @param message ${@link String} 失败消息
   * @return AnYiResult<T> ${@link AnYiResult <T>} 响应信息
   * @author zxh
   * @date 2020-06-22 17:17
   */
  public static <T> AnYiResult<T> fail(final Integer code, final String message) {
    return new AnYiResult<>(code, message);
  }
}
