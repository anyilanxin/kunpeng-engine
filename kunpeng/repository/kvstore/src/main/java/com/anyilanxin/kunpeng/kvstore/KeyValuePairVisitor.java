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

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;

/**
 * 表示一个接收 key-value 对并返回 boolean 基本类型结果的函数。
 *
 * @param <Key> key 的类型
 * @param <Value> value 的类型
 */
@FunctionalInterface
public interface KeyValuePairVisitor<
    Key extends BufferReader & BufferWriter, Value extends BufferWriter & BufferReader> {

  /**
   * 访问 key-value 对。返回值表示是否继续访问后续的 key-value 对。
   *
   * @param key 键
   * @param value 值
   * @return 若继续访问则返回 true，否则返回 false
   */
  boolean visit(Key key, Value value);
}
