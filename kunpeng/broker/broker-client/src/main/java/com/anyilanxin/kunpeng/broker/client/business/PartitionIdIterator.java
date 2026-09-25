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
package com.anyilanxin.kunpeng.broker.client.business;

/**
 * 分区 id 迭代器：从起始分区开始按 1..partitionCount 环形遍历，每轮每分区恰好一次
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class PartitionIdIterator {

  private final int partitionCount;
  private int currentPartitionId;
  private int visited;

  public PartitionIdIterator(final int startPartitionId, final int partitionCount) {
    this.currentPartitionId = Math.max(startPartitionId, 1);
    this.partitionCount = partitionCount;
  }

  public boolean hasNext() {
    return visited < partitionCount;
  }

  public int next() {
    if (!hasNext()) {
      throw new java.util.NoSuchElementException();
    }
    final int partitionId = currentPartitionId;
    currentPartitionId = currentPartitionId % partitionCount + 1;
    visited++;
    return partitionId;
  }

  public int getCurrentPartitionId() {
    return currentPartitionId;
  }
}
