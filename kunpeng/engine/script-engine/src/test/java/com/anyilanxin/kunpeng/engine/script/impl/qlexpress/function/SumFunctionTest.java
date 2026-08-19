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

/** 通过表达式引擎端到端测试 {@code sum()} 函数。 */
class SumFunctionTest extends FunctionTestBase {

  @Test
  void sumsListOfIntegers() {
    assertThat(eval("sum([1, 2, 3])")).isEqualTo(6.0);
  }

  @Test
  void sumsListOfDoubles() {
    assertThat(eval("sum([1.5, 2.5, 3.0])")).isEqualTo(7.0);
  }

  @Test
  void emptyListReturnsZero() {
    assertThat(eval("sum([])")).isEqualTo(0.0);
  }

  @Test
  void negativeValuesAreSummed() {
    assertThat(eval("sum([-1, -2, 3])")).isEqualTo(0.0);
  }

  @Test
  void composesWithAbs() {
    // sum([abs(-3), abs(-4)]) == 7.0
    assertThat(eval("sum([abs(-3), abs(-4)])")).isEqualTo(7.0);
  }
}
