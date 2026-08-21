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
package io.atomix.utils.collection;

import java.util.Map;

/**
 * A two-keyed map, i.e. a "table" of rows and columns where each cell holds a value.
 *
 * @param <R> the row key type
 * @param <C> the column key type
 * @param <V> the cell value type
 */
public class Table<R, C, V> {

  private final Map<R, Map<C, V>> rows;

  private Table(final Map<R, Map<C, V>> rows) {
    this.rows = rows;
  }

  /**
   * Returns a new table backed by concurrent maps, safe for concurrent access.
   *
   * @return a new concurrent table
   */
  public static <R, C, V> Table<R, C, V> concurrent() {
    return new Table<>(new java.util.concurrent.ConcurrentHashMap<>());
  }

  /**
   * Returns a new table backed by plain hash maps.
   *
   * @return a new table
   */
  public static <R, C, V> Table<R, C, V> hash() {
    return new Table<>(new java.util.HashMap<>());
  }

  /**
   * Returns the value at the given row and column, or {@code null} if absent.
   *
   * @param row the row key
   * @param column the column key
   * @return the cell value or {@code null}
   */
  public V get(final R row, final C column) {
    final var columns = rows.get(row);
    return columns == null ? null : columns.get(column);
  }

  /**
   * Puts a value into the given cell, returning the previous value (or {@code null}).
   *
   * @param row the row key
   * @param column the column key
   * @param value the new cell value
   * @return the previous value, or {@code null}
   */
  public V put(final R row, final C column, final V value) {
    return row(row).put(column, value);
  }

  /**
   * Computes the cell value if absent using the given mapping function.
   *
   * @param row the row key
   * @param column the column key
   * @param mappingFunction the function producing the value when absent
   * @return the current (existing or computed) cell value
   */
  public V computeIfAbsent(
      final R row, final C column, final BiMapping<R, C, V> mappingFunction) {
    return row(row).computeIfAbsent(column, c -> mappingFunction.apply(row, c));
  }

  /** Removes the value at the given cell. */
  public V remove(final R row, final C column) {
    final var columns = rows.get(row);
    return columns == null ? null : columns.remove(column);
  }

  /** Returns (creating if needed) the column map for the given row. */
  public Map<C, V> row(final R row) {
    return rows.computeIfAbsent(row, r -> newRowMap());
  }

  /** Returns a view of all rows. */
  public Map<R, Map<C, V>> asMap() {
    return rows;
  }

  /** Removes all entries. */
  public void clear() {
    rows.clear();
  }

  private Map<C, V> newRowMap() {
    return rows instanceof java.util.concurrent.ConcurrentHashMap
        ? new java.util.concurrent.ConcurrentHashMap<>()
        : new java.util.HashMap<>();
  }

  /** Mapping function receiving both the row and the column key. */
  @FunctionalInterface
  public interface BiMapping<R, C, V> {
    V apply(R row, C column);
  }
}
