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

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

/**
 * 通用状态码
 *
 * @author zxh
 * @date 2020-06-22 16:35
 * @since 1.0.0
 */
@Getter
@ToString
public enum AnYiResultStatus {
  // ------------------------成功----------------
  /** 操作成功！ */
  SUCCESS(0, "操作成功！", HttpStatus.OK),

  /** 退出成功！ */
  LOGOUT(0, "退出成功！", HttpStatus.OK),

  // ------------------------需要重新登录----------------

  /** 未授权 */
  TOKEN_EXPIRED(4001, "未授权", HttpStatus.UNAUTHORIZED),

  /** 被提下线 */
  TOKEN_KICKED_OUT(4002, "被提下线", HttpStatus.UNAUTHORIZED),

  /** 禁止访问,权限不足 */
  TOKEN_LOGIN_ELSEWHERE(4003, "禁止访问,权限不足", HttpStatus.FORBIDDEN),

  // ------------------------操作异常------------------
  /** 操作异常！ */
  ERROR(5000, "操作异常！", HttpStatus.INTERNAL_SERVER_ERROR),

  /** 暂无权限访问！ */
  ACCESS_DENIED(4003, "暂无权限访问！", HttpStatus.FORBIDDEN),

  /** 授权异常！ */
  ACCESS_ERROR(4001, "授权异常！", HttpStatus.UNAUTHORIZED),

  /** 授权异常！ */
  ACCESS_INFO_ERROR(4012, "授权异常！", HttpStatus.PRECONDITION_FAILED),

  /** 请求不存在！ */
  REQUEST_NOT_FOUND(4004, "请求不存在！", HttpStatus.NOT_FOUND),

  /** 数据库操作失败 */
  DATABASE_BASE_ERROR(5000, "数据库操作失败", HttpStatus.INTERNAL_SERVER_ERROR),

  /** 验证失败 */
  VERIFICATION_FAILED(5000, "验证失败", HttpStatus.INTERNAL_SERVER_ERROR),

  /** 需要刷新 */
  NEED_REFRESH(4006, "需要刷新", HttpStatus.NOT_ACCEPTABLE),

  /** 调用第三方接口失败 */
  API_ERROR(5003, "调用第三方接口失败", HttpStatus.SERVICE_UNAVAILABLE);

  /** 状态码 */
  private final Integer code;

  /** 返回信息 */
  private final String message;

  /** http状态码 */
  private final HttpStatus status;

  AnYiResultStatus(final Integer code, final String message, final HttpStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }
}
