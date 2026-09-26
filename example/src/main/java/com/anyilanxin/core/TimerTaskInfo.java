/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.core;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zxuanhong
 * @date 2026-04-23 12:23
 * @since
 */
public abstract class TimerTaskInfo implements TimerTask, AutoCloseable {
  private final HashedWheelTimer wheelTimer;
  private final long delay;
  private final TimeUnit unit;
  private final AtomicBoolean state = new AtomicBoolean(false);
  private final AtomicBoolean cycleStart = new AtomicBoolean(false);

  public TimerTaskInfo(final HashedWheelTimer wheelTimer, final long delay, final TimeUnit unit) {
    this.wheelTimer = wheelTimer;
    this.delay = delay;
    this.unit = unit;
  }

  public void start() {
    wheelTimer.newTimeout(this, delay, unit);
    state.set(true);
  }

  public void cycleStart() {
    cycleStart.set(true);
    start();
  }

  @Override
  public void run(final Timeout timeout) {
    if (state.get()) {
      process();
      if (cycleStart.get()) {
        wheelTimer.newTimeout(this, delay, unit);
      }
    }
  }

  @Override
  public void close() {
    state.set(false);
  }

  public abstract void process();
}
