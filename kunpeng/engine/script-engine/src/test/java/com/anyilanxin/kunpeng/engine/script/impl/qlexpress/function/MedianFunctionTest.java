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

/** 通过表达式引擎端到端测试 {@code median()} 函数。 */
class MedianFunctionTest extends FunctionTestBase {

  @Test
  void medianOfOddLengthIsMiddle() {
    // sorted: [3, 4, 5, 8, 2] -> [2,3,4,5,8], mid=4
    assertThat(eval("median([8, 2, 5, 3, 4])")).isEqualTo(4.0);
  }

  @Test
  void medianOfEvenLengthIsAverageOfTwoMiddles() {
    // sorted: [1,2,3,6] -> mid (2+3)/2 = 2.5
    assertThat(eval("median([6, 1, 2, 3])")).isEqualTo(2.5);
  }

  @Test
  void medianOfSingleValue() {
    assertThat(eval("median([42])")).isEqualTo(42.0);
  }

  @Test
  void medianUnsortedInputIsSorted() {
    // [3, 1, 2] -> sorted [1,2,3], median=2
    assertThat(eval("median([3, 1, 2])")).isEqualTo(2.0);
  }
}
