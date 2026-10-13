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

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** getEntries(context) — list of {key, value} */
public class GetEntriesFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() == 0) {
      return null;
    }
    final Object ctx = parameters.getValue(0);
    if (!(ctx instanceof Map<?, ?> map)) {
      return null;
    }
    final List<Map<String, Object>> result = new ArrayList<>();
    for (final Map.Entry<?, ?> e : map.entrySet()) {
      final Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("key", e.getKey());
      entry.put("value", e.getValue());
      result.add(entry);
    }
    return result;
  }

  @Override
  public String getSignature() {
    return "getEntries";
  }
}
