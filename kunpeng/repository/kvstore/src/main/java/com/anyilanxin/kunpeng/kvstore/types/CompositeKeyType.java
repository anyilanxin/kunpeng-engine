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

/**
 * 由两个 key 顺序拼接而成的组合 key，序列化时先写第一个 key，再写第二个 key
 *
 * @param <FirstKeyType> 第一个 key 的类型
 * @param <SecondKeyType> 第二个 key 的类型
 */
public class CompositeKeyType<FirstKeyType extends KeyType, SecondKeyType extends KeyType>
    implements KeyType {

  private final FirstKeyType firstKeyTypePart;
  private final SecondKeyType secondKeyTypePart;

  /**
   * 以两个 key 组成组合 key
   *
   * @param firstKeyTypePart 第一个 key
   * @param secondKeyTypePart 第二个 key
   */
  public CompositeKeyType(
      final FirstKeyType firstKeyTypePart, final SecondKeyType secondKeyTypePart) {
    this.firstKeyTypePart = firstKeyTypePart;
    this.secondKeyTypePart = secondKeyTypePart;
  }

  /** 返回第一个 key */
  public FirstKeyType getFirst() {
    return firstKeyTypePart;
  }

  /** 返回第二个 key */
  public SecondKeyType getSecond() {
    return secondKeyTypePart;
  }

  /** 先读取第一个 key，再从其后偏移读取第二个 key */
  @Override
  public void wrap(final DirectBuffer directBuffer, final int offset, final int length) {
    firstKeyTypePart.wrap(directBuffer, offset, length);
    final int firstKeyLength = firstKeyTypePart.getLength();
    secondKeyTypePart.wrap(directBuffer, offset + firstKeyLength, length - firstKeyLength);
  }

  /** 返回两个 key 的序列化长度之和 */
  @Override
  public int getLength() {
    return firstKeyTypePart.getLength() + secondKeyTypePart.getLength();
  }

  /** 先写入第一个 key，随后紧跟着写入第二个 key */
  @Override
  public void write(final MutableDirectBuffer mutableDirectBuffer, final int offset) {
    firstKeyTypePart.write(mutableDirectBuffer, offset);
    final int firstKeyPartLength = firstKeyTypePart.getLength();
    secondKeyTypePart.write(mutableDirectBuffer, offset + firstKeyPartLength);
  }
}
