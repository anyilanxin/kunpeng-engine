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

/** 通过表达式引擎端到端测试 {@code metBy()} 函数。 */
class MetByFunctionTest extends FunctionTestBase {

  @Test
  void rangeMetByRange() {
    // 参考 FEEL 文档：metBy([5..10], [1..5]) -> true（a.start == b.end）
    assertThat(eval("metBy([5, 10], [1, 5])")).isEqualTo(true);
  }

  @Test
  void rangeNotMetByRange() {
    // 参考 FEEL 文档：metBy([3..4], [1..2]) -> false
    assertThat(eval("metBy([3, 4], [1, 2])")).isEqualTo(false);
  }

  @Test
  void pointMetByRange() {
    // 点 5 与区间 [1, 5] 终点相同
    assertThat(eval("metBy(5, [1, 5])")).isEqualTo(true);
  }
}
