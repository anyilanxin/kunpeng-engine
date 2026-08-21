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
package io.atomix.raft.impl;

import io.atomix.raft.metrics.RaftServiceMetrics;
import io.atomix.raft.snapshot.PersistedSnapshotStore;
import io.atomix.raft.storage.log.RaftLog;
import io.atomix.utils.VisibleForTesting;
import io.atomix.utils.concurrent.ThreadContext;
import org.agrona.LangUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 快照允许丢弃旧日志条目后，对 Raft 日志执行截断。
 *
 * <p>按快照边界截断旧日志的思想参考自 Apache-2.0 的 SOFAJRaft LogManager（日志清理），
 * 典型时序：
 *
 * <pre>
 *  快照存储/业务              本类                        RaftLog(Journal)
 *      |                       |                               |
 *      |-- 新快照提交 --------->|                               |
 *      |   setCompactableIndex(快照最低索引)                    |
 *      |---------------- compact() / compactFromSnapshots() -->|
 *      |                       |-- trimTo(边界 - 追赶保留余量) ->|
 *      |                       |<-------- 截断完成(是否删除) ----|
 *      |                       |   （落后跟随者仍可复制追上，
 *      |                       |    无需触发完整快照传输）
 * </pre>
 */
public final class LogCompactor {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogCompactor.class);

  private final RaftLog raftLog;
  private final ThreadContext raftThread;
  private final RaftServiceMetrics metrics;

  /**
   * 在可截断边界之外额外保留的条目数，让落后的跟随者仍能靠条目复制追上，
   * 而不是被迫走完整的快照传输。
   */
  private final int followerCatchupRetention;

  /** 当前允许截断到的最新边界索引；可能被其他线程更新。 */
  private volatile long trimUpperBound;

  public LogCompactor(
      final ThreadContext raftThread,
      final RaftLog raftLog,
      final int followerCatchupRetention,
      final RaftServiceMetrics metrics) {
    this.raftThread = raftThread;
    this.raftLog = raftLog;
    this.followerCatchupRetention = followerCatchupRetention;
    this.metrics = metrics;
  }

  /**
   * 记录允许截断到的最高索引，随后的 {@link #compact()} 或
   * {@link #compactIgnoringReplicationThreshold()} 会删到该位置为止。
   *
   * <p>注意：可在任意线程调用。
   */
  @VisibleForTesting
  void setCompactableIndex(final long index) {
    LOGGER.trace("Updated compactable index to {}", index);
    trimUpperBound = index;
  }

  @VisibleForTesting
  public long getCompactableIndex() {
    return trimUpperBound;
  }

  /**
   * 截到最新可截断边界，但保留一个复制阈值宽度的条目，方便轻微落后的跟随者低成本追赶。
   * 由于快照是异步完成的、边界也随之推进，完全可能本次什么都没删
   * （例如上次运行后没有新快照）。
   *
   * @return 是否实际删除了数据
   */
  public boolean compact() {
    return trimTo(trimUpperBound - followerCatchupRetention);
  }

  /** 与 {@link #compact()} 相同，但不保留复制阈值、直接截到边界。 */
  public boolean compactIgnoringReplicationThreshold() {
    return trimTo(trimUpperBound);
  }

  /** 以快照存储的最低边界推导截断上界并执行截断。 */
  public void compactFromSnapshots(final PersistedSnapshotStore snapshotStore) {
    snapshotStore
        .getCompactionBound()
        .whenCompleteAsync(this::onCompactionBoundReady, raftThread);
  }

  /** 在 Raft 线程上把日志删到指定索引，并计入压缩耗时指标。 */
  private boolean trimTo(final long index) {
    raftThread.checkThread();

    try (final var ignored = metrics.compactionTime()) {
      final var trimmed = raftLog.deleteUntil(index);
      LOGGER.debug("Compacted log up to index {}", index);
      return trimmed;
    } catch (final Exception failure) {
      LOGGER.error("Failed to compact up to index {}", index, failure);
      LangUtil.rethrowUnchecked(failure);
      return false;
    }
  }

  private void onCompactionBoundReady(final Long index, final Throwable failure) {
    if (failure != null) {
      LOGGER.error(
          "Expected to compact logs, but could not the compaction bound from the snapshot store",
          failure);
      return;
    }

    LOGGER.debug("Scheduling log compaction up to index {}", index);
    setCompactableIndex(index);
    compact();
  }
}
