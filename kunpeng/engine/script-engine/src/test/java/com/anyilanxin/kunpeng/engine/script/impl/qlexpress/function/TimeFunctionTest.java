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

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * 通过表达式引擎端到端测试 {@code time()} 函数。
 *
 * <p><b>BUG MARKER</b>：与 {@code date()} 类似，当前实现 {@link TimeFunction} 单参路径
 * {@code format(value, null)} 在 format 为 null 时直接返回 null，因此 {@code time("12:00:00")}
 * 无法按 ISO 解析。下方断言按 FEEL 规范写，预期 fail。
 */
class TimeFunctionTest extends FunctionTestBase {

  @Test
  void timeFromIsoString() {
    // FEEL 规范：time("12:00:00") -> LocalTime.of(12, 0)
    assertThat(eval("time(\"12:00:00\")")).isEqualTo(LocalTime.of(12, 0));
  }

  @Test
  void timeWithExplicitFormat() {
    // time("12:00", "HH:mm") -> LocalTime.of(12, 0)
    assertThat(eval("time(\"12:00\", \"HH:mm\")")).isEqualTo(LocalTime.of(12, 0));
  }
}
