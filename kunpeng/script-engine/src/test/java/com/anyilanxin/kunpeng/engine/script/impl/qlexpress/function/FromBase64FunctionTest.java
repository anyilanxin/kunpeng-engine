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

/** 通过表达式引擎端到端测试 {@code fromBase64()} 函数。 */
class FromBase64FunctionTest extends FunctionTestBase {

  @Test
  void decodesAsciiString() {
    // base64("hello") == "aGVsbG8="
    assertThat(eval("fromBase64(\"aGVsbG8=\")")).isEqualTo("hello");
  }

  @Test
  void decodesUtf8String() {
    // base64("你好，世界") == "5L2g5aW977yM5LiW55WM"
    assertThat(eval("fromBase64(\"5L2g5aW977yM5LiW55WM\")")).isEqualTo("你好，世界");
  }

  @Test
  void decodesEmptyString() {
    // base64("") == ""
    assertThat(eval("fromBase64(\"\")")).isEqualTo("");
  }

  @Test
  void decodesNumberLookingString() {
    // base64("NDI=") == "42"
    assertThat(eval("fromBase64(\"NDI=\")")).isEqualTo("42");
  }

  @Test
  void invalidBase64ReturnsNull() {
    // 非法 base64 字符串返回 null
    assertThat(eval("fromBase64(\"!!!not-base64!!!\")")).isNull();
  }

  @Test
  void roundTripsWithToBase64() {
    // fromBase64(toBase64("hello")) == "hello"
    assertThat(eval("fromBase64(toBase64(\"hello\"))")).isEqualTo("hello");
  }
}
