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

/** 通过表达式引擎端到端测试 {@code flatten()} 函数。 */
class FlattenFunctionTest extends FunctionTestBase {

  @Test
  void flattensOneLevelOfNesting() {
    // 参考 FEEL 文档：flatten([[1,2], [[3]], 4]) -> [1, 2, 3, 4]
    assertThat(eval("flatten([[1, 2], [[3]], 4])").toString()).isEqualTo("[1, 2, 3, 4]");
  }

  @Test
  void flattensAlreadyFlatList() {
    assertThat(eval("flatten([1, 2, 3])").toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void flattensEmptyList() {
    assertThat(eval("flatten([])").toString()).isEqualTo("[]");
  }
}
