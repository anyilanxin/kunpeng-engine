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

/** 通过表达式引擎端到端测试 {@code all()} 函数。 */
class AllFunctionTest extends FunctionTestBase {

  @Test
  void allTrueReturnsTrue() {
    // 参考 FEEL 文档：all([true, false]) -> false
    assertThat(eval("all([true, true])")).isEqualTo(true);
  }

  @Test
  void oneFalseReturnsFalse() {
    assertThat(eval("all([true, false])")).isEqualTo(false);
  }

  @Test
  void allFalseReturnsFalse() {
    assertThat(eval("all([false, false])")).isEqualTo(false);
  }

  @Test
  void emptyListReturnsTrue() {
    // 参考 FEEL 文档：0 个参数时返回 true
    assertThat(eval("all([])")).isEqualTo(true);
  }

  @Test
  void nullElementReturnsFalse() {
    // null 被 toBool 视为 false
    assertThat(eval("all([true, null])")).isEqualTo(false);
  }
}
