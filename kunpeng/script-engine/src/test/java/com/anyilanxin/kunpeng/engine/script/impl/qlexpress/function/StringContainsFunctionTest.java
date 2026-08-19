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

/** 通过表达式引擎端到端测试 {@code contains()} 函数。 */
class StringContainsFunctionTest extends FunctionTestBase {

  @Test
  void containsMatch() {
    // 参考 FEEL 文档：contains("foobar", "obar") -> true
    assertThat(eval("contains(\"foobar\", \"obar\")")).isEqualTo(true);
  }

  @Test
  void doesNotContainNonSubstring() {
    // 参考 FEEL 文档：contains("foobar", "of") -> false
    assertThat(eval("contains(\"foobar\", \"of\")")).isEqualTo(false);
  }

  @Test
  void containsExactMatch() {
    assertThat(eval("contains(\"foo\", \"foo\")")).isEqualTo(true);
  }
}
