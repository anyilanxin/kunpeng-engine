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

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 通过表达式引擎端到端测试 {@code date()} 函数。
 *
 * <p><b>BUG MARKER</b>：
 * <ul>
 *   <li>FEEL 规范中 {@code date("2026-01-01")} 应返回对应日期，但当前实现单参路径
 *       {@code format(value, null)} 在 format 为 null 时直接返回 null。</li>
 *   <li>FEEL 规范中 {@code date(year, month, day)} 是 3 参构造，但当前实现只有 0/1/2 参，
 *       {@code date(2026, 1, 1)} 被当作 (value, format) 处理，返回 null。</li>
 * </ul>
 * 下方断言按 FEEL 规范写，预期 fail。
 */
class DateFunctionTest extends FunctionTestBase {

  @Test
  void dateFromIsoString() {
    // FEEL 规范：date("2026-01-01") -> LocalDate.of(2026, 1, 1)
    assertThat(eval("date(\"2026-01-01\")")).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void dateWithExplicitFormat() {
    // date("01/01/2026", "dd/MM/yyyy") -> LocalDate.of(2026, 1, 1)
    assertThat(eval("date(\"01/01/2026\", \"dd/MM/yyyy\")")).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void dateFromComponents() {
    // date(2026, 1, 1) -> LocalDate.of(2026, 1, 1)
    assertThat(eval("date(2026, 1, 1)")).isEqualTo(LocalDate.of(2026, 1, 1));
  }
}
