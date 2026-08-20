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

import java.time.Duration;
import java.util.function.LongSupplier;

/** 积压调节：written-exported 差超阈值时按观察写入速率压低令牌桶，回落即恢复上限。 每分辨率周期至多调节一次（提交线程顺序调用，无锁）。 */
final class BacklogThrottle {

  private final TokenBucket bucket;
  private final double configuredRate;
  private final double minRate;
  private final long acceptableBacklog;
  private final long resolutionNanos;
  private final LongSupplier nanoClock;

  private long lastAdjustNanos;
  private long entriesSinceLastPeriod;
  private long periodStartNanos;
  private volatile long lastExportedPosition;

  BacklogThrottle(
      final TokenBucket bucket,
      final double configuredRate,
      final double minRate,
      final long acceptableBacklog,
      final Duration resolution,
      final LongSupplier nanoClock) {
    this.bucket = bucket;
    this.configuredRate = configuredRate;
    this.minRate = minRate;
    this.acceptableBacklog = acceptableBacklog;
    this.resolutionNanos = resolution.toNanos();
    this.nanoClock = nanoClock;
    final long now = nanoClock.getAsLong();
    this.lastAdjustNanos = now;
    this.periodStartNanos = now;
  }

  /** 提交路径：计入观察速率，按周期调节 */
  void onCommitted(final long lastWrittenPosition, final int entryCount) {
    entriesSinceLastPeriod += entryCount;
    final long now = nanoClock.getAsLong();
    if (now - lastAdjustNanos < resolutionNanos) {
      return;
    }
    final long backlog = Math.max(0, lastWrittenPosition - lastExportedPosition);
    if (backlog <= acceptableBacklog) {
      if (bucket.permitsPerSecond() < configuredRate) {
        bucket.setRate(configuredRate);
      }
    } else {
      final double periodSeconds = Math.max(1, now - periodStartNanos) / 1e9;
      final double observedRate = entriesSinceLastPeriod / periodSeconds;
      final double factor = (double) acceptableBacklog / backlog;
      bucket.setRate(Math.max(minRate, Math.min(configuredRate, observedRate * factor)));
    }
    entriesSinceLastPeriod = 0;
    periodStartNanos = now;
    lastAdjustNanos = now;
  }

  /** 导出路径（exporter 线程） */
  void onExported(final long position) {
    if (position > lastExportedPosition) {
      lastExportedPosition = position;
    }
  }

  double currentRate() {
    return bucket.permitsPerSecond();
  }
}
