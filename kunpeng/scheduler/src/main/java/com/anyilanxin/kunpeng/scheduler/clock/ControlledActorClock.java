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
package com.anyilanxin.kunpeng.scheduler.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** 测试用受控时钟：可 pin 到指定时刻或 offset 推进 */
public class ControlledActorClock implements ActorClock {

  private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());
  private final AtomicLong currentNanoTime = new AtomicLong(System.nanoTime());
  private volatile boolean pinCurrentTime = false;

  public ControlledActorClock pin(final Instant instant) {
    pinCurrentTime = true;
    currentTimeMillis.set(instant.toEpochMilli());
    return this;
  }

  public ControlledActorClock addTime(final Duration duration) {
    currentTimeMillis.addAndGet(duration.toMillis());
    currentNanoTime.addAndGet(duration.toNanos());
    return this;
  }

  public void reset() {
    pinCurrentTime = false;
    currentTimeMillis.set(System.currentTimeMillis());
    currentNanoTime.set(System.nanoTime());
  }

  @Override
  public boolean update() {
    if (!pinCurrentTime) {
      currentTimeMillis.set(System.currentTimeMillis());
      currentNanoTime.set(System.nanoTime());
      return true;
    }
    return false;
  }

  @Override
  public long getTimeMillis() {
    return currentTimeMillis.get();
  }

  @Override
  public long getNanosSinceLastMillisecond() {
    return 0;
  }

  @Override
  public long getNanoTime() {
    return currentNanoTime.get();
  }
}
