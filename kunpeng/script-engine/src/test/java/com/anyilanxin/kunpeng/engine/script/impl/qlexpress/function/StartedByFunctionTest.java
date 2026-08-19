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

/** 通过表达式引擎端到端测试 {@code startedBy()} 函数。 */
class StartedByFunctionTest extends FunctionTestBase {

  @Test
  void rangeStartedByRange() {
    // 参考 FEEL 文档：startedBy([1..10], [1..5]) -> true（起点相同，a 更晚结束）
    assertThat(eval("startedBy([1, 10], [1, 5])")).isEqualTo(true);
  }

  @Test
  void rangeStartedByPoint() {
    // 参考 FEEL 文档：startedBy([1..10], 1) -> true
    assertThat(eval("startedBy([1, 10], 1)")).isEqualTo(true);
  }

  @Test
  void differentStartReturnsFalse() {
    assertThat(eval("startedBy([2, 10], [1, 5])")).isEqualTo(false);
  }
}
