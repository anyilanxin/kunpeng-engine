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
package com.anyilanxin.kunpeng.eventlog.impl.sequencer;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.AppendResult.Appended;
import com.anyilanxin.kunpeng.eventlog.AppendResult.Rejected;
import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import com.anyilanxin.kunpeng.eventlog.impl.flow.FlowController;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrame;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.agrona.ExpandableArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 无锁乐观定序器（{@link EventLogWriter} 实现）。
 *
 * <p>与"单锁串行"方案的差异：position 由 {@link AtomicLong#getAndAdd} 乐观预约（无临界区）， 提交顺序由 watermark 提交链保证（{@code
 * synchronized} 仅覆盖已就绪槽位的逐个提交， 不含 position 分配与帧序列化）。失败区间<b>烧毁</b>——position 只进不退，读者容忍 gap。
 *
 * <p>不变量：
 *
 * <ul>
 *   <li>position 全局唯一严格递增；批内连续（first..first+n-1）
 *   <li>{@code EventStore.append} 调用序 == firstPosition 升序
 *   <li>帧缓冲为每批独立自持（Raft 延迟序列化持有引用，不可复用线程本地缓冲）
 *   <li>单写者常态：领取槽位 → CAS 预约 → 序列化（无锁区）→ 提交（无竞争监视器）
 * </ul>
 */
public final class PositionSequencer implements EventLogWriter {

  private static final Logger LOG = LoggerFactory.getLogger(PositionSequencer.class);
  private static final long RESERVE_TIMEOUT_NANOS = 5_000_000_000L; // 看门狗烧毁阈值

  private final EventStore store;
  private final FlowController flowControl;
  private final EventLogMetrics metrics;
  private final int maxBatchSize;
  private final Clock wallClock;
  private final PendingAppendQueue pending;

  private final AtomicLong nextPosition;
  private volatile long submittedThrough; // 已提交(含烧毁)的最大 lastPosition
  private volatile boolean closed;
  private long reserveTimeoutNanos = RESERVE_TIMEOUT_NANOS;

  public PositionSequencer(
      final EventStore store,
      final FlowController flowControl,
      final EventLogMetrics metrics,
      final int maxBatchSize,
      final int maxConcurrentAppends,
      final long initialPosition,
      final Clock wallClock) {
    this.store = store;
    this.flowControl = flowControl;
    this.metrics = metrics;
    this.maxBatchSize = maxBatchSize;
    this.wallClock = wallClock;
    this.pending = new PendingAppendQueue(maxConcurrentAppends);
    this.nextPosition = new AtomicLong(initialPosition);
    this.submittedThrough = initialPosition - 1;
  }

  @Override
  public AppendResult tryAppend(final WriteContext context, final AppendEntry entry) {
    return tryAppend(context, List.of(entry), -1);
  }

  @Override
  public AppendResult tryAppend(final WriteContext context, final List<AppendEntry> entries) {
    return tryAppend(context, entries, -1);
  }

  @Override
  public AppendResult tryAppend(
      final WriteContext context, final List<AppendEntry> entries, final long sourcePosition) {
    if (closed) {
      metrics.incRejected(context, RejectionReason.CLOSED);
      return new Rejected(RejectionReason.CLOSED);
    }
    if (entries == null || entries.isEmpty()) {
      metrics.incRejectedInvalid();
      metrics.incRejected(context, RejectionReason.INVALID_ARGUMENT);
      return new Rejected(RejectionReason.INVALID_ARGUMENT);
    }
    final int slot = pending.tryClaim();
    if (slot < 0) {
      metrics.incRejectedWindow();
      metrics.incRejected(context, RejectionReason.REQUEST_WINDOW_EXHAUSTED);
      return new Rejected(RejectionReason.REQUEST_WINDOW_EXHAUSTED);
    }
    final RejectionReason reason = flowControl.tryAcquire(context, entries.size());
    if (reason != null) {
      pending.releaseClaim(slot);
      metrics.incRejected(context, reason);
      return new Rejected(reason);
    }

    final int count = entries.size();
    final long first = nextPosition.getAndAdd(count);
    final long last = first + count - 1;
    flowControl.onAppend(first, last, count);
    pending.reserve(slot, first, last, System.nanoTime() + reserveTimeoutNanos);

    try {
      final long timestamp = wallClock.millis();
      final int frameLength = BatchFrame.calculateLength(first, sourcePosition, timestamp, entries);
      if (frameLength > maxBatchSize) {
        burn(first, last);
        metrics.incRejectedInvalid();
        metrics.incRejected(context, RejectionReason.INVALID_ARGUMENT);
        return new Rejected(RejectionReason.INVALID_ARGUMENT);
      }
      final ExpandableArrayBuffer frameBytes = new ExpandableArrayBuffer(frameLength);
      BatchFrame.serialize(frameBytes, 0, first, sourcePosition, timestamp, entries);
      pending.fulfill(first, new DirectBufferWriter().wrap(frameBytes, 0, frameLength));
    } catch (final RuntimeException e) {
      LOG.warn("批帧序列化失败, 烧毁区间 [{}, {}]", first, last, e);
      burn(first, last);
      metrics.incRejectedInvalid();
      metrics.incRejected(context, RejectionReason.INVALID_ARGUMENT);
      return new Rejected(RejectionReason.INVALID_ARGUMENT);
    }

    metrics.incAppended();
    metrics.incAppended(context, count);
    drain();
    return new Appended(first, last);
  }

  @Override
  public boolean canAppend(final int entryCount, final int batchSizeBytes) {
    return !closed
        && batchSizeBytes <= maxBatchSize
        && pending.hasCapacity()
        && flowControl.canAcquire(WriteContext.USER_COMMAND, entryCount);
  }

  @Override
  public void close() {
    closed = true;
  }

  /** 测试专用：调整看门狗阈值与内部状态可见性 */
  void setReserveTimeoutForTest(final long nanos) {
    this.reserveTimeoutNanos = nanos;
  }

  /** 测试专用：占一个过期槽位并消耗对应 position 区间（触发看门狗路径） */
  void reserveExpiredSlotForTest(final int count) {
    final int slot = pending.tryClaim();
    final long first = nextPosition.getAndAdd(count);
    pending.reserve(slot, first, first + count - 1, System.nanoTime() - 1);
  }

  long submittedThroughForTest() {
    return submittedThrough;
  }

  /** 预约后失败：标记烧毁并推进提交链（释放流控占位） */
  private void burn(final long first, final long last) {
    pending.fulfill(first, null);
    flowControl.onAppendRolledBack(first, last);
    drain();
  }

  /**
   * 有序提交：按 submittedThrough+1 找槽推进。READY(帧)→提交；READY(null)→烧毁推进； RESERVED 未超期→停止等待；超期→看门狗烧毁；无槽→停止。
   */
  private synchronized void drain() {
    while (true) {
      final long expect = submittedThrough + 1;
      final int slot = pending.slotOfFirst(expect);
      if (slot < 0) {
        return;
      }
      if (!pending.isReady(slot)) {
        if (System.nanoTime() < pending.deadline(slot)) {
          return; // 写线程序列化中, 正常等待
        }
        LOG.warn("提交链槽位超期, 看门狗烧毁区间起始于 {}", expect);
        pending.fulfill(expect, null);
        flowControl.onAppendRolledBack(expect, pending.lastOf(slot));
      }
      final DirectBufferWriter frame = pending.take(slot);
      final long last = pending.lastOf(slot);
      pending.free(slot);
      if (frame == null) {
        submittedThrough = last;
        metrics.incBurned(last - expect + 1);
        continue;
      }
      try {
        store.append(expect, last, frame, flowControl);
      } catch (final RuntimeException e) {
        LOG.warn("存储追加同步失败, 烧毁区间 [{}, {}]", expect, last, e);
        flowControl.onFailure(last, e);
      }
      submittedThrough = last;
    }
  }
}
