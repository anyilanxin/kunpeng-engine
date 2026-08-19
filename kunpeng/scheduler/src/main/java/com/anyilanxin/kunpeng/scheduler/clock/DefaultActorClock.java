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

/** 默认时钟：每毫秒至多一次 System.currentTimeMillis */
public class DefaultActorClock implements ActorClock {

  private long currentNanoTime;
  private long currentTimeMillis;

  public DefaultActorClock() {
    currentNanoTime = System.nanoTime();
    currentTimeMillis = System.currentTimeMillis();
  }

  @Override
  public boolean update() {
    final long nowNano = System.nanoTime();
    final long elapsed = nowNano - currentNanoTime;
    if (elapsed >= 1_000_000L) {
      currentNanoTime = nowNano;
      currentTimeMillis = System.currentTimeMillis();
      return true;
    }
    return false;
  }

  @Override
  public long getTimeMillis() {
    return currentTimeMillis;
  }

  @Override
  public long getNanosSinceLastMillisecond() {
    return System.nanoTime() - currentNanoTime;
  }

  @Override
  public long getNanoTime() {
    return currentNanoTime;
  }
}
