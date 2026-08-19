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

/** 通过表达式引擎端到端测试 {@code exp()} 函数。 */
class ExpFunctionTest extends FunctionTestBase {

  @Test
  void expOfZeroIsOne() {
    assertThat(eval("exp(0)")).isEqualTo(1.0);
  }

  @Test
  void expOfOneIsE() {
    assertThat((double) eval("exp(1)")).isCloseTo(Math.E, within(1e-9));
  }

  @Test
  void expOfFive() {
    // e^5 ≈ 148.4131591025766
    assertThat((double) eval("exp(5)")).isCloseTo(148.4131591025766, within(1e-9));
  }

  @Test
  void expOfNegativeValue() {
    // e^-1 ≈ 0.36787944117144233
    assertThat((double) eval("exp(-1)")).isCloseTo(0.36787944117144233, within(1e-9));
  }
}
