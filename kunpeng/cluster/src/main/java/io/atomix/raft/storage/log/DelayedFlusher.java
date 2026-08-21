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
package io.atomix.raft.storage.log;

import io.atomix.raft.journal.CheckedJournalException;
import io.atomix.raft.journal.Journal;
import io.atomix.raft.journal.JournalException;
import io.atomix.utils.concurrent.Scheduled;
import io.atomix.utils.concurrent.Scheduler;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合并刷盘信号的 {@link RaftLogFlusher}：首个请求会按配置的延迟调度一次 fsync，
 * 在它真正执行之前，后续请求全部被吸收。只要发生过写入，日志至少每个延迟周期落盘一次。
 *
 * <p>注意：按契约单线程使用，必须与写日志的线程（通常是 Raft 线程）相同。
 */
public final class DelayedFlusher implements RaftLogFlusher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelayedFlusher.class);

  private final Scheduler scheduler;
  private final Duration flushDelay;

  /** 保护 pendingFlush 与 closed 状态的监视器。 */
  private final Object pendingMonitor = new Object();

  private Scheduled pendingFlush;
  private boolean closed;

  public DelayedFlusher(final Scheduler scheduler, final Duration flushDelay) {
    this.scheduler = Objects.requireNonNull(scheduler, "must specify a scheduler");
    this.flushDelay = Objects.requireNonNull(flushDelay, "must specify a valid flush delay");
  }

  @Override
  public void flush(final Journal journal) {
    synchronized (pendingMonitor) {
      if (closed) {
        LOGGER.debug("Flusher already closed, ignoring flush request");
        return;
      }

      if (pendingFlush != null) {
        // 已有待执行的延迟刷盘，它会一并覆盖本次请求
        LOGGER.trace("A delayed flush is already pending, coalescing this request");
        return;
      }

      LOGGER.trace("Delaying journal flush by {}, journal ends at index {}", flushDelay,
          journal.getLastIndex());
      pendingFlush = scheduler.schedule(flushDelay, () -> runFlush(journal));
    }
  }

  @Override
  public void close() {
    synchronized (pendingMonitor) {
      closed = true;
      if (pendingFlush != null) {
        pendingFlush.cancel();
        pendingFlush = null;
      }
    }

    scheduler.close();
  }

  /** 由调度器在延迟到期后回调：先清空待刷标记，再执行真正的 fsync，失败则重新调度。 */
  private void runFlush(final Journal journal) {
    synchronized (pendingMonitor) {
      pendingFlush = null;
    }

    LOGGER.trace("Flushing journal after {}", flushDelay);
    try {
      journal.flush();
    } catch (final CheckedJournalException | JournalException | UncheckedIOException failure) {
      LOGGER.warn("Journal flush failed, will retry after {}", flushDelay, failure);
      flush(journal);
    }
  }

  @Override
  public String toString() {
    return "DelayedFlusher{scheduler=" + scheduler + ", flushDelay=" + flushDelay
        + ", pendingFlush=" + pendingFlush + '}';
  }
}
