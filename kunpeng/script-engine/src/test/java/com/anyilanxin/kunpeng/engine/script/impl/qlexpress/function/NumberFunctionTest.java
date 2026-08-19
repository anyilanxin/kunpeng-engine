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

/** 通过表达式引擎端到端测试 {@code number()} 函数。 */
class NumberFunctionTest extends FunctionTestBase {

  @Test
  void parsesNumericString() {
    // 参考 FEEL 文档：number("1500.5") -> 1500.5
    assertThat(eval("number(\"1500.5\")")).isEqualTo(1500.5);
  }

  @Test
  void parsesIntegerString() {
    assertThat(eval("number(\"42\")")).isEqualTo(42.0);
  }

  @Test
  void returnsNumberAsIs() {
    // 已经是 Number 时原样返回；QLExpress 字面量 3.14 是 BigDecimal
    assertThat(eval("number(3.14)")).isEqualTo(new java.math.BigDecimal("3.14"));
  }

  @Test
  void nonNumericStringReturnsNull() {
    assertThat(eval("number(\"abc\")")).isNull();
  }
}
