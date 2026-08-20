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
package com.anyilanxin.kunpeng.eventlog.impl;

import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.LogFlowControl;
import com.anyilanxin.kunpeng.eventlog.RecordAvailableListener;
import com.anyilanxin.kunpeng.eventlog.impl.flow.FlowController;
import com.anyilanxin.kunpeng.eventlog.impl.reader.BatchEntryReaderImpl;
import com.anyilanxin.kunpeng.eventlog.impl.reader.EventLogReaderImpl;
import com.anyilanxin.kunpeng.eventlog.impl.sequencer.PositionSequencer;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.concurrent.CopyOnWriteArraySet;

/** {@link EventLog} 实现：恢复（seekToEnd 找回 lastPosition）→ 装配定序器/流控 → 提交通知分发。 */
public final class EventLogImpl implements EventLog {

  private final EventStore store;
  private final String logName;
  private final int partitionId;
  private final EventLogMetrics metrics;
  private final FlowController flowControl;
  private final PositionSequencer sequencer;
  private final CopyOnWriteArraySet<RecordAvailableListener> listeners =
      new CopyOnWriteArraySet<>();
  private final EventStore.CommitListener commitListener = this::onCommit;
  private volatile boolean closed;

  public EventLogImpl(
      final EventStore store,
      final String logName,
      final int partitionId,
      final int maxBatchSize,
      final Clock clock,
      final FlowControlParams params,
      final MeterRegistry registry,
      final int maxConcurrentAppends) {
    this.store = store;
    this.logName = logName;
    this.partitionId = partitionId;
    this.metrics =
        registry == null
            ? EventLogMetrics.noop()
            : new EventLogMetrics(registry, logName, partitionId);
    this.flowControl = new FlowController(params, System::nanoTime, metrics);
    final long lastPosition = recoverLastPosition(store);
    this.flowControl.seedPositions(lastPosition);
    this.sequencer =
        new PositionSequencer(
            store,
            flowControl,
            metrics,
            maxBatchSize,
            maxConcurrentAppends,
            lastPosition + 1,
            clock);
    store.addCommitListener(commitListener);
  }

  /** 打开期恢复：临时 reader 扫到末尾取 lastPosition（空日志 = 0） */
  private static long recoverLastPosition(final EventStore store) {
    try (EventLogReaderImpl recovery = new EventLogReaderImpl(store.newReader())) {
      return recovery.seekToEnd();
    }
  }

  @Override
  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public String getLogName() {
    return logName;
  }

  @Override
  public EventLogReader newReader() {
    checkOpen();
    return new EventLogReaderImpl(store.newReader());
  }

  @Override
  public BatchEntryReader newBatchReader() {
    checkOpen();
    return new BatchEntryReaderImpl(newReader());
  }

  @Override
  public EventLogWriter newWriter() {
    checkOpen();
    return sequencer;
  }

  @Override
  public LogFlowControl getFlowControl() {
    return flowControl;
  }

  @Override
  public long getLastCommittedPosition() {
    return flowControl.lastCommittedPosition();
  }

  @Override
  public void registerRecordAvailableListener(final RecordAvailableListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeRecordAvailableListener(final RecordAvailableListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      sequencer.close();
      store.removeCommitListener(commitListener);
      flowControl.releaseAll();
    }
  }

  /** 存储提交通知 → 消费方唤醒（回调轻量，重活由消费方自己线程拉取） */
  private void onCommit() {
    for (final RecordAvailableListener listener : listeners) {
      try {
        listener.onRecordAvailable();
      } catch (final RuntimeException e) {
        // 消费方回调异常不阻断其他监听者
      }
    }
  }

  private void checkOpen() {
    if (closed) {
      throw new IllegalStateException("event log 已关闭: " + logName);
    }
  }
}
