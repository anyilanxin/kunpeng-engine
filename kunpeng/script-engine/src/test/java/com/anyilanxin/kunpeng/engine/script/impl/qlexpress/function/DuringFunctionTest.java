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

/** 通过表达式引擎端到端测试 {@code during()} 函数。 */
class DuringFunctionTest extends FunctionTestBase {

  @Test
  void pointDuringRange() {
    // 参考 FEEL 文档：during(5, [1..10]) -> true
    assertThat(eval("during(5, [1, 10])")).isEqualTo(true);
  }

  @Test
  void pointNotDuringRange() {
    // 参考 FEEL 文档：during(12, [1..10]) -> false
    assertThat(eval("during(12, [1, 10])")).isEqualTo(false);
  }

  @Test
  void rangeDuringRange() {
    // 参考 FEEL 文档：during([4..6], [1..10]) -> true
    assertThat(eval("during([4, 6], [1, 10])")).isEqualTo(true);
  }

  @Test
  void identicalRangeIsNotDuring() {
    // during 是严格的，相同区间不算 during
    assertThat(eval("during([1, 10], [1, 10])")).isEqualTo(false);
  }
}
