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

/**
 * 追加→处理往返延迟估计：EMA（α=0.2）+ 最小值跟踪。
 *
 * <p>仅由处理线程单线程更新（onProcessed 路径），无竞争；窗口梯度 = EMA/minRTT。
 */
final class RttEstimator {

  private static final double ALPHA = 0.2;

  private long minRttNanos = Long.MAX_VALUE;
  private double emaRttNanos = Double.NaN;

  void sample(final long rttNanos) {
    if (rttNanos <= 0) {
      return;
    }
    if (rttNanos < minRttNanos) {
      minRttNanos = rttNanos;
    }
    emaRttNanos =
        Double.isNaN(emaRttNanos) ? rttNanos : emaRttNanos + ALPHA * (rttNanos - emaRttNanos);
  }

  /** 是否已可计算梯度（至少一个样本） */
  boolean ready() {
    return !Double.isNaN(emaRttNanos);
  }

  /** RTT 梯度（>1 表示拥塞趋升） */
  double gradient() {
    return emaRttNanos / minRttNanos;
  }

  long minRttNanos() {
    return minRttNanos;
  }

  double emaRttNanos() {
    return emaRttNanos;
  }
}
