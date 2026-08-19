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

/** 通过表达式引擎端到端测试 {@code isBlank()} 函数。 */
class IsBlankFunctionTest extends FunctionTestBase {

  @Test
  void blankStringReturnsTrue() {
    assertThat(eval("isBlank(\"   \")")).isEqualTo(true);
  }

  @Test
  void nonBlankStringReturnsFalse() {
    assertThat(eval("isBlank(\"hello\")")).isEqualTo(false);
  }

  @Test
  void emptyStringReturnsTrue() {
    assertThat(eval("isBlank(\"\")")).isEqualTo(true);
  }

  @Test
  void nullReturnsTrue() {
    // 无参时 isBlank() 也返回 true
    assertThat(eval("isBlank(null)")).isEqualTo(true);
  }
}
