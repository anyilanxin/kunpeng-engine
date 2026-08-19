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
package com.anyilanxin.kunpeng.engine.script;

import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QlExpressLanguage;
import com.anyilanxin.kunpeng.utils.Either;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zxuanhong
 * @date 2025-12-22 16:11
 * @since
 */
public class QlTest {
  static void main() {
    final QlExpressLanguage expressLanguage = new QlExpressLanguage(null);
    final ScriptExpression parse = expressLanguage.parse("orderId>10");
//    final Either<String, Boolean> stringBooleanEither = parse.evaluateBoolean();
//    final ScriptExpression parse = expressLanguage.parse("20 between (10->30]");
    final Either<String, Object> stringBooleanEither = parse.evaluateObject(() -> {
      final Map<String, Object> value = new HashMap<>();
      value.put("orderId", 20);
      return value;
    });
    System.out.println(stringBooleanEither);

  }
}
