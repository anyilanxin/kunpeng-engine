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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIMD 在途窗口（自研，替代外部并发限流库）：RTT 梯度（EMA/minRTT）低于容差线性增， 超限按梯度乘性减；窗口调整按成功次数节流（防振荡）。
 *
 * <p>仅约束 {@code USER_COMMAND}：acquire 在写线程（并发，CAS 计数），onSuccess 在 处理线程（单线程，无竞争）。
 */
final class AimdWindow {

  private static final int UPDATE_INTERVAL = 100;

  private final int min;
  private final int max;
  private final double tolerance;
  private final boolean enabled;
  private final RttEstimator rtt = new RttEstimator();

  private volatile int window;
  private final AtomicInteger inflight = new AtomicInteger();
  private int successSinceUpdate;

  AimdWindow(final int initial, final int min, final int max, final double tolerance) {
    this.min = min;
    this.max = max;
    this.tolerance = tolerance;
    this.enabled = max > 1;
    this.window = Math.max(min, Math.min(max, initial));
  }

  boolean tryAcquire() {
    if (!enabled) {
      return true;
    }
    while (true) {
      final int current = inflight.get();
      if (current >= window) {
        return false;
      }
      if (inflight.compareAndSet(current, current + 1)) {
        return true;
      }
    }
  }

  /** 释放一个占位（失败/忽略路径，不采样 RTT） */
  void release() {
    if (enabled) {
      inflight.decrementAndGet();
    }
  }

  /** 处理完成：释放占位 + RTT 采样 + 节流后的窗口调整 */
  void onSuccess(final long rttNanos) {
    if (!enabled) {
      return;
    }
    inflight.decrementAndGet();
    rtt.sample(rttNanos);
    if (++successSinceUpdate >= UPDATE_INTERVAL && rtt.ready()) {
      successSinceUpdate = 0;
      final double gradient = rtt.gradient();
      if (gradient < 1 + tolerance) {
        window = Math.min(max, window + 1);
      } else {
        final double decrease = 1 - 0.5 * (1 - 1 / gradient);
        window = Math.max(min, (int) (window * Math.max(decrease, 0.5)));
      }
    }
  }

  int window() {
    return window;
  }

  int inflight() {
    return inflight.get();
  }

  boolean enabled() {
    return enabled;
  }
}
