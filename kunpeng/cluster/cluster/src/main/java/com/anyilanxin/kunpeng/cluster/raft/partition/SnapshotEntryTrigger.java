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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition;

/**
 * 快照条目数触发器（kafka SnapshotGenerator 双阈值同源）：commit index 相对上次触发水位推进
 * 达到阈值时触发一次并就地重新武装。commit 通知可能按批合并（一次通知跨多个 index），按水位差值
 * 判定因此天然兼容。仅 raft 线程访问，无需同步。
 */
final class SnapshotEntryTrigger {

  private final long threshold;
  private long lastFiredIndex;

  SnapshotEntryTrigger(final long threshold, final long seedIndex) {
    this.threshold = threshold;
    this.lastFiredIndex = seedIndex;
  }

  /** @return commitIndex 相对水位推进达到阈值时返回 true，并把水位推进到 commitIndex。 */
  boolean tryFire(final long commitIndex) {
    if (threshold <= 0 || commitIndex - lastFiredIndex < threshold) {
      return false;
    }
    lastFiredIndex = commitIndex;
    return true;
  }
}
