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

/** 通过表达式引擎端到端测试 {@code modulo()} 函数。 */
class ModuloFunctionTest extends FunctionTestBase {

  @Test
  void returnsRemainder() {
    // 12 % 5 == 2.0
    assertThat(eval("modulo(12, 5)")).isEqualTo(2.0);
  }

  @Test
  void exactDivisionReturnsZero() {
    assertThat(eval("modulo(10, 2)")).isEqualTo(0.0);
  }

  @Test
  void worksWithFractions() {
    // 5.5 % 2 == 1.5
    assertThat(eval("modulo(5.5, 2)")).isEqualTo(1.5);
  }

  @Test
  void dividendSmallerThanDivisor() {
    assertThat(eval("modulo(3, 7)")).isEqualTo(3.0);
  }
}
