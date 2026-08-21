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
package com.anyilanxin.kunpeng.cluster.raft.utils;

import java.util.function.LongConsumer;
import org.slf4j.Logger;

/** 校验最新快照与 Raft 日志头部之间的索引衔接关系。 */
public final class StateUtil {

  private StateUtil() {}

  /**
   * 保证快照与日志合起来能覆盖所有索引。唯一可容忍的缺口是日志为空的情况——
   * 它意味着节点在提交快照与重置日志之间崩溃，此时把日志重置到 {@code snapshotIndex + 1}；
   * 其余情形均属状态不一致，直接抛出 {@link IllegalStateException}。
   *
   * @param logResetter 用于执行日志重置，入参为目标起始索引
   */
  public static void verifySnapshotLogConsistent(
      final long partitionId,
      final long snapshotIndex,
      final long firstIndex,
      final boolean isLogEmpty,
      final LongConsumer logResetter,
      final Logger log) {
    final boolean startsAtGenesis = firstIndex == 1;
    final boolean snapshotCoversLog = snapshotIndex > 0 && snapshotIndex + 1 >= firstIndex;
    if (startsAtGenesis || snapshotCoversLog) {
      return;
    }

    if (!isLogEmpty) {
      throw new IllegalStateException(
          String.format(
              "In partition %d expected to find a snapshot at index >= log's first index %d,"
                  + " but found snapshot %d. A previous snapshot is most likely corrupted.",
              partitionId, firstIndex, snapshotIndex));
    }

    log.info(
        "In partition {} current snapshot index ({}) is lower than log's first index {}, "
            + "but the log is empty. Most likely the node crashed while committing a snapshot "
            + "at index {}. Resetting log to {}",
        partitionId,
        snapshotIndex,
        firstIndex,
        firstIndex - 1,
        snapshotIndex + 1);
    logResetter.accept(snapshotIndex + 1);
  }
}
