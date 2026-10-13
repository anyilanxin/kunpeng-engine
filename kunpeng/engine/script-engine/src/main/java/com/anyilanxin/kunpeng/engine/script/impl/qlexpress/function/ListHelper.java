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

package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Helper：把 QL 传入的 list/collection/array 转为 Object 列表。 */
final class ListHelper {
  private ListHelper() {}

  static List<Object> toObjectList(final Object value) {
    if (value == null) {
      return new ArrayList<>();
    }
    final List<Object> result = new ArrayList<>();
    if (value instanceof Collection<?> coll) {
      for (final Object o : coll) {
        result.add(o);
      }
      return result;
    }
    if (value.getClass().isArray()) {
      final int len = java.lang.reflect.Array.getLength(value);
      for (int i = 0; i < len; i++) {
        result.add(java.lang.reflect.Array.get(value, i));
      }
      return result;
    }
    result.add(value);
    return result;
  }
}
