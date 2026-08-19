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

/** 通过表达式引擎端到端测试 {@code roundUp()} 函数（远离零舍入）。 */
class RoundUpFunctionTest extends FunctionTestBase {

  @Test
  void roundsUpPositiveValue() {
    // 参考 FEEL 文档：roundUp(5.5) -> 6
    assertThat(eval("roundUp(5.5, 0)")).isEqualTo(6.0);
  }

  @Test
  void roundsUpAwayFromZeroForNegative() {
    // 参考 FEEL 文档：roundUp(-5.5) -> -6（远离零）
    assertThat(eval("roundUp(-5.5, 0)")).isEqualTo(-6.0);
  }

  @Test
  void roundsUpAtGivenScale() {
    // 参考 FEEL 文档：roundUp(1.121, 2) -> 1.13
    assertThat(eval("roundUp(1.121, 2)")).isEqualTo(1.13);
  }

  @Test
  void roundsUpNegativeAtGivenScale() {
    // 参考 FEEL 文档：roundUp(-1.126, 2) -> -1.13
    assertThat(eval("roundUp(-1.126, 2)")).isEqualTo(-1.13);
  }
}
