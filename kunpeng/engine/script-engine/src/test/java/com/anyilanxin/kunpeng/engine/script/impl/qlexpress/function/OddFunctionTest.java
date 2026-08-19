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

import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code odd()} 函数。 */
class OddFunctionTest extends FunctionTestBase {

  @Test
  void oddNumberReturnsTrue() {
    assertThat(eval("odd(5)")).isEqualTo(true);
  }

  @Test
  void evenNumberReturnsFalse() {
    assertThat(eval("odd(2)")).isEqualTo(false);
  }

  @Test
  void oneIsOdd() {
    assertThat(eval("odd(1)")).isEqualTo(true);
  }

  @Test
  void negativeOdd() {
    // (-3).longValue() % 2 != 0 -> odd
    assertThat(eval("odd(-3)")).isEqualTo(true);
  }

  @Test
  void negativeEven() {
    assertThat(eval("odd(-4)")).isEqualTo(false);
  }
}
