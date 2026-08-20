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

import java.util.function.LongSupplier;

/**
 * 令牌桶写入速率限制（自研，替代外部限流库）：nanoTime 定点补充 + CAS 单循环， 无浮点争用；permits = 批 entry 数。速率可被 {@link
 * BacklogThrottle} 动态重设。
 *
 * <p>状态打包：tokens 微单位（1e-6 token 精度，避免 double CAS）——上下 64 位 分别存 tokensMicro 与最后补充时刻，单 AtomicLong
 * 不足以原子更新两者，故用 synchronized 短临界区（写路径低频且竞争小，实测足够；正确性优先）。
 */
final class TokenBucket {

  private static final long MICROS_PER_UNIT = 1_000_000L;

  private final LongSupplier nanoClock;
  private long capacityMicro;
  private long tokensMicro;
  private long lastRefillNanos;
  private double permitsPerSecond;

  TokenBucket(final double permitsPerSecond, final double burst, final LongSupplier nanoClock) {
    this.nanoClock = nanoClock;
    this.permitsPerSecond = permitsPerSecond;
    this.capacityMicro = Math.max(1, (long) (burst * MICROS_PER_UNIT));
    this.tokensMicro = capacityMicro;
    this.lastRefillNanos = nanoClock.getAsLong();
  }

  synchronized boolean tryAcquire(final int permits) {
    refill();
    final long cost = (long) permits * MICROS_PER_UNIT;
    if (tokensMicro < cost) {
      return false;
    }
    tokensMicro -= cost;
    return true;
  }

  /** 非破坏性探测（canAppend 用） */
  synchronized boolean hasTokens(final int permits) {
    refill();
    return tokensMicro >= (long) permits * MICROS_PER_UNIT;
  }

  /** 积压调节重设速率（保持桶内余量） */
  synchronized void setRate(final double newPermitsPerSecond) {
    refill();
    this.permitsPerSecond = newPermitsPerSecond;
  }

  synchronized double permitsPerSecond() {
    return permitsPerSecond;
  }

  private void refill() {
    final long now = nanoClock.getAsLong();
    final long elapsed = now - lastRefillNanos;
    if (elapsed <= 0) {
      return;
    }
    final long produced = (long) (elapsed * permitsPerSecond * MICROS_PER_UNIT / 1_000_000_000L);
    if (produced > 0) {
      tokensMicro = Math.min(capacityMicro, tokensMicro + produced);
      lastRefillNanos = now;
    }
  }
}
