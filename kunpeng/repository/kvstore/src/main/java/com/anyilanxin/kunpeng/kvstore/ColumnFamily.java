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
package com.anyilanxin.kunpeng.kvstore;

import com.anyilanxin.kunpeng.kvstore.types.KeyType;
import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 列族访问接口，提供对指定 column family 中 key-value 对的写入、读取、遍历与删除等操作
 *
 * @author zxuanhong
 */
public interface ColumnFamily<Key extends KeyType, Value extends ValueType> {
  /**
   * 将 key-value 对写入当前列族。
   *
   * @param key 键
   * @param value 值
   */
  void put(Key key, Value value);

  /**
   * 获取列族中指定 key 对应的存储值。
   *
   * @param key 键
   * @return 若在列族中找到该 key 则返回对应值，否则返回 null
   */
  Value get(Key key);

  /**
   * 遍历列族中存储的值，顺序由 key 决定。
   *
   * <p>给定的 consumer 会依次接收每个值。注意：给定的 ValueType 只是对存储值的包装，反映的是当前迭代步骤， 迭代过程中其内部值会被复用修改，因此不应将该
   * ValueType 保存下来。
   *
   * @param consumer 接收值的 consumer
   */
  void forEach(Consumer<Value> consumer);

  /**
   * 遍历列族中存储的 key-value 对，顺序由 key 决定。
   *
   * <p>类似 {@link #forEach(BiConsumer)}。
   *
   * @param consumer 接收 key-value 对的 consumer
   */
  void forEach(BiConsumer<Key, Value> consumer);

  /**
   * 遍历列族中存储的 key-value 对，顺序由 key 决定。visitor 可通过返回值控制迭代是否继续， 即 visitor 返回 false 时迭代将停止。
   *
   * <p>类似 {@link #forEach(BiConsumer)}。
   *
   * @param visitor 访问 key-value 对的 visitor
   */
  void whileTrue(KeyValuePairVisitor<Key, Value> visitor);

  /**
   * 从指定 key 开始遍历列族中存储的 key-value 对，顺序由 key 决定。visitor 可通过返回值控制迭代是否 继续，即 visitor 返回 false 时迭代将停止。
   *
   * <p>类似 {@link #forEach(BiConsumer)}。
   *
   * @param visitor 访问 key-value 对的 visitor
   */
  void whileTrue(Key startAtKey, KeyValuePairVisitor<Key, Value> visitor);

  /**
   * 遍历列族中具有相同公共前缀的 key-value 对，顺序由 key 决定。
   *
   * <p>类似 {@link #forEach(BiConsumer)}。
   *
   * @param keyPrefix key 的公共前缀
   * @param visitor 访问 key-value 对的 visitor
   */
  void whileEqualPrefix(KeyType keyPrefix, BiConsumer<Key, Value> visitor);

  /**
   * 遍历列族中具有相同公共前缀的 key-value 对，顺序由 key 决定。visitor 可通过返回值控制迭代是否继续， 即 visitor 返回 false 时迭代将停止。
   *
   * <p>类似 {@link #whileEqualPrefix(KeyType, BiConsumer)} 与 {@link #whileTrue(KeyValuePairVisitor)}。
   *
   * @param keyPrefix key 的公共前缀
   * @param visitor 访问 key-value 对的 visitor
   */
  void whileEqualPrefix(KeyType keyPrefix, KeyValuePairVisitor<Key, Value> visitor);

  /**
   * 遍历列族中具有相同公共前缀的 key-value 对，顺序由 key 决定。visitor 可通过返回值控制迭代是否继续， 即 visitor 返回 false 时迭代将停止。
   *
   * <p>给定的 {@code startAtKey} 指示迭代的起始位置：若该 key 存在，则第一个 key-value 对的 key 即等于 {@code
   * startAtKey}；若不存在，则从其后的 key 开始。
   *
   * <p>类似 {@link #whileEqualPrefix(KeyType, BiConsumer)} 与 {@link #whileTrue(KeyValuePairVisitor)}。
   *
   * @param keyPrefix key 的公共前缀
   * @param startAtKey 迭代起始的 key
   * @param visitor 访问 key-value 对的 visitor
   */
  void whileEqualPrefix(KeyType keyPrefix, Key startAtKey, KeyValuePairVisitor<Key, Value> visitor);

  /**
   * 从列族中删除指定 key 对应的 key-value 对。
   *
   * @param key 标识该 key-value 对的 key
   */
  void delete(Key key);

  /**
   * 检查 key 是否存在于当前列族中。
   *
   * @param key 待查找的 key
   * @return 若 key 存在于当前列族则返回 true，否则返回 false
   */
  boolean exists(Key key);

  /**
   * 检查列族中是否没有任何条目。
   *
   * @return 若列族没有任何条目则返回 <code>true</code>
   */
  boolean isEmpty();

  /**
   * 通过迭代列族中的所有条目来统计条目数量。该操作代价较高，应谨慎使用。
   *
   * @return 列族中的条目数量
   */
  long count();

  /**
   * 通过迭代列族中的所有条目，统计具有相同公共前缀的条目数量。该操作代价较高，应谨慎使用。
   *
   * @param prefix key 的公共前缀
   * @return 列族中具有该公共前缀的条目数量
   */
  long countEqualPrefix(KeyType prefix);
}
