/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.structpack.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** BufferUtil 测试 */
@DisplayName("BufferUtil buffer 工具")
class BufferUtilTest {

  @Test
  @DisplayName("wrapString: UTF-8 包装与 null 容错")
  void wrapString() {
    assertThat(BufferUtil.bufferAsString(BufferUtil.wrapString("鲲鹏"))).isEqualTo("鲲鹏");
    assertThat(BufferUtil.wrapString(null).capacity()).isZero();
    assertThat(BufferUtil.wrapString("").capacity()).isZero();
    assertThat(BufferUtil.bufferAsString(BufferUtil.wrapString(""))).isEmpty();
  }

  @Test
  @DisplayName("bufferAsArray: 拷贝独立于原 buffer")
  void bufferAsArrayCopy() {
    final org.agrona.DirectBuffer buffer = BufferUtil.wrapString("data");
    final byte[] copy = BufferUtil.bufferAsArray(buffer);
    assertThat(copy).hasSize(4);
    copy[0] = 'X';
    assertThat(buffer.getByte(0)).isEqualTo((byte) 'd');
  }
}
