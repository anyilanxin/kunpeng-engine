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
 * 在有序主调度器与一组车道调度器之间路由 {@link TimerScheduler} 调用。
 *
 * <p>{@code routeAllToCompute} 为真时，连同步方法也会派发到 {@link ExecutionLane#COMPUTE} 车道，主 actor
 * 零调度负载。异步方法始终在独立的车道 actor 上执行，因此并发运行，不保证 顺序或同线程执行。
 *
 * <p>车道调度器只暴露同步入口（实现 {@link TimerScheduler}），因此这里的异步方法委托给 车道的同步方法；返回的句柄即车道自身的句柄，由于车道队列可从任意线程入列，取消立即
 * 生效。
 */
public final class AsyncTimerRouter implements AsyncTimerScheduler {

  private final TimerScheduler primary;
  private final LanePool lanes;
  private final boolean routeAllToCompute;

  public AsyncTimerRouter(
      final TimerScheduler primary, final LanePool lanes, final boolean routeAllToCompute) {
    this.primary = primary;
    this.lanes = lanes;
    this.routeAllToCompute = routeAllToCompute;
  }

  /** 返回一个把所有入口（含同步）都路由到 compute 车道的路由器。 */
  public static AsyncTimerRouter alwaysAsync(final TimerScheduler primary, final LanePool lanes) {
    return new AsyncTimerRouter(primary, lanes, true);
  }

  @Override
  public TimerHandle scheduleAfter(final Duration delay, final Runnable action) {
    return routeAllToCompute
        ? computeLane().scheduleAfter(delay, action)
        : primary.scheduleAfter(delay, action);
  }

  @Override
  public TimerHandle scheduleAfter(final Duration delay, final TimerJob job) {
    return routeAllToCompute
        ? computeLane().scheduleAfter(delay, job)
        : primary.scheduleAfter(delay, job);
  }

  @Override
  public TimerHandle scheduleAt(final long epochMilli, final Runnable action) {
    return routeAllToCompute
        ? computeLane().scheduleAt(epochMilli, action)
        : primary.scheduleAt(epochMilli, action);
  }

  @Override
  public TimerHandle scheduleAt(final long epochMilli, final TimerJob job) {
    return routeAllToCompute
        ? computeLane().scheduleAt(epochMilli, job)
        : primary.scheduleAt(epochMilli, job);
  }

  @Override
  public void scheduleEvery(final Duration period, final TimerJob job) {
    if (routeAllToCompute) {
      computeLane().scheduleEvery(period, job);
    } else {
      primary.scheduleEvery(period, job);
    }
  }

  @Override
  public void scheduleEveryAsync(final Duration period, final TimerJob job) {
    computeLane().scheduleEvery(period, job);
  }

  @Override
  public TimerHandle scheduleAfterAsync(final Duration delay, final TimerJob job) {
    return computeLane().scheduleAfter(delay, job);
  }

  @Override
  public TimerHandle scheduleAtAsync(final long epochMilli, final TimerJob job) {
    return computeLane().scheduleAt(epochMilli, job);
  }

  @Override
  public void scheduleEveryAsync(
      final Duration period, final TimerJob job, final ExecutionLane lane) {
    lanes.actorFor(lane).scheduler().scheduleEvery(period, job);
  }

  @Override
  public TimerHandle scheduleAfterAsync(
      final Duration delay, final TimerJob job, final ExecutionLane lane) {
    return lanes.actorFor(lane).scheduler().scheduleAfter(delay, job);
  }

  @Override
  public TimerHandle scheduleAtAsync(
      final long epochMilli, final TimerJob job, final ExecutionLane lane) {
    return lanes.actorFor(lane).scheduler().scheduleAt(epochMilli, job);
  }

  private TimerScheduler computeLane() {
    return lanes.actorFor(ExecutionLane.COMPUTE).scheduler();
  }
}
