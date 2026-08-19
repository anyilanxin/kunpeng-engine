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

/** 通过表达式引擎端到端测试 {@code overlaps()} 函数。 */
class OverlapsFunctionTest extends FunctionTestBase {

  @Test
  void rangesOverlap() {
    // 参考 FEEL 文档：overlaps([5..10], [1..6]) -> true
    assertThat(eval("overlaps([5, 10], [1, 6])")).isEqualTo(true);
  }

  @Test
  void rangesDoNotOverlap() {
    // 参考 FEEL 文档：overlaps([1..3], [4..6]) -> false
    assertThat(eval("overlaps([1, 3], [4, 6])")).isEqualTo(false);
  }

  @Test
  void identicalRangesOverlap() {
    // 参考 FEEL 文档：overlaps([1..5], [1..5]) -> true
    assertThat(eval("overlaps([1, 5], [1, 5])")).isEqualTo(true);
  }
}
