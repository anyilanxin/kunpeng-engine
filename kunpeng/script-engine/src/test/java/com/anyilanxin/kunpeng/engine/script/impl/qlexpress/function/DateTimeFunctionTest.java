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

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code dateTime()} 函数。 */
class DateTimeFunctionTest extends FunctionTestBase {

  @Test
  void dateTimeFromIsoString() {
    // dateTime("2026-01-01T12:00:00") -> LocalDateTime.of(2026, 1, 1, 12, 0)
    assertThat(eval("dateTime(\"2026-01-01T12:00:00\")"))
        .isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
  }

  @Test
  void dateTimeWithExplicitFormat() {
    // dateTime("01/01/2026 12:00", "dd/MM/yyyy HH:mm") -> LocalDateTime.of(2026, 1, 1, 12, 0)
    assertThat(eval("dateTime(\"01/01/2026 12:00\", \"dd/MM/yyyy HH:mm\")"))
        .isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
  }
}
