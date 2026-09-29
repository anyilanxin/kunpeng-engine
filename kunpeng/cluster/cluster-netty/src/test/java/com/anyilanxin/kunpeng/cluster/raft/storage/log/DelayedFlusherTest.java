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
package com.anyilanxin.kunpeng.cluster.raft.storage.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.raft.journal.CheckedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.Journal;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduled;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.agrona.CloseHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 延迟刷盘器：刷盘请求被合并成一次延迟调度，关闭时取消调度，失败时重新调度。 */
final class DelayedFlusherTest {

  // 换用不同的延迟参数，覆盖语义不变：仍验证“按配置的延迟间隔调度”
  private static final Duration FLUSH_DELAY = Duration.ofSeconds(3);

  private final RecordingScheduler scheduler = new RecordingScheduler();
  private final DelayedFlusher flusher = new DelayedFlusher(scheduler, FLUSH_DELAY);

  @AfterEach
  void tearDown() {
    CloseHelper.quietClose(flusher);
  }

  private static Journal openJournal() {
    final var journal = mock(Journal.class);
    when(journal.isOpen()).thenReturn(true);
    return journal;
  }

  @Test
  void flushIsScheduledAfterDelayAndNotExecutedEagerly() throws CheckedJournalException {
    final var journal = openJournal();

    flusher.flush(journal);

    assertThat(scheduler.tasks).hasSize(1);
    assertThat(scheduler.tasks.peek().delay).isEqualTo(FLUSH_DELAY);
    // 调度时刻不真正落盘
    verify(journal, never()).flush();
  }

  @Test
  void runningScheduledTaskFlushesTheJournal() throws CheckedJournalException {
    final var journal = openJournal();
    when(journal.getLastIndex()).thenReturn(9L);

    flusher.flush(journal);
    scheduler.runPending();

    verify(journal, times(1)).flush();
  }

  @Test
  void concurrentFlushRequestsCollapseIntoOneScheduling() throws CheckedJournalException {
    final var journal = openJournal();
    when(journal.getLastIndex()).thenReturn(9L);

    flusher.flush(journal);
    flusher.flush(journal);
    flusher.flush(journal);

    assertThat(scheduler.tasks).hasSize(1);
    assertThat(scheduler.tasks.peek().delay).isEqualTo(FLUSH_DELAY);
  }

  @Test
  void closingCancelsPendingFlushTask() {
    final var journal = mock(Journal.class);

    flusher.flush(journal);
    flusher.close();

    assertThat(scheduler.tasks.peek().cancelled).isTrue();
  }

  @Test
  void noSchedulingHappensAfterClose() throws CheckedJournalException {
    final var journal = openJournal();

    flusher.close();
    flusher.flush(journal);

    assertThat(scheduler.tasks).isEmpty();
  }

  @Test
  void failedFlushIsRetriedOnNextScheduledRun() throws CheckedJournalException {
    final var journal = openJournal();
    doThrow(new UncheckedIOException(new IOException("模拟内存分配失败")))
        .when(journal)
        .flush();

    flusher.flush(journal);
    scheduler.runPending();
    // 第一次刷盘失败后重新调度；恢复后第二次成功
    doNothing().when(journal).flush();
    scheduler.runPending();

    verify(journal, times(2)).flush();
  }

  @Test
  void failedFlushIsNotRetriedAfterClose() throws CheckedJournalException {
    final var journal = openJournal();
    doThrow(new UncheckedIOException(new IOException("模拟内存分配失败")))
        .when(journal)
        .flush();

    flusher.flush(journal);
    flusher.close();
    scheduler.runPending();

    assertThat(scheduler.tasks).isEmpty();
  }

  /** 记录式调度器：只支持一次性调度，把任务排队等待测试手动触发。 */
  private static final class RecordingScheduler implements Scheduler {
    private final Queue<TrackedTask> tasks = new ArrayDeque<>();

    @Override
    public Scheduled schedule(final long delay, final TimeUnit unit, final Runnable command) {
      final var task = new TrackedTask(Duration.of(delay, unit.toChronoUnit()), command);
      tasks.add(task);
      return task;
    }

    @Override
    public Scheduled schedule(
        final Duration initialDelay, final Duration interval, final Runnable command) {
      throw new UnsupportedOperationException("测试调度器不支持周期性调度");
    }

    void runPending() {
      tasks.poll().command.run();
    }
  }

  /** 可观察取消状态的一次性任务。 */
  private static final class TrackedTask implements Scheduled {
    private final Duration delay;
    private final Runnable command;
    private boolean cancelled;

    private TrackedTask(final Duration delay, final Runnable command) {
      this.delay = delay;
      this.command = command;
    }

    @Override
    public void cancel() {
      cancelled = true;
    }

    @Override
    public boolean isDone() {
      return cancelled;
    }
  }
}
