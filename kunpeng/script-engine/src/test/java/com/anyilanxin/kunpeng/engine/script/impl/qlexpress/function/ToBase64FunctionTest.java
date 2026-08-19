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

/** 通过表达式引擎端到端测试 {@code toBase64()} 函数。 */
class ToBase64FunctionTest extends FunctionTestBase {

  @Test
  void encodesAsciiString() {
    assertThat(eval("toBase64(\"hello\")")).isEqualTo("aGVsbG8=");
  }

  @Test
  void encodesUtf8String() {
    assertThat(eval("toBase64(\"你好，世界\")")).isEqualTo("5L2g5aW977yM5LiW55WM");
  }

  @Test
  void encodesEmptyString() {
    assertThat(eval("toBase64(\"\")")).isEqualTo("");
  }

  @Test
  void encodesNonStringByToString() {
    // 数字 42 通过 toString() 得到 "42"，再 base64 == "NDI="
    assertThat(eval("toBase64(42)")).isEqualTo("NDI=");
  }

  @Test
  void roundTripsWithFromBase64() {
    // fromBase64(toBase64("hello")) == "hello"
    assertThat(eval("fromBase64(toBase64(\"hello\"))")).isEqualTo("hello");
  }

  @Test
  void compositionWithAbs() {
    // toBase64(string(abs(-7))) == base64("7.0") == "Ny4w"
    assertThat(eval("toBase64(string(abs(-7)))")).isEqualTo("Ny4w");
  }
}
