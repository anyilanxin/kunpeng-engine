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

/** 通过表达式引擎端到端测试 {@code roundHalfDown()} 函数（最近舍入，.5 向下）。 */
class RoundHalfDownFunctionTest extends FunctionTestBase {

  @Test
  void roundsHalfDownPositiveValue() {
    // 参考 FEEL 文档：roundHalfDown(5.5, 0) -> 5
    assertThat(eval("roundHalfDown(5.5, 0)")).isEqualTo(5.0);
  }

  @Test
  void roundsHalfDownNegativeValue() {
    // 参考 FEEL 文档：roundHalfDown(-5.5, 0) -> -5
    assertThat(eval("roundHalfDown(-5.5, 0)")).isEqualTo(-5.0);
  }

  @Test
  void roundsHalfDownAtGivenScale() {
    // 参考 FEEL 文档：roundHalfDown(1.121, 2) -> 1.12
    assertThat(eval("roundHalfDown(1.121, 2)")).isEqualTo(1.12);
  }

  @Test
  void roundsHalfDownThirdDecimalUp() {
    // 参考 FEEL 文档：roundHalfDown(-1.126, 2) -> -1.13（.06 > .5 向上）
    assertThat(eval("roundHalfDown(-1.126, 2)")).isEqualTo(-1.13);
  }
}
