/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import java.time.Duration;

/**
 * 调度定时任务，任务在其所属上下文中按序执行。
 *
 * <p>实现保证任务相对其绑定的上下文有序且单线程执行；不保证顺序的并发调度由继承本接口的 {@link AsyncTimerScheduler} 提供。延迟已到期的句柄再调用取消可能不生效。
 */
public interface TimerScheduler {

  /**
   * 在 {@code delay} 之后调度执行一个动作。
   *
   * @return 可在该动作执行前取消它的句柄
   */
  TimerHandle scheduleAfter(Duration delay, Runnable action);

  /**
   * 在 {@code delay} 之后调度执行一个任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAfter(Duration delay, TimerJob job);

  /**
   * 在给定的纪元毫秒时刻调度执行一个任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAt(long epochMilli, TimerJob job);

  /**
   * 在给定的纪元毫秒时刻调度执行一个动作。
   *
   * @return 可在该动作执行前取消它的句柄
   */
  TimerHandle scheduleAt(long epochMilli, Runnable action);

  /** 调度周期动作，每次执行后自动重新调度，直到被取消或调度器关闭。 */
  default void scheduleEvery(final Duration period, final Runnable action) {
    scheduleAfter(
        period,
        () -> {
          try {
            action.run();
          } finally {
            scheduleEvery(period, action);
          }
        });
  }

  /** 调度周期任务。 */
  void scheduleEvery(Duration period, TimerJob job);

  /** 已调度工作单元的句柄，可在其执行前取消。 */
  @FunctionalInterface
  interface TimerHandle {
    void cancel();
  }
}
