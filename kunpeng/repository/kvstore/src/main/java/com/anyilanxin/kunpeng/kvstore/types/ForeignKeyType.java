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

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 包装来自指定 column family 的 key，序列化行为与内部 key 完全一致，用于描述跨列族的外键引用关系
 *
 * @param inner 被包装的内部 key
 * @param columnFamily 内部 key 所属的 column family
 * @param match 外键匹配类型
 * @param skip 跳过外键校验的判断条件
 * @param <K> 内部 key 的类型
 */
public record ForeignKeyType<K extends KeyType>(
    K inner, Enum<?> columnFamily, MatchType match, Predicate<K> skip)
    implements ContainsForeignKeys, KeyType, ValueType {

  /**
   * 以完整匹配方式包装指定 column family 的 key
   *
   * @param inner 被包装的内部 key
   * @param columnFamily 内部 key 所属的 column family
   */
  public ForeignKeyType(final K inner, final Enum<?> columnFamily) {
    this(inner, columnFamily, MatchType.Full);
  }

  /**
   * 包装指定 column family 的 key 并指定匹配类型
   *
   * @param inner 被包装的内部 key
   * @param columnFamily 内部 key 所属的 column family
   * @param match 外键匹配类型
   */
  public ForeignKeyType(final K inner, final Enum<?> columnFamily, final MatchType match) {
    this(inner, columnFamily, match, (k) -> false);
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    inner.wrap(buffer, offset, length);
  }

  @Override
  public int getLength() {
    return inner.getLength();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    inner.write(buffer, offset);
  }

  /**
   * 返回仅包含自身的外键集合
   *
   * @return 包含自身的外键集合
   */
  @Override
  public Collection<ForeignKeyType<KeyType>> containedForeignKeys() {
    return Collections.singletonList((ForeignKeyType<KeyType>) this);
  }

  /**
   * 判断是否应跳过对该外键的一致性校验
   *
   * @return 需要跳过校验时返回 true
   */
  public boolean shouldSkipCheck() {
    return skip.test(inner);
  }

  /** 外键匹配类型 */
  public enum MatchType {
    /** 完整匹配 */
    Full,
    /** 前缀匹配 */
    Prefix,
  }
}
