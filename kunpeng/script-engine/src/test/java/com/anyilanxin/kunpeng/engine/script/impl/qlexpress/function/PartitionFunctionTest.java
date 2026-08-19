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

/** 通过表达式引擎端到端测试 {@code partition()} 函数（按固定大小分块）。 */
class PartitionFunctionTest extends FunctionTestBase {

  @Test
  void partitionsEvenly() {
    // partition([1,2,3,4], 2) -> [[1, 2], [3, 4]]
    assertThat(eval("partition([1, 2, 3, 4], 2)").toString()).isEqualTo("[[1, 2], [3, 4]]");
  }

  @Test
  void partitionsWithRemainder() {
    // partition([1,2,3,4,5], 2) -> [[1, 2], [3, 4], [5]]
    assertThat(eval("partition([1, 2, 3, 4, 5], 2)").toString()).isEqualTo("[[1, 2], [3, 4], [5]]");
  }

  @Test
  void partitionSizeLargerThanList() {
    // partition([1,2,3], 10) -> [[1, 2, 3]]
    assertThat(eval("partition([1, 2, 3], 10)").toString()).isEqualTo("[[1, 2, 3]]");
  }
}
