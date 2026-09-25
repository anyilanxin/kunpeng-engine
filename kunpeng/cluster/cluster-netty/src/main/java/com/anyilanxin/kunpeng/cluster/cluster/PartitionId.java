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
package com.anyilanxin.kunpeng.cluster.cluster;

import java.util.Objects;

/** Identifies a partition within a partition group (physical tenant). */
public final class PartitionId {

  private final String group;
  private final int number;

  public PartitionId(final String group, final int number) {
    this.group = Objects.requireNonNull(group, "group must not be null");
    this.number = number;
  }

  public static PartitionId from(final String group, final int number) {
    return new PartitionId(group, number);
  }

  /**
   * 从 {@link #toString()} 格式（{@code group-number}）解析分区标识。
   *
   * @throws IllegalArgumentException 格式非法时抛出
   */
  public static PartitionId parse(final String value) {
    // 分组名可能含 '-'，按最后一个分隔符切分
    final int separator = value.lastIndexOf('-');
    if (separator <= 0 || separator == value.length() - 1) {
      throw new IllegalArgumentException("Invalid partition id: " + value);
    }
    return new PartitionId(
        value.substring(0, separator), Integer.parseInt(value.substring(separator + 1)));
  }

  /**
   * @return the partition group (physical tenant) name
   */
  public String group() {
    return group;
  }

  /**
   * @return the partition number
   */
  public int id() {
    return number;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final PartitionId that)) {
      return false;
    }
    return number == that.number && group.equals(that.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, number);
  }

  @Override
  public String toString() {
    return group + "-" + number;
  }
}
