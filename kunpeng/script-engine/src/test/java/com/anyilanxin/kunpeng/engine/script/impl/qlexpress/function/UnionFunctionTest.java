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

/** 通过表达式引擎端到端测试 {@code union()} 函数。 */
class UnionFunctionTest extends FunctionTestBase {

  @Test
  void unionsTwoListsWithoutDuplicates() {
    // 参考 FEEL 文档：union([1,2], [2,3]) -> [1, 2, 3]
    assertThat(eval("union([1, 2], [2, 3])").toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void unionsThreeLists() {
    assertThat(eval("union([1], [2], [3])").toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void unionWithEmptyList() {
    assertThat(eval("union([1, 2], [])").toString()).isEqualTo("[1, 2]");
  }
}
