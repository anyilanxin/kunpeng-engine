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

/** 通过表达式引擎端到端测试 {@code max()} 函数。 */
class MaxFunctionTest extends FunctionTestBase {

  @Test
  void maximumOfPositiveNumbers() {
    assertThat(eval("max([1, 3, 2])")).isEqualTo(3.0);
  }

  @Test
  void maximumWithNegativeValues() {
    assertThat(eval("max([-5, 0, 5])")).isEqualTo(5.0);
  }

  @Test
  void maximumOfFractions() {
    assertThat(eval("max([1.5, 2.75, 0.25])")).isEqualTo(2.75);
  }

  @Test
  void singleElementListReturnsItself() {
    assertThat(eval("max([42])")).isEqualTo(42.0);
  }
}
