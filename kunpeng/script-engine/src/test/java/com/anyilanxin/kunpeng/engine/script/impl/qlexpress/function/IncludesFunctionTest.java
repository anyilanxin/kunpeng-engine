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

/** 通过表达式引擎端到端测试 {@code includes()} 函数。 */
class IncludesFunctionTest extends FunctionTestBase {

  @Test
  void rangeIncludesPoint() {
    // 参考 FEEL 文档：includes([5..10], 6) -> true
    assertThat(eval("includes([5, 10], 6)")).isEqualTo(true);
  }

  @Test
  void rangeDoesNotIncludePoint() {
    // 参考 FEEL 文档：includes([3..4], 5) -> false
    assertThat(eval("includes([3, 4], 5)")).isEqualTo(false);
  }

  @Test
  void rangeIncludesRange() {
    // 参考 FEEL 文档：includes([1..10], [4..6]) -> true
    assertThat(eval("includes([1, 10], [4, 6])")).isEqualTo(true);
  }

  @Test
  void rangeDoesNotIncludeRange() {
    assertThat(eval("includes([1, 5], [4, 10])")).isEqualTo(false);
  }
}
