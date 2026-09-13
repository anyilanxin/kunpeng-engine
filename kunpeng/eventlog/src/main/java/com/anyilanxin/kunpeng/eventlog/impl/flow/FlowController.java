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
package com.anyilanxin.kunpeng.eventlog.impl.flow;

import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.LogFlowControl;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 流控总控：三态水位（append→write→commit→processed）+ AIMD 在途窗口背压（唯一准入控制，仅约束 {@link
 * WriteContext#USER_COMMAND}；内部上下文零耦合）。
 *
 * <p>线程契约（各路径单线程或顺序化，热路径零分配）：
 *
 * <ul>
 *   <li>{@code tryAcquire/onAppend}：定序提交路径（firstPosition 升序串行）
 *   <li>{@code onWrite/onCommit/onFailure}：存储回调线程（提交序）
 *   <li>{@code onProcessed}：处理 actor（单线程）
 * </ul>
 */
public final class FlowController implements LogFlowControl, EventStore.AppendListener {

  private final AimdWindow window;
  private final InflightRing inflight = new InflightRing();
  private final LongSupplier nanoClock;
  private final EventLogMetrics metrics;

  private final AtomicLong lastWritten = new AtomicLong();
  private final AtomicLong lastCommitted = new AtomicLong();
  private final AtomicLong lastProcessed = new AtomicLong();

  public FlowController(
      final FlowControlParams params, final LongSupplier nanoClock, final EventLogMetrics metrics) {
    this.nanoClock = nanoClock;
    this.metrics = metrics;
    this.window =
        params.aimdEnabled()
            ? new AimdWindow(
                params.requestWindowInitial(),
                params.requestWindowMin(),
                params.requestWindowMax(),
                params.rttTolerance())
            : null;
  }

  /**
   * 流量准入（定序前调用；拒绝时不产生任何状态）。仅 USER_COMMAND 消耗窗口占位（每批 1 个）； 内部上下文直接放行。
   *
   * @return null 表示放行，否则拒绝原因
   */
  public RejectionReason tryAcquire(final WriteContext context) {
    if (context != WriteContext.USER_COMMAND) {
      return null;
    }
    if (window != null && !window.tryAcquire()) {
      metrics.incRejectedWindow();
      return RejectionReason.REQUEST_WINDOW_EXHAUSTED;
    }
    return null;
  }

  /** 非破坏性余量探测 */
  public boolean canAcquire(final WriteContext context) {
    if (context != WriteContext.USER_COMMAND) {
      return true;
    }
    return window == null || window.inflight() < window.window();
  }

  /** AIMD 窗口是否生效（定序器据此决定登记标志） */
  public boolean windowActive() {
    return window != null && window.enabled();
  }

  /** 准入放行后未入日志的归还路径（临界区内拒绝）: 仅归还窗口占位, 不触在途环 */
  public void abandonAdmission(final WriteContext context) {
    if (context == WriteContext.USER_COMMAND && window != null) {
      window.release();
    }
  }

  /**
   * 已定序（提交路径按序调用）。仅 {@code windowTaken=true}（持背压占位）的批次登记在途环； 内部上下文批次与流控零耦合——不登记、无生命周期、永不因流控状态失败。
   */
  public void onAppend(
      final long firstPosition,
      final long lastPosition,
      final int entryCount,
      final boolean windowTaken) {
    if (windowTaken) {
      // 兜底（窗口上限 ≥ 环容量等极端配置）: 同槽覆盖驱逐而非失败, 被驱逐者占位保守归还
      final long displaced = inflight.add(lastPosition, nanoClock.getAsLong());
      if (displaced >= 0) {
        metrics.incEvicted(1);
        window.release();
      }
    }
    if (window != null) {
      metrics.window(window.window());
      metrics.inflight(window.inflight());
    }
  }

  /** 测试专用: 窗口在途计数直读 */
  int windowInflight() {
    return window != null ? window.inflight() : 0;
  }

  // ===== 存储回调（提交序） =====

  @Override
  public void onWrite(final long index, final long lastPosition) {
    inflight.markWritten(lastPosition);
    maxCas(lastWritten, lastPosition);
    metrics.lastWritten(lastWritten.get());
  }

  @Override
  public void onCommit(final long index, final long lastPosition) {
    inflight.markCommitted(lastPosition);
    maxCas(lastCommitted, lastPosition);
    metrics.lastCommitted(lastCommitted.get());
    metrics.incCommit();
  }

  @Override
  public void onFailure(final long lastPosition, final Throwable cause) {
    releaseUpTo(lastPosition, false);
    metrics.incBurned(1);
  }

  // ===== 消费侧 =====

  @Override
  public void onProcessed(final long position) {
    releaseUpTo(position, true);
    maxCas(lastProcessed, position);
    metrics.lastProcessed(lastProcessed.get());
    if (window != null) {
      metrics.inflight(window.inflight());
    }
  }

  @Override
  public long lastWrittenPosition() {
    return lastWritten.get();
  }

  @Override
  public long lastCommittedPosition() {
    return lastCommitted.get();
  }

  @Override
  public long lastProcessedPosition() {
    return lastProcessed.get();
  }

  /** 关闭时全量释放（防泄漏） */
  public void releaseAll() {
    releaseUpTo(Long.MAX_VALUE, false);
  }

  /** 打开期播种水位（恢复出的 lastPosition 视为已提交持久前缀） */
  public void seedPositions(final long lastPosition) {
    maxCas(lastWritten, lastPosition);
    maxCas(lastCommitted, lastPosition);
  }

  /** 释放 position 之前所有在途批（环内仅含窗口批次，逐批归还占位; processed 时采样 RTT） */
  private void releaseUpTo(final long position, final boolean processed) {
    long next = inflight.nextActive(Long.MIN_VALUE);
    while (next != Long.MAX_VALUE && next <= position) {
      if (processed) {
        final long appendedAt = inflight.appendedAt(next);
        window.onSuccess(appendedAt < 0 ? 0 : nanoClock.getAsLong() - appendedAt);
      } else {
        window.release();
      }
      inflight.release(next);
      next = inflight.nextActive(next + 1);
    }
  }

  private static void maxCas(final AtomicLong target, final long value) {
    long current;
    do {
      current = target.get();
      if (current >= value) {
        return;
      }
    } while (!target.compareAndSet(current, value));
  }
}
