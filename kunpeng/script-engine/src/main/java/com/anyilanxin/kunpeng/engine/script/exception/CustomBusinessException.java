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
package com.anyilanxin.kunpeng.engine.script.exception;

import com.alibaba.qlexpress4.runtime.Value;
import com.alibaba.qlexpress4.runtime.data.AssignableDataValue;

/**
 * @author zxuanhong
 * @date 2026-07-14 04:17
 * @since
 */
public class CustomBusinessException extends RuntimeException {

  public CustomBusinessException(final String message) {
    super(message);
  }

  public CustomBusinessException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public CustomBusinessException(final Throwable cause) {
    super(cause);
  }

  public CustomBusinessException(
      final String message,
      final Throwable cause,
      final boolean enableSuppression,
      final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public static Object checkParam(final String function, final Value value) {
    if (value.get() == null) {
      if (value instanceof final AssignableDataValue assignableDataValue) {
        throw new CustomBusinessException(
            function + " 函数缺少变量 " + assignableDataValue.getSymbolName());
      } else {
        throw new CustomBusinessException(function + " 函数缺少变量");
      }
    }
    return value.get();
  }
}
