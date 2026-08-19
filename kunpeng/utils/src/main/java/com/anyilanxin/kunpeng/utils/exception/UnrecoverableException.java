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
package com.anyilanxin.kunpeng.utils.exception;

/**
 * 不可恢复异常
 *
 * @author zxuanhong
 * @date 2026-08-19 23:41
 * @since
 */
public class UnrecoverableException extends RuntimeException {

  public UnrecoverableException(final String message) {
    super(message);
  }

  public UnrecoverableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public UnrecoverableException(final Throwable cause) {
    super(cause);
  }
}

