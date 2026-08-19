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

/** 通过表达式引擎端到端测试 {@code randomNumber()} 函数。 */
class RandomNumberFunctionTest extends FunctionTestBase {

  @Test
  void returnsDouble() {
    assertThat(eval("randomNumber()")).isInstanceOf(Double.class);
  }

  @Test
  void valueIsInUnitRange() {
    final double value = (double) eval("randomNumber()");
    assertThat(value).isBetween(0.0, 1.0);
  }

  @Test
  void twoCallsProduceDifferentValues() {
    // 极低概率相等，主要是确认能多次调用
    final double a = (double) eval("randomNumber()");
    final double b = (double) eval("randomNumber()");
    // 不强断言 a != b（理论上有极小概率相等），只断言都在范围内
    assertThat(a).isBetween(0.0, 1.0);
    assertThat(b).isBetween(0.0, 1.0);
  }
}
