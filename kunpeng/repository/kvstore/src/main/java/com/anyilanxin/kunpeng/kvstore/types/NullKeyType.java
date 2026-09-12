/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.kvstore.types;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** 仅由 #isEmpty 内部使用的空 key，用于按相同 column family 前缀进行查找 */
public final class NullKeyType implements KeyType {

  public static final NullKeyType INSTANCE = new NullKeyType();

  public NullKeyType() {}

  /** 空实现，不读取任何内容 */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    // 无需处理
  }

  /** 空实现，不写入任何内容 */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    // 无需处理
  }

  /** 返回 0，不占用任何字节 */
  @Override
  public int getLength() {
    return 0;
  }
}
