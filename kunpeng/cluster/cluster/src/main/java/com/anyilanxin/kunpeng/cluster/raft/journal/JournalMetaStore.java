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
package com.anyilanxin.kunpeng.cluster.raft.journal;

/**
 * “最后已刷盘索引”的持久化通道。
 *
 * <p>该索引独立于日志本体存放（实现可落库、落文件或仅驻内存）。重启后 journal 以它为完整性 基准：磁盘上不允许出现高于它的记录，出现即判定损坏。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JournalMetaStore {

  /**
   * 取回最后已刷盘索引。
   *
   * <p>实现可能触发数据库或文件 IO，属高代价操作，调用方应缓存结果、按需读取。
   *
   * @return 最后已刷盘索引；从未记录过时返回实现约定的占位值（先用 {@link #hasLastFlushedIndex()} 判定再取值）
   */
  long loadLastFlushedIndex();

  /**
   * 覆写最后已刷盘索引。同样可能是高代价的阻塞操作。
   *
   * @param index 新的最后已刷盘索引
   */
  void storeLastFlushedIndex(long index);

  /**
   * 清除已记录的最后已刷盘索引，使 {@link #hasLastFlushedIndex()} 回到 false。删除全部日志 前必须先执行本操作，保证中途崩溃后重启也能识别“空日志”状态。
   */
  void resetLastFlushedIndex();

  /**
   * @return 是否已记录过最后已刷盘索引
   */
  boolean hasLastFlushedIndex();

  /** volatile 单变量实现：全部操作 O(1)，用于测试与内存型场景。 */
  class InMemory implements JournalMetaStore {

    private static final long UNSET = -1L;

    private volatile long lastFlushed = UNSET;

    @Override
    public long loadLastFlushedIndex() {
      return lastFlushed;
    }

    @Override
    public void storeLastFlushedIndex(final long index) {
      lastFlushed = index;
    }

    @Override
    public void resetLastFlushedIndex() {
      lastFlushed = UNSET;
    }

    @Override
    public boolean hasLastFlushedIndex() {
      return lastFlushed != UNSET;
    }
  }
}
