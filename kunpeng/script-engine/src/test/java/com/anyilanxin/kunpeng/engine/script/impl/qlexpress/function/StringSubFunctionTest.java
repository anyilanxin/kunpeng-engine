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
 * 通过表达式引擎端到端测试 {@code substring()} 函数。
 *
 * <p><b>BUG MARKER</b>：FEEL 规范定义 {@code substring(s, pos)} 的位置从 <b>1</b> 开始
 * （例：{@code substring("foobar", 3) -> "obar"}），但当前实现 {@link StringSubFunction}
 * 直接调用 {@code StringUtils.substring(s, pos)}，使用 <b>0</b> 基索引
 * （返回 {@code "bar"}）。下面的断言按 FEEL 规范写，预期 fail，待实现修复后转绿。
 */
class StringSubFunctionTest extends FunctionTestBase {

  @Test
  void substringFromPositivePosition() {
    // 参考 FEEL 文档：substring("foobar", 3) -> "obar"（位置从 1 开始）
    assertThat(eval("substring(\"foobar\", 3)")).isEqualTo("obar");
  }

  @Test
  void substringWithLength() {
    // 参考 FEEL 文档：substring("foobar", 3, 3) -> "oba"
    assertThat(eval("substring(\"foobar\", 3, 3)")).isEqualTo("oba");
  }

  @Test
  void substringFromNegativePosition() {
    // 参考 FEEL 文档：substring("foobar", -2) -> "ar"
    assertThat(eval("substring(\"foobar\", -2)")).isEqualTo("ar");
  }

  @Test
  void substringLengthExceedsRemaining() {
    // 参考 FEEL 文档：substring("foobar", 3, 10) -> "obar"（超出则到末尾）
    assertThat(eval("substring(\"foobar\", 3, 10)")).isEqualTo("obar");
  }
}
