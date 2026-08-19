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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过表达式引擎端到端测试 {@code lastDayOfMonth()} 函数。
 */
class LastDayOfMonthFunctionTest extends FunctionTestBase {

  @Test
  void lastDayOfJanuary() {
    // 2026-01-15 的月末是 2026-01-31
    assertThat(eval("lastDayOfMonth(date(\"2026-01-15\", \"yyyy-MM-dd\"))"))
      .isEqualTo(LocalDate.of(2026, 1, 31));
  }

  @Test
  void lastDayOfFebruaryInLeapYear() {
    // 2024-02-15 的月末是 2024-02-29
    assertThat(eval("lastDayOfMonth(dateTime(\"2012-12-25T11:00:00\"))"))
      .isEqualTo(LocalDate.of(2012, 12, 31));
  }
}
