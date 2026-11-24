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

/**
 * 默认时钟：每毫秒至多采样一次 System.currentTimeMillis; 亚毫秒精度查询每次调用 nanoTime。
 *
 * <p>毫秒值 = 最近一次采样时的（墙上时钟 − 单调时钟）+ 当前单调时钟：采样结果只占一个 volatile 字段，读侧随时用单调时钟外推。
 * 载体线程忙于一个长任务（采样暂停）期间，被其它线程读到的时间照常前进。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultActorClock implements ActorClock {

  /** 最近一次采样时墙上时钟落后单调时钟多少毫秒（一次 volatile 写发布，读侧无需再同步） */
  private volatile long wallMinusNanoMillis;

  private volatile long currentNanoTime;

  public DefaultActorClock() {
    currentNanoTime = System.nanoTime();
    wallMinusNanoMillis = System.currentTimeMillis() - currentNanoTime / 1_000_000L;
  }

  @Override
  public boolean update() {
    final long nowNano = System.nanoTime();
    final long elapsed = nowNano - currentNanoTime;
    if (elapsed >= 1_000_000L) {
      currentNanoTime = nowNano;
      wallMinusNanoMillis = System.currentTimeMillis() - nowNano / 1_000_000L;
      return true;
    }
    return false;
  }

  @Override
  public long getTimeMillis() {
    return wallMinusNanoMillis + System.nanoTime() / 1_000_000L;
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
