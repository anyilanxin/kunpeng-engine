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

/** 通过表达式引擎端到端测试 {@code mode()} 函数。 */
class ModeFunctionTest extends FunctionTestBase {

  @Test
  void clearMostFrequentValue() {
    // 6 出现 3 次，是最频繁的
    assertThat(eval("mode([6, 3, 9, 6, 6])")).isEqualTo(6);
  }

  @Test
  void allUniqueValuesPicksAny() {
    // 全部唯一，mode 返回其中之一（实现取迭代首个达到 max 计数的元素）
    assertThat(eval("mode([1, 2, 3])")).isIn(1, 2, 3);
  }

  @Test
  void modeOfStringValues() {
    // "a" 出现 2 次
    assertThat(eval("mode([\"a\", \"b\", \"a\"])")).isEqualTo("a");
  }

  @Test
  void singleElementListReturnsThatElement() {
    assertThat(eval("mode([42])")).isEqualTo(42);
  }
}
