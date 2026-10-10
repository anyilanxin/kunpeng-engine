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

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * 响应结果封装
 *
 * @author zxh
 * @date 2020-06-22 16:29
 * @since 1.0.0
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode
@Schema
public class AnYiResult<T> implements Serializable {
  @Serial private static final long serialVersionUID = 2824340746431686918L;

  public static final String DATA_KEY = "data";
  public static final String CODE_KEY = "code";
  public static final String SUCCESS_KEY = "success";
  public static final String MESSAGE_KEY = "message";
  public static final String TIMESTAMP_KEY = "timestamp";

  @Schema(title = "响应状态码")
  private int code;

  @Schema(title = "成功标识")
  private boolean success;

  @Schema(title = "响应消息")
  private String message;

  @Schema(title = "响应数据")
  private T data;

  @Schema(title = "响应时间")
  private long timestamp;

  public AnYiResult() {}

  public AnYiResult(final AnYiResultStatus status) {
    setSuccess(status.getCode() == 0);
    setCode(status.getCode());
    setMessage(status.getMessage());
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final AnYiResultStatus status, final T data) {
    setSuccess(status.getCode() == 0);
    setCode(status.getCode());
    setMessage(status.getMessage());
    setData(data);
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final AnYiResultStatus status, final String message) {
    setSuccess(status.getCode() == 0);
    setCode(status.getCode());
    setMessage(message);
    setData(data);
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final AnYiResultStatus status, final String message, final T data) {
    setSuccess(status.getCode() == 0);
    setCode(status.getCode());
    setMessage(StringUtils.isNotBlank(message) ? message : status.getMessage());
    setData(data);
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final int code, final T data) {
    setSuccess(code == 0);
    setCode(code);
    setData(data);
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final int code, final String message) {
    setSuccess(code == 0);
    setCode(code);
    setMessage(message);
    timestamp = System.currentTimeMillis();
  }

  public AnYiResult(final int code, final String message, final T data) {
    setSuccess(code == 0);
    setCode(code);
    setData(data);
    setMessage(message);
    timestamp = System.currentTimeMillis();
  }
}
