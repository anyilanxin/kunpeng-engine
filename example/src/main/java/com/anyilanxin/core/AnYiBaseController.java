/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
