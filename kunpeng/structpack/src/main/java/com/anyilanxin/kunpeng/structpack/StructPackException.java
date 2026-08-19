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
package com.anyilanxin.kunpeng.structpack;

import com.anyilanxin.kunpeng.structpack.value.StringValue;

public class StructPackException extends RuntimeException {

  public StructPackException(final String message) {
    super(message);
  }

  public StructPackException(final StringValue key, final String message) {
    super(String.format("Property '%s' is invalid: %s", key, message));
  }

  public StructPackException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
