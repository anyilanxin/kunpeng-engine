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

/**
 * 通过表达式引擎端到端测试 {@code indexOf()} 函数。
 *
 * <p><b>BUG MARKER</b>：FEEL 规范定义 {@code indexOf} 返回 <b>1 基</b>位置列表
 * （例：{@code indexOf([1,2,3,2], 2) -> [2, 4]}），但当前实现 {@link IndexOfFunction}
 * 返回单个 <b>0 基</b>索引（返回 {@code 1L}）。下方断言按 FEEL 规范写，预期 fail。
 */
class IndexOfFunctionTest extends FunctionTestBase {

  @Test
  void indexOfPresentElement() {
    // FEEL 规范：indexOf([1,2,3,2], 2) -> [2, 4]（1 基，所有匹配位置）
    assertThat(eval("indexOf([1, 2, 3, 2], 2)").toString()).isEqualTo("[2, 4]");
  }

  @Test
  void indexOfFirstElement() {
    // FEEL 规范：indexOf([1,2,3], 1) -> [1]
    assertThat(eval("indexOf([1, 2, 3], 1)").toString()).isEqualTo("[1]");
  }

  @Test
  void indexOfAbsentElement() {
    // FEEL 规范：未找到时返回空列表 []，实现返回 -1
    assertThat(eval("indexOf([1, 2, 3], 9)").toString()).isEqualTo("[]");
  }
}
