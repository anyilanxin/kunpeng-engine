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
package com.anyilanxin.kunpeng.sink.common.cache;

/**
 * Sink 转换记录时所需实体的本地有界缓存。
 *
 * <p>Sink 经常需要在记录中解析引用（流程定义 key、父实例 id……），而这些实体来自流中更早的记录。 每条记录都去目标系统查询显然太慢，因此 Sink
 * 会维护本地缓存。本接口把这个缓存抽象出来， 使具体实现（以及测试中的替身）可以独立于 Sink 逻辑替换。
 *
 * <p>实现应保证线程安全。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存实体类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SinkEntityCache<K, V> extends AutoCloseable {

  /**
   * 查询实体，不触发加载器。
   *
   * @param key 缓存键
   * @return 命中的实体；不存在时为 {@code null}
   */
  V get(K key);

  /**
   * 查询实体，未命中时通过配置的加载器加载。
   *
   * @param key 缓存键
   * @return 命中或新加载的实体；加载器对该键返回 {@code null} 时也可能是 {@code null}
   */
  V getOrLoad(K key);

  /**
   * 直接写入或替换一个条目。
   *
   * @param key 缓存键
   * @param value 待存储的实体
   */
  void put(K key, V value);

  /**
   * 丢弃单个条目，例如记录表明该实体已被删除时。
   *
   * @param key 缓存键
   */
  void invalidate(K key);

  /** 丢弃全部条目；在底层数据源已知发生整体变化时使用。 */
  void clear();

  /**
   * @return 缓存条目的近似数量
   */
  long estimatedSize();

  /** 释放缓存资源；之后不得再使用该缓存。 */
  @Override
  default void close() {}
}
