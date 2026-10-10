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
