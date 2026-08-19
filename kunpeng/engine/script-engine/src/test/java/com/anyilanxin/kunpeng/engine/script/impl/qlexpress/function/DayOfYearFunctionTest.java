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

/** 通过表达式引擎端到端测试 {@code dayOfYear()} 函数。 */
class DayOfYearFunctionTest extends FunctionTestBase {

  @Test
  void dayOfYearForJanFirst() {
    // 2026-01-01 是第 1 天
    assertThat(eval("dayOfYear(date(\"2026-01-01\", \"yyyy-MM-dd\"))")).isEqualTo(1L);
  }

  @Test
  void dayOfYearForDec31() {
    // 2026-12-31 是第 365 天（2026 非闰年）
    assertThat(eval("dayOfYear(date(\"2026-12-31\", \"yyyy-MM-dd\"))")).isEqualTo(365L);
  }
}
