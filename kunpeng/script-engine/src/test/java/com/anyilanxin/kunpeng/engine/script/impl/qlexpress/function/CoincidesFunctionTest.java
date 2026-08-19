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

/** 通过表达式引擎端到端测试 {@code coincides()} 函数。 */
class CoincidesFunctionTest extends FunctionTestBase {

  @Test
  void pointsCoincide() {
    // 参考 FEEL 文档：coincides(5, 5) -> true
    assertThat(eval("coincides(5, 5)")).isEqualTo(true);
  }

  @Test
  void pointsDoNotCoincide() {
    // 参考 FEEL 文档：coincides(3, 4) -> false
    assertThat(eval("coincides(3, 4)")).isEqualTo(false);
  }

  @Test
  void rangesCoincide() {
    // 参考 FEEL 文档：coincides([1..5], [1..5]) -> true
    assertThat(eval("coincides([1, 5], [1, 5])")).isEqualTo(true);
  }

  @Test
  void rangesDoNotCoincide() {
    // 参考 FEEL 文档：coincides([1..5], [2..6]) -> false
    assertThat(eval("coincides([1, 5], [2, 6])")).isEqualTo(false);
  }
}
