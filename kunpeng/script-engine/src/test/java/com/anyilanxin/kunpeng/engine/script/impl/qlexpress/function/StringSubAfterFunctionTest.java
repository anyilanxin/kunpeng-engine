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
 * 通过表达式引擎端到端测试 {@code substringAfter()} 函数。
 *
 * <p><b>BUG MARKER</b>：当 match 未找到时，FEEL 规范期望返回原字符串，但当前实现
 * {@link StringSubAfterFunction} 用 {@code indexOf(match) + match.length()} 作起点，
 * 未找到时 indexOf 返回 -1，得到 {@code substring(len-1)} 而非原字符串。
 * {@code noMatchReturnsEntireString} 用例按规范写，预期 fail，待实现修复后转绿。
 */
class StringSubAfterFunctionTest extends FunctionTestBase {

  @Test
  void returnsPortionAfterFirstMatch() {
    // 参考 FEEL 文档：substringAfter("foobar", "ob") -> "ar"
    assertThat(eval("substringAfter(\"foobar\", \"ob\")")).isEqualTo("ar");
  }

  @Test
  void matchAtStartReturnsRest() {
    // "foobar" 去掉前缀 "foo" -> "bar"
    assertThat(eval("substringAfter(\"foobar\", \"foo\")")).isEqualTo("bar");
  }

  @Test
  void noMatchReturnsEntireString() {
    // BUG MARKER：FEEL 规范期望返回原串 "foobar"，实际实现返回 "obar"
    assertThat(eval("substringAfter(\"foobar\", \"xyz\")")).isEqualTo("foobar");
  }
}
