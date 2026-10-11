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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.core;

/**
 * REST 统一响应包装：code=0 表示成功，非 0 为失败码（400 参数校验、broker 拒绝透传其响应码、501 未实现、500 内部异常）。
 *
 * @param code 响应码，0 成功
 * @param success 是否成功
 * @param message 提示信息
 * @param data 业务数据
 * @param timestamp 服务器时间戳（ms）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record Result<T>(int code, boolean success, String message, T data, long timestamp) {

  public static <T> Result<T> ok(final T data) {
    return new Result<>(0, true, "ok", data, System.currentTimeMillis());
  }

  public static Result<Void> ok() {
    return ok(null);
  }

  public static <T> Result<T> fail(final int code, final String message) {
    return new Result<>(code, false, message, null, System.currentTimeMillis());
  }
}
