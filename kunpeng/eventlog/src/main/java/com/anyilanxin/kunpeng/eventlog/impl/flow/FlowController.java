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
 * 流控总控：三态水位（append→write→commit→processed）+ 双限流（AIMD 窗口 / 令牌桶） + 积压调节。
 *
 * <p>线程契约（各路径单线程或顺序化，热路径零分配）：
 *
 * <ul>
 *   <li>{@code tryAcquire/onAppend}：定序提交路径（firstPosition 升序串行）
 *   <li>{@code onWrite/onCommit/onFailure}：存储回调线程（提交序）
 *   <li>{@code onProcessed}：处理 actor（单线程）
 *   <li>{@code onExported}：导出线程
 * </ul>
 */
public final class FlowController implements LogFlowControl, EventStore.AppendListener {

  /** 在途槽位陈旧阈值（消费方未释放时的驱逐兜底, 正常生命周期毫秒级释放） */
  private static final long STALE_INFLIGHT_NANOS = 60_000_000_000L; // 60s

  private final FlowControlParams params;
  private final AimdWindow window;
  private final TokenBucket rateLimiter;
  private final BacklogThrottle throttle;
  private final InflightRing inflight = new InflightRing();
  private final LongSupplier nanoClock;
  private final EventLogMetrics metrics;

  private final AtomicLong lastWritten = new AtomicLong();
  private final AtomicLong lastCommitted = new AtomicLong();
  private final AtomicLong lastProcessed = new AtomicLong();

  public FlowController(
      final FlowControlParams params, final LongSupplier nanoClock, final EventLogMetrics metrics) {
    this.params = params;
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
    this.rateLimiter =
        params.rateLimitEnabled()
            ? new TokenBucket(
                params.writeRateLimitPerSecond(),
                params.writeRateBurst() > 0
                    ? params.writeRateBurst()
                    : params.writeRateLimitPerSecond(),
                nanoClock)
            : null;
    this.throttle =
        rateLimiter != null
            ? new BacklogThrottle(
                rateLimiter,
                params.writeRateLimitPerSecond(),
                params.minThrottleRate(),
                params.acceptableBacklog(),
                params.throttleResolution(),
                nanoClock)
            : null;
  }

  /**
   * 流量准入（定序前调用；拒绝时不产生任何状态）。
   *
   * @return null 表示放行，否则拒绝原因
   */
  public RejectionReason tryAcquire(final WriteContext context, final int entryCount) {
    if (context != WriteContext.USER_COMMAND) {
      return null;
    }
    if (window != null && !window.tryAcquire()) {
      metrics.incRejectedWindow();
      return RejectionReason.REQUEST_WINDOW_EXHAUSTED;
    }
    if (rateLimiter != null && !rateLimiter.tryAcquire(entryCount)) {
      if (window != null) {
        window.release();
      }
      metrics.incRejectedRate();
      return RejectionReason.WRITE_RATE_EXHAUSTED;
    }
    return null;
  }

  /** 非破坏性余量探测 */
  public boolean canAcquire(final WriteContext context, final int entryCount) {
    if (context != WriteContext.USER_COMMAND) {
      return true;
    }
    if (window != null && window.inflight() >= window.window()) {
      return false;
    }
    return rateLimiter == null || rateLimiter.hasTokens(entryCount);
  }

  /** 已定序（提交路径按序调用）：登记在途批（含陈旧滞留驱逐兜底） */
  public void onAppend(final long firstPosition, final long lastPosition, final int entryCount) {
    final long now = nanoClock.getAsLong();
    // 兜底: 槽位被滞留超期的旧批占用（消费方未按生命周期释放）→ 驱逐而非崩溃
    // （旧实现为无界表静默泄漏, 此处显式驱逐并告警）
    final long stale = inflight.staleOccupant(lastPosition, now, STALE_INFLIGHT_NANOS);
    if (stale >= 0) {
      metrics.incBurned(1);
      releaseUpTo(stale, false);
    }
    inflight.add(lastPosition, now);
    if (window != null) {
      metrics.window(window.window());
      metrics.inflight(window.inflight());
    }
  }

  /** 追加失败回滚（定序后、提交前同步异常）：释放占位不采样 */
  public void onAppendRolledBack(final long firstPosition, final long lastPosition) {
    releaseUpTo(lastPosition, false);
    metrics.incBurned(lastPosition - firstPosition + 1);
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
    if (throttle != null) {
      throttle.onCommitted(lastWritten.get(), 1);
    }
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
  public void onExported(final long position) {
    if (throttle != null) {
      throttle.onExported(position);
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

  private void releaseUpTo(final long position, final boolean processed) {
    long next = inflight.nextActive(Long.MIN_VALUE);
    while (next != Long.MAX_VALUE && next <= position) {
      if (processed) {
        final long appendedAt = inflight.appendedAt(next);
        if (window != null) {
          window.onSuccess(appendedAt < 0 ? 0 : nanoClock.getAsLong() - appendedAt);
        }
      } else if (window != null) {
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
