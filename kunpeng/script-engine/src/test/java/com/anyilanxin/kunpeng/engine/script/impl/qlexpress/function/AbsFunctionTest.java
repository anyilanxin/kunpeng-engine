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

/** 通过表达式引擎端到端测试 {@code abs()} 函数。 */
class AbsFunctionTest extends FunctionTestBase {

  @Test
  void positiveValueIsUnchanged() {
    assertThat(eval("abs(10)")).isEqualTo(10.0);
  }

  @Test
  void negativeValueIsNegated() {
    assertThat(eval("abs(-10)")).isEqualTo(10.0);
  }

  @Test
  void fractionalValueIsHandled() {
    assertThat(eval("abs(-3.5)")).isEqualTo(3.5);
  }

  @Test
  void zeroIsZero() {
    assertThat(eval("abs(0)")).isEqualTo(0.0);
  }

  @Test
  void canComposeWithOtherExpressions() {
    // abs(-5) + abs(-2) == 7.0
    assertThat(eval("abs(-5) + abs(-2)")).isEqualTo(7.0);
  }
}
