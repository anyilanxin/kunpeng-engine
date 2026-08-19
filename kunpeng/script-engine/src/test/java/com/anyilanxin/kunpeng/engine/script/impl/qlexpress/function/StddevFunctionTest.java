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
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * 通过表达式引擎端到端测试 {@code stddev()} 函数。
 *
 * <p>注意：当前实现使用<b>总体标准差</b>（除以 n），与 FEEL 规范定义的<b>样本标准差</b>（除以 n-1）不同。
 * 例：{@code stddev([2, 4, 7, 5])} 总体标准差 ≈ 1.8027756，样本标准差 ≈ 2.0816659。此差异留作后续讨论。
 */
class StddevFunctionTest extends FunctionTestBase {

  @Test
  void stddevOfFourValues() {
    // 总体标准差：mean=4.5, var=((2-4.5)^2+(4-4.5)^2+(7-4.5)^2+(5-4.5)^2)/4=3.25, stddev=sqrt(3.25)
    assertThat((double) eval("stddev([2, 4, 7, 5])")).isCloseTo(1.8027756377319946, within(1e-9));
  }

  @Test
  void stddevOfIdenticalValuesIsZero() {
    assertThat(eval("stddev([5, 5, 5, 5])")).isEqualTo(0.0);
  }

  @Test
  void stddevOfSingleValueIsZero() {
    assertThat(eval("stddev([42])")).isEqualTo(0.0);
  }

  @Test
  void stddevOfTwoValues() {
    // 总体方差：mean=(2+4)/2=3, var=((2-3)^2+(4-3)^2)/2=1, stddev=1
    assertThat(eval("stddev([2, 4])")).isEqualTo(1.0);
  }
}
