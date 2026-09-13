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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import org.agrona.ExpandableArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单临界区定序器（{@link EventLogWriter} 实现）。
 *
 * <p>写者线程在一个同步临界区内完成「取号 → 算长 → 组帧 → 登记在途 → 存储移交 → 推进取号」。 可失败步骤全部位于取号提交点之前（拒绝零消耗）或存储移交之后（异步生命周期由
 * {@link EventStore.AppendListener} 回调承接），结构上不存在半成品 position 区间——
 * 无需提交链、看门狗与区间烧毁推进，故障只会拒绝当批，永不影响后续写入。
 *
 * <p>不变量：
 *
 * <ul>
 *   <li>position 由临界区独占推进，成功区间并集无重叠；存储调用序 == firstPosition 升序
 *   <li>拒绝（关闭/空批/超限/序列化失败）不消耗 position
 *   <li>存储移交同步失败：区间烧毁（onFailure 后永不 onCommit），调用方仍收到 Appended
 *   <li>在途环仅登记持背压占位的批次（≤ 窗口上限 &lt; 环容量 1024）；内部上下文与流控零耦合， 永不拒绝、永不因流控状态失败
 * </ul>
 */
public final class PositionSequencer implements EventLogWriter {

  private static final Logger LOG = LoggerFactory.getLogger(PositionSequencer.class);

  private final EventStore store;
  private final FlowController flowControl;
  private final EventLogMetrics metrics;
  private final int maxBatchSize;
  private final Clock wallClock;

  private long nextWritePosition; // 临界区独占; 提交点前失败不消耗
  private volatile boolean closed;

  public PositionSequencer(
      final EventStore store,
      final FlowController flowControl,
      final EventLogMetrics metrics,
      final int maxBatchSize,
      final long initialPosition,
      final Clock wallClock) {
    this.store = store;
    this.flowControl = flowControl;
    this.metrics = metrics;
    this.maxBatchSize = maxBatchSize;
    this.wallClock = wallClock;
    this.nextWritePosition = initialPosition;
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
    // 准入在锁外: 拒绝时零状态; 临界区内退出路径须归还占位
    final RejectionReason admission = flowControl.tryAcquire(context);
    if (admission != null) {
      metrics.incRejected(context, admission);
      return new Rejected(admission);
    }
    final AppendResult result = appendUnderLock(context, entries, sourcePosition);
    if (result instanceof Rejected) {
      flowControl.abandonAdmission(context);
    }
    return result;
  }

  @Override
  public boolean canAppend(final int entryCount, final int batchSizeBytes) {
    return !closed
        && batchSizeBytes <= maxBatchSize
        && flowControl.canAcquire(WriteContext.USER_COMMAND);
  }

  @Override
  public void close() {
    closed = true;
  }

  /** 单临界区写入: 取号提交点之前的任何失败直接拒绝（position 未消耗）; 递交点之后的失败由存储回调生命周期承接。 */
  private synchronized AppendResult appendUnderLock(
      final WriteContext context, final List<AppendEntry> entries, final long sourcePosition) {
    if (closed) {
      metrics.incRejected(context, RejectionReason.CLOSED);
      return new Rejected(RejectionReason.CLOSED);
    }
    final int count = entries.size();
    final long first = nextWritePosition;
    final long last = first + count - 1;
    final long timestamp = wallClock.millis();
    final int frameLength = BatchFrame.calculateLength(first, sourcePosition, timestamp, entries);
    if (frameLength > maxBatchSize) {
      metrics.incRejectedInvalid();
      metrics.incRejected(context, RejectionReason.INVALID_ARGUMENT);
      return new Rejected(RejectionReason.INVALID_ARGUMENT);
    }
    final ExpandableArrayBuffer frameBytes = new ExpandableArrayBuffer(frameLength);
    try {
      BatchFrame.serialize(frameBytes, 0, first, sourcePosition, timestamp, entries);
    } catch (final RuntimeException e) {
      LOG.warn("批帧序列化失败, 本批未入日志: [{}, {}]", first, last, e);
      metrics.incRejectedInvalid();
      metrics.incRejected(context, RejectionReason.INVALID_ARGUMENT);
      return new Rejected(RejectionReason.INVALID_ARGUMENT);
    }

    // 递交点: 登记 → 移交 → 推进取号; 此后无本地可失败路径
    flowControl.onAppend(
        first, last, count, context == WriteContext.USER_COMMAND && flowControl.windowActive());
    try {
      store.append(
          first, last, new DirectBufferWriter().wrap(frameBytes, 0, frameLength), flowControl);
    } catch (final RuntimeException e) {
      // 存储契约: onFailure 后该批永不 onCommit, 区间烧毁, 读者容忍 gap
      LOG.warn("存储移交同步失败, 烧毁区间 [{}, {}]", first, last, e);
      flowControl.onFailure(last, e);
    }
    nextWritePosition = last + 1;

    metrics.incAppended();
    metrics.incAppended(context, count);
    return new Appended(first, last);
  }
}
