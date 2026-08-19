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

/** 通过表达式引擎端到端测试 {@code roundDown()} 函数（趋向零舍入）。 */
class RoundDownFunctionTest extends FunctionTestBase {

  @Test
  void roundsDownPositiveValue() {
    // 参考 FEEL 文档：roundDown(5.5, 0) -> 5
    assertThat(eval("roundDown(5.5, 0)")).isEqualTo(5.0);
  }

  @Test
  void roundsDownTowardZeroForNegative() {
    // 参考 FEEL 文档：roundDown(-5.5, 0) -> -5（趋向零）
    assertThat(eval("roundDown(-5.5, 0)")).isEqualTo(-5.0);
  }

  @Test
  void roundsDownAtGivenScale() {
    // 参考 FEEL 文档：roundDown(1.121, 2) -> 1.12
    assertThat(eval("roundDown(1.121, 2)")).isEqualTo(1.12);
  }

  @Test
  void roundsDownNegativeAtGivenScale() {
    // 参考 FEEL 文档：roundDown(-1.126, 2) -> -1.12
    assertThat(eval("roundDown(-1.126, 2)")).isEqualTo(-1.12);
  }
}
