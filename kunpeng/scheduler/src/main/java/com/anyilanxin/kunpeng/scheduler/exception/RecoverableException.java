/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.scheduler.exception;

/** 可恢复异常：retry 的 RecoverableRetryStrategy 仅对此类重试 */
public class RecoverableException extends RuntimeException {
  public RecoverableException(final String message) {
    super(message);
  }

  public RecoverableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public RecoverableException(final Throwable cause) {
    super(cause);
  }
}
