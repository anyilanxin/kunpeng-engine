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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

/** 测试用快照标识 */
public record SnapshotId(long index, long term) {

  @Override
  public String toString() {
    return index + "-" + term;
  }

  public static SnapshotId parse(final String value) {
    final String[] parts = value.split("-", 2);
    return new SnapshotId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
  }
}
