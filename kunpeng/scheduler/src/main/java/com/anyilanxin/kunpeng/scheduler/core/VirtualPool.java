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
package com.anyilanxin.kunpeng.scheduler.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 虚拟线程组：cell → 载体一一绑定 */
public final class VirtualPool implements CellPool {

  private final ConcurrentMap<ActorCell, VirtualCarrier> carriers = new ConcurrentHashMap<>();
  private final BlockingRunner blocking;
  private final SchedulerMetrics metrics;
  private volatile boolean stopped;

  public VirtualPool(final BlockingRunner blocking, final SchedulerMetrics metrics) {
    this.blocking = blocking;
    this.metrics = metrics;
    metrics.registerQueueDepthGauge("virtual", carriers::size);
  }

  @Override
  public String poolName() {
    return "virtual";
  }

  SchedulerMetrics getMetrics() {
    return metrics;
  }

  public void submitTask(final ActorCell cell) {
    metrics.incActorSubmitted();
    cell.onScheduled(this, metrics);
    cell.wakeSignal();
  }

  public void route(final ActorCell cell) {
    final VirtualCarrier carrier = carriers.computeIfAbsent(cell, c -> new VirtualCarrier(this, c));
    carrier.start();
    carrier.hint();
  }

  public void stop() {
    stopped = true;
    carriers.values().forEach(VirtualCarrier::signalStop);
  }

  /** 载体线程退出（终态退线程/池关停）时自移除; 移除后复查调度门, 丢失的唤醒重新路由到新载体 */
  void onCarrierExited(final VirtualCarrier carrier) {
    final ActorCell cell = carrier.getCell();
    carriers.remove(cell, carrier);
    // 终态 cell 不复活（杂散唤醒无事可做, 只会空转建线程）
    if (!stopped
        && !cell.getPhase().isTerminal()
        && cell.getGate().state() == SchedulingGate.WAKING_UP) {
      route(cell);
    }
  }

  BlockingRunner getBlocking() {
    return blocking;
  }
}
