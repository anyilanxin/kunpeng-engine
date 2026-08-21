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
package io.atomix.raft.journal;

/**
 * 日志元信息存取接口。
 *
 * <p>负责在日志体系之外持久化"最后已刷盘索引"（lastFlushedIndex），用于重启后的完整性
 * 判定：磁盘上不该存在超过该索引的记录，否则视为损坏。实现可能落在数据库或文件中。
 */
public interface JournalMetaStore {

  /**
   * 读取最后已刷盘索引。
   *
   * <p>实现可能涉及数据库或文件读取，代价较高且可能阻塞；调用方应自行缓存结果，仅在必要时
   * 读取。
   *
   * @return 最后已刷盘索引
   */
  long loadLastFlushedIndex();

  /**
   * 更新最后已刷盘索引。
   *
   * <p>实现可能涉及数据库或文件写入，代价较高且可能阻塞。
   *
   * @param index 最后已刷盘索引
   */
  void storeLastFlushedIndex(long index);

  /**
   * 将最后已刷盘索引重置为语义空值。
   *
   * <p>重置后具体取值由实现决定；判断是否已重置请使用 {@link #hasLastFlushedIndex()}。
   */
  void resetLastFlushedIndex();

  /** @return 若当前不存在已知的最后已刷盘索引则返回 true */
  boolean hasLastFlushedIndex();

  /** 基于内存 volatile 变量的默认实现，读写均为 O(1)。 */
  class InMemory implements JournalMetaStore {

    /** 语义空值：表示尚未记录任何已刷盘索引。 */
    private static final long NO_FLUSHED_INDEX = -1L;

    private volatile long flushedIndex = NO_FLUSHED_INDEX;

    @Override
    public long loadLastFlushedIndex() {
      return flushedIndex;
    }

    @Override
    public void storeLastFlushedIndex(final long index) {
      this.flushedIndex = index;
    }

    @Override
    public void resetLastFlushedIndex() {
      flushedIndex = NO_FLUSHED_INDEX;
    }

    @Override
    public boolean hasLastFlushedIndex() {
      return flushedIndex != NO_FLUSHED_INDEX;
    }
  }
}
