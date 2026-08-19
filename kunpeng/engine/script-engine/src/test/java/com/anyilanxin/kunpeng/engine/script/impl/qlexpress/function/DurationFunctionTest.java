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

import java.time.Duration;
import java.time.Period;
import org.junit.jupiter.api.Test;

/** 通过表达式引擎端到端测试 {@code duration()} 函数。 */
class DurationFunctionTest extends FunctionTestBase {

  @Test
  void parsesDaysDuration() {
    // FEEL 规范：duration("P5D") 是 days-and-time duration（Duration）
    // 但当前实现把 "P5D" 解析为 Period（P5D），不是 Duration（120H）
    assertThat(eval("duration(\"P5D\")")).isEqualTo(Duration.parse("P5D"));
  }

  @Test
  void parsesYearsDuration() {
    // 文本以 P 开头且无 T，解析为 Period
    assertThat(eval("duration(\"P1Y\")")).isEqualTo(Period.parse("P1Y"));
  }

  @Test
  void parsesTimeDuration() {
    // 文本包含 T，解析为 Duration
    assertThat(eval("duration(\"PT1H\")")).isEqualTo(Duration.parse("PT1H"));
  }

  @Test
  void invalidDurationReturnsNull() {
    assertThat(eval("duration(\"invalid\")")).isNull();
  }
}
