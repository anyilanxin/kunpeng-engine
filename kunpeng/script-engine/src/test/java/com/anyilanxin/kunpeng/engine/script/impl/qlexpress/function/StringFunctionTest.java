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

/** 通过表达式引擎端到端测试 {@code string()} 函数。 */
class StringFunctionTest extends FunctionTestBase {

  @Test
  void stringFromNumber() {
    // 参考 FEEL 文档：string(1.1) -> "1.1"
    assertThat(eval("string(1.1)")).isEqualTo("1.1");
  }

  @Test
  void stringFromString() {
    assertThat(eval("string(\"foo\")")).isEqualTo("foo");
  }

  @Test
  void stringFromBoolean() {
    assertThat(eval("string(true)")).isEqualTo("true");
  }
}
