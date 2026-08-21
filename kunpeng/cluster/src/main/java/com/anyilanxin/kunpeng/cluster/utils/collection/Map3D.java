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
package com.anyilanxin.kunpeng.cluster.utils.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A three-keyed map, addressed by a first, second and third key.
 *
 * @param <K1> the first key type
 * @param <K2> the second key type
 * @param <K3> the third key type
 * @param <V> the value type
 */
public class Map3D<K1, K2, K3, V> {

  private final Map<K1, Map<K2, Map<K3, V>>> outer;

  private Map3D(final Map<K1, Map<K2, Map<K3, V>>> outer) {
    this.outer = outer;
  }

  /** Returns a new concurrent instance. */
  public static <K1, K2, K3, V> Map3D<K1, K2, K3, V> concurrent() {
    return new Map3D<>(new ConcurrentHashMap<>());
  }

  /** Returns the value addressed by the three keys, or {@code null} if absent. */
  public V get(final K1 first, final K2 second, final K3 third) {
    final var middle = outer.get(first);
    if (middle == null) {
      return null;
    }
    final var inner = middle.get(second);
    return inner == null ? null : inner.get(third);
  }

  /** Puts a value at the given address, returning the previous value (or {@code null}). */
  public V put(final K1 first, final K2 second, final K3 third, final V value) {
    return inner(first, second).put(third, value);
  }

  /** Computes the value at the given address if absent. */
  public V computeIfAbsent(
      final K1 first, final K2 second, final K3 third, final TriFunction<K1, K2, K3, V> fn) {
    return inner(first, second).computeIfAbsent(third, t -> fn.apply(first, second, t));
  }

  /** Removes the value at the given address. */
  public V remove(final K1 first, final K2 second, final K3 third) {
    final var inner = innerOrNull(first, second);
    return inner == null ? null : inner.remove(third);
  }

  /** Removes all entries under the given first key. */
  public void removeAll(final K1 first) {
    outer.remove(first);
  }

  /** Removes all entries. */
  public void clear() {
    outer.clear();
  }

  private Map<K3, V> inner(final K1 first, final K2 second) {
    return outer
        .computeIfAbsent(first, f -> new ConcurrentHashMap<>())
        .computeIfAbsent(second, s -> new ConcurrentHashMap<>());
  }

  private Map<K3, V> innerOrNull(final K1 first, final K2 second) {
    final var middle = outer.get(first);
    return middle == null ? null : middle.get(second);
  }

  /** Function receiving all three keys. */
  @FunctionalInterface
  public interface TriFunction<K1, K2, K3, V> {
    V apply(K1 first, K2 second, K3 third);
  }
}
