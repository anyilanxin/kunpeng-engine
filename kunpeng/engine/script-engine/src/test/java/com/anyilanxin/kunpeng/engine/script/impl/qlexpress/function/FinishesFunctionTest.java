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

/** 通过表达式引擎端到端测试 {@code finishes()} 函数。 */
class FinishesFunctionTest extends FunctionTestBase {

  @Test
  void rangeFinishesRange() {
    // 参考 FEEL 文档：finishes([3..5], [1..5]) -> true（终点相同，a 更晚开始）
    assertThat(eval("finishes([3, 5], [1, 5])")).isEqualTo(true);
  }

  @Test
  void pointFinishesRange() {
    // 参考 FEEL 文档：finishes(5, [1..5]) -> true
    assertThat(eval("finishes(5, [1, 5])")).isEqualTo(true);
  }

  @Test
  void differentEndReturnsFalse() {
    assertThat(eval("finishes([3, 6], [1, 5])")).isEqualTo(false);
  }
}
