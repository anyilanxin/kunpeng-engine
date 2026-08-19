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

/**
 * 通过表达式引擎端到端测试 {@code decimal()} 函数（HALF_EVEN / 银行家舍入）。
 *
 * <p>注意：{@code decimal} 使用 {@link java.math.RoundingMode#HALF_EVEN}，与 {@code roundHalfUp}
 * 的 {@link java.math.RoundingMode#HALF_UP} 不同，是区分两者的关键测试点。
 */
class DecimalFunctionTest extends FunctionTestBase {

  @Test
  void roundsToGivenScale() {
    // 1/3 保留 2 位 -> 0.33
    assertThat(eval("decimal(1/3, 2)")).isEqualTo(0.33);
  }

  @Test
  void halfEvenRoundsToEven() {
    // 2.5 保留 0 位：HALF_EVEN -> 2（向偶数靠）
    assertThat(eval("decimal(2.5, 0)")).isEqualTo(2.0);
  }

  @Test
  void halfEvenRoundsOddUp() {
    // 1.5 保留 0 位：HALF_EVEN -> 2
    assertThat(eval("decimal(1.5, 0)")).isEqualTo(2.0);
  }

  @Test
  void roundsToTwoDecimalPlaces() {
    // 3.14159 保留 2 位 -> 3.14
    assertThat(eval("decimal(3.14159, 2)")).isEqualTo(3.14);
  }
}
