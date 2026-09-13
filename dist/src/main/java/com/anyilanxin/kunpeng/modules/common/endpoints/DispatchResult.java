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
package com.anyilanxin.kunpeng.modules.common.endpoints;

/**
 * 调度端点统一响应结果。
 *
 * @author zxuanhong
 * @since
 */
public record DispatchResult(boolean success, int code, String message, Object data) {

  /** 请求异常（如等待调度响应超时）时的失败结果 */
  public static DispatchResult failure(final Throwable cause) {
    final String message =
        cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    return new DispatchResult(false, 500, message, null);
  }
}
