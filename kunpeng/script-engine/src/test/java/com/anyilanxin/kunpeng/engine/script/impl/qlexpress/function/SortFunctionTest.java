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

/** 通过表达式引擎端到端测试 {@code sort()} 函数（默认升序）。 */
class SortFunctionTest extends FunctionTestBase {

  @Test
  void sortsNumbersAscendingByDefault() {
    // sort([3,1,4,5,2]) 默认升序 -> [1, 2, 3, 4, 5]
    assertThat(eval("sort([3, 1, 4, 5, 2])").toString()).isEqualTo("[1, 2, 3, 4, 5]");
  }

  @Test
  void sortsNumbersDescending() {
    // 第二参数 false -> 降序
    assertThat(eval("sort([3, 1, 4, 5, 2], false)").toString()).isEqualTo("[5, 4, 3, 2, 1]");
  }

  @Test
  void alreadySortedRemainsSame() {
    assertThat(eval("sort([1, 2, 3])").toString()).isEqualTo("[1, 2, 3]");
  }
}
