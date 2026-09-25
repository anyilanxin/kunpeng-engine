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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Objects;
import java.util.function.Function;

/**
 * 基于 Caffeine 的 {@link SinkEntityCache} 实现。
 *
 * <p>条目只按最大容量淘汰；需要基于时间的过期策略时，可通过 {@link #CaffeineEntityCache(Caffeine, Function)} 传入自定义 Caffeine
 * 规格。 加载失败原样抛给调用方；加载器返回 {@code null} 只会得到 {@code null}，不会缓存任何内容。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存实体类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CaffeineEntityCache<K, V> implements SinkEntityCache<K, V> {

  private static final long DEFAULT_MAX_ENTRIES = 10_000L;

  private final Cache<K, V> cache;
  private final Function<K, V> loader;

  /**
   * 创建容量上限默认为 10000 条的缓存。
   *
   * @param loader 产出缺失实体的函数；可返回 {@code null}
   */
  public CaffeineEntityCache(final Function<K, V> loader) {
    this(DEFAULT_MAX_ENTRIES, loader);
  }

  /**
   * 创建按容量淘汰的缓存。
   *
   * @param maxEntries 触发淘汰前的最大条目数；必须为正
   * @param loader 产出缺失实体的函数；可返回 {@code null}
   */
  public CaffeineEntityCache(final long maxEntries, final Function<K, V> loader) {
    this(Caffeine.newBuilder().maximumSize(maxEntries), loader);
  }

  /**
   * 以完全自定义的 Caffeine 规格创建缓存。
   *
   * @param caffeine 描述淘汰策略的构建器
   * @param loader 产出缺失实体的函数；可返回 {@code null}
   */
  public CaffeineEntityCache(final Caffeine<Object, Object> caffeine, final Function<K, V> loader) {
    Objects.requireNonNull(caffeine, "caffeine spec must not be null");
    this.loader = Objects.requireNonNull(loader, "loader must not be null");
    this.cache = caffeine.build();
  }

  @Override
  public V get(final K key) {
    return cache.getIfPresent(key);
  }

  @Override
  public V getOrLoad(final K key) {
    return cache.get(key, loader);
  }

  @Override
  public void put(final K key, final V value) {
    cache.put(key, value);
  }

  @Override
  public void invalidate(final K key) {
    cache.invalidate(key);
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public long estimatedSize() {
    return cache.estimatedSize();
  }

  @Override
  public void close() {
    cache.invalidateAll();
    cache.cleanUp();
  }
}
