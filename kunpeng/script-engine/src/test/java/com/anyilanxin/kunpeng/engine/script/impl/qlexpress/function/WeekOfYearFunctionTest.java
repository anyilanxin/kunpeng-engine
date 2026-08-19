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

/** 通过表达式引擎端到端测试 {@code weekOfYear()} 函数。 */
class WeekOfYearFunctionTest extends FunctionTestBase {

  @Test
  void weekOfYearForFirstWeek() {
    // 2026-01-01 是周四，属于 2026 年第 1 周
    assertThat(eval("weekOfYear(date(\"2026-01-01\", \"yyyy-MM-dd\"))")).isEqualTo(1L);
  }

  @Test
  void weekOfYearForMidYear() {
    // 2026-06-15 是周一，属于第 25 周
    assertThat(eval("weekOfYear(date(\"2026-06-15\", \"yyyy-MM-dd\"))")).isEqualTo(25L);
  }
}
