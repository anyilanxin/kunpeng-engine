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

/** 通过表达式引擎端到端测试 {@code before()} 函数。 */
class BeforeFunctionTest extends FunctionTestBase {

  @Test
  void pointBeforePoint() {
    // 参考 FEEL 文档：before(1, 10) -> true
    assertThat(eval("before(1, 10)")).isEqualTo(true);
  }

  @Test
  void pointNotBeforePoint() {
    // 参考 FEEL 文档：before(10, 1) -> false
    assertThat(eval("before(10, 1)")).isEqualTo(false);
  }

  @Test
  void rangeBeforePoint() {
    // 参考 FEEL 文档：before([1..5], 10) -> true
    assertThat(eval("before([1, 5], 10)")).isEqualTo(true);
  }

  @Test
  void pointBeforeRange() {
    // 参考 FEEL 文档：before(1, [2..5]) -> true
    assertThat(eval("before(1, [2, 5])")).isEqualTo(true);
  }

  @Test
  void rangeBeforeRange() {
    // 参考 FEEL 文档：before([1..5], [6..10]) -> true
    assertThat(eval("before([1, 5], [6, 10])")).isEqualTo(true);
  }
}
