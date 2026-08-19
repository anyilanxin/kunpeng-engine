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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QlExpressLanguage;
import com.anyilanxin.kunpeng.utils.Either;

/**
 * 所有 *Function 测试的基类：通过表达式引擎 {@link QlExpressLanguage} 端到端求值。
 *
 * @author zxuanhong
 * @date 2025-12-22 16:11
 * @since
 */
class FunctionTestBase {
  /** 共享一个 language 实例（所有函数都在 loadFunction 中注册好了）。 */
  static final QlExpressLanguage LANGUAGE = new QlExpressLanguage(null);

  /**
   * 解析并求值表达式，断言成功后返回结果。
   *
   * @param expression 表达式
   * @return 求值结果
   */
  static Object eval(final String expression) {
    final Either<String, Object> either = LANGUAGE.parse(expression).evaluateObject();
    assertThat(either.isRight())
        .as(() -> "表达式求值失败: " + either.getLeft() + " (表达式: " + expression + ")")
        .isTrue();
    return either.get();
  }

  /**
   * 解析并求值表达式，返回 Either（不断言成功/失败，用于验证错误路径）。
   *
   * @param expression 表达式
   * @return Either
   */
  static Either<String, Object> evalEither(final String expression) {
    return LANGUAGE.parse(expression).evaluateObject();
  }
}
