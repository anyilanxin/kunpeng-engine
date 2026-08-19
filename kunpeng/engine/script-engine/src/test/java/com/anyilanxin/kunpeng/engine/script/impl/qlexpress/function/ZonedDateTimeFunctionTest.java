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

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code zonedDateTime()} 函数。 */
class ZonedDateTimeFunctionTest extends FunctionTestBase {

  @Test
  void zonedDateTimeFromIsoString() {
    // zonedDateTime("2026-01-01T12:00:00+08:00") -> ZonedDateTime
    assertThat(eval("zonedDateTime(\"2026-01-01T12:00:00+08:00\")"))
        .isEqualTo(ZonedDateTime.parse("2026-01-01T12:00:00+08:00"));
  }

  @Test
  void zonedDateTimeWithExplicitFormat() {
    // zonedDateTime("01/01/2026 12:00 +0800", "dd/MM/yyyy HH:mm Z") -> ZonedDateTime
    assertThat(eval("zonedDateTime(\"01/01/2026 12:00 +0800\", \"dd/MM/yyyy HH:mm Z\")"))
        .isEqualTo(ZonedDateTime.parse("2026-01-01T12:00:00+08:00"));
  }
}
