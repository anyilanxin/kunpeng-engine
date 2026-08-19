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

/** 通过表达式引擎端到端测试 {@code contextPut()} 函数。 */
class ContextPutFunctionTest extends FunctionTestBase {

  @Test
  void addsNewEntryToContext() {
    // 参考 FEEL 文档：contextPut({x:1}, ["y"], 2) -> {x:1, y:2}
    assertThat(eval("contextPut({\"x\": 1}, [\"y\"], 2)").toString()).isEqualTo("{x=1, y=2}");
  }

  @Test
  void overridesExistingEntry() {
    // contextPut({x:1}, ["x"], 99) -> {x:99}
    assertThat(eval("contextPut({\"x\": 1}, [\"x\"], 99)").toString()).isEqualTo("{x=99}");
  }

  @Test
  void addsNestedEntry() {
    // contextPut({x:1}, ["y", "z"], 2) -> {x:1, y:{z:2}}
    assertThat(eval("contextPut({\"x\": 1}, [\"y\", \"z\"], 2)").toString())
        .isEqualTo("{x=1, y={z=2}}");
  }
}
