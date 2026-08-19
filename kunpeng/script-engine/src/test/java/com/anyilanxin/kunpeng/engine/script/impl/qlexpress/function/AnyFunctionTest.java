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

import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.booleanandlogic.AllFunction;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.booleanandlogic.AnyFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过表达式引擎端到端测试 {@code any()} 函数。
 *
 * <p><b>BUG MARKER</b>：当前实现 {@link AnyFunction} 复用 {@link AllFunction#toBool(Object)}，
 * 而该 {@code toBool} 对集合的语义是"全部 true"，不是"任一 true"。因此 {@code any([false, true])}
 * 实际返回 false，与 FEEL 规范不符。下方断言按 FEEL 规范写，预期 fail。
 */
class AnyFunctionTest extends FunctionTestBase {

  @Test
  void oneTrueReturnsTrue() {
    // FEEL 规范：any([false, true]) -> true
    assertThat(eval("any([false, true])")).isEqualTo(true);
  }

  @Test
  void allFalseReturnsFalse() {
    assertThat(eval("any([false, false])")).isEqualTo(false);
  }

  @Test
  void emptyListReturnsFalse() {
    // FEEL 规范：0 个参数时返回 false
    assertThat(eval("any([])")).isEqualTo(false);
  }

  @Test
  void nullElementReturnsFalse() {
    // null 被 toBool 视为 false
    assertThat(eval("any([false, null])")).isEqualTo(false);
  }
}
