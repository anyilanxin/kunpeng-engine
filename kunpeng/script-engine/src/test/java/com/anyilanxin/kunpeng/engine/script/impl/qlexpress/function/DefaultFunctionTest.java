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

/** 通过表达式引擎端到端测试 {@code default()} 函数。 */
class DefaultFunctionTest extends FunctionTestBase {

  @Test
  void nullValueReturnsDefault() {
    assertThat(eval("default(null, \"fallback\")")).isEqualTo("fallback");
  }

  @Test
  void nonNullValueReturnsItself() {
    assertThat(eval("default(\"actual\", \"fallback\")")).isEqualTo("actual");
  }

  @Test
  void nonNullNumberReturnsItself() {
    assertThat(eval("default(42, 0)")).isEqualTo(42);
  }

  @Test
  void emptyStringIsNotReplaced() {
    // default 只对 null 替换，空串不替换
    assertThat(eval("default(\"\", \"fallback\")")).isEqualTo("");
  }
}
