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
 * 通过表达式引擎端到端测试 {@code insertBefore()} 函数。
 *
 * <p><b>BUG MARKER</b>：FEEL 规范定义 {@code insertBefore(list, position, item)} 的位置从 <b>1</b> 开始
 * （例：{@code insertBefore([1,3], 1, 2) -> [2, 1, 3]}），但当前实现 {@link InsertBeforeFunction}
 * 用 0 基索引（{@code insertBefore([1,3], 1, 2) -> [1, 2, 3]}）。下方断言按 FEEL 规范写，预期 fail。
 */
class InsertBeforeFunctionTest extends FunctionTestBase {

  @Test
  void insertAtPositionOne() {
    // FEEL 规范：insertBefore([1,3], 1, 2) -> [2, 1, 3]（在位置 1 前插入）
    assertThat(eval("insertBefore([1, 3], 1, 2)").toString()).isEqualTo("[2, 1, 3]");
  }

  @Test
  void insertAtEndPosition() {
    // FEEL 规范：insertBefore([1,2], 3, 99) -> [1, 2, 99]（位置 3 是末尾后）
    assertThat(eval("insertBefore([1, 2], 3, 99)").toString()).isEqualTo("[1, 2, 99]");
  }

  @Test
  void insertIntoEmptyList() {
    // FEEL 规范：insertBefore([], 1, 5) -> [5]
    assertThat(eval("insertBefore([], 1, 5)").toString()).isEqualTo("[5]");
  }
}
