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
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code log()} 函数（自然对数）。 */
class LogFunctionTest extends FunctionTestBase {

  @Test
  void logOfOneIsZero() {
    assertThat(eval("log(1)")).isEqualTo(0.0);
  }

  @Test
  void logOfEIsOne() {
    assertThat((double) eval("log(2.718281828459045)")).isCloseTo(1.0, within(1e-9));
  }

  @Test
  void logOfTen() {
    // ln(10) ≈ 2.302585092994046
    assertThat((double) eval("log(10)")).isCloseTo(2.302585092994046, within(1e-9));
  }

  @Test
  void logOfFractionIsNegative() {
    // ln(0.5) ≈ -0.6931471805599453
    assertThat((double) eval("log(0.5)")).isCloseTo(-0.6931471805599453, within(1e-9));
  }
}
