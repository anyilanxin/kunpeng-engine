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

/** 通过表达式引擎端到端测试 {@code count()} 函数。 */
class CountFunctionTest extends FunctionTestBase {

  @Test
  void countsNumberOfElements() {
    assertThat(eval("count([1, 2, 3])")).isEqualTo(3L);
  }

  @Test
  void emptyListReturnsZero() {
    assertThat(eval("count([])")).isEqualTo(0L);
  }

  @Test
  void singleElementListReturnsOne() {
    assertThat(eval("count([42])")).isEqualTo(1L);
  }

  @Test
  void listOfStringsIsCounted() {
    assertThat(eval("count([\"a\", \"b\", \"c\", \"d\"])")).isEqualTo(4L);
  }
}
