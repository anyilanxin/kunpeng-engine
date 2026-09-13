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
 * 在 {@link TimerScheduler} 之上额外提供异步入口的调度器。
 *
 * <p>异步调度不保证顺序与单线程执行：任务在独立的车道 actor 上并发运行。未指定车道的方法 默认使用 {@link ExecutionLane#COMPUTE} 车道。从 {@link
 * TimerScheduler} 继承的同步方法仍 保持其顺序约定。
 */
public interface AsyncTimerScheduler extends TimerScheduler {

  /** 在默认异步车道上调度周期任务。 */
  void scheduleEveryAsync(Duration period, TimerJob job);

  /**
   * 在默认异步车道上调度 {@code delay} 之后执行的任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAfterAsync(Duration delay, TimerJob job);

  /**
   * 在默认异步车道上调度给定纪元毫秒时刻执行的任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAtAsync(long epochMilli, TimerJob job);

  /** 在给定异步车道上调度周期任务。 */
  void scheduleEveryAsync(Duration period, TimerJob job, ExecutionLane lane);

  /**
   * 在给定异步车道上调度 {@code delay} 之后执行的任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAfterAsync(Duration delay, TimerJob job, ExecutionLane lane);

  /**
   * 在给定异步车道上调度给定纪元毫秒时刻执行的任务。
   *
   * @return 可在该任务执行前取消它的句柄
   */
  TimerHandle scheduleAtAsync(long epochMilli, TimerJob job, ExecutionLane lane);
}
