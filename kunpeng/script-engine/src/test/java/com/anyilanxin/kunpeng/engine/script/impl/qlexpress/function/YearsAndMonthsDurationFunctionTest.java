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

import java.time.Period;
import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code yearsAndMonthsDuration()} 函数。 */
class YearsAndMonthsDurationFunctionTest extends FunctionTestBase {

  @Test
  void yearsAndMonthsDuration() {
    // 参考 FEEL 文档：yearsAndMonthsDuration(date("2011-12-22"), date("2013-08-24")) -> P1Y8M
    assertThat(eval("yearsAndMonthsDuration(date(\"2011-12-22\", \"yyyy-MM-dd\"), date(\"2013-08-24\", \"yyyy-MM-dd\"))"))
        .isEqualTo(Period.parse("P1Y8M"));
  }

  @Test
  void negativeDuration() {
    assertThat(eval("yearsAndMonthsDuration(date(\"2013-08-24\", \"yyyy-MM-dd\"), date(\"2011-12-22\", \"yyyy-MM-dd\"))"))
        .isEqualTo(Period.parse("-P1Y8M"));
  }
}
