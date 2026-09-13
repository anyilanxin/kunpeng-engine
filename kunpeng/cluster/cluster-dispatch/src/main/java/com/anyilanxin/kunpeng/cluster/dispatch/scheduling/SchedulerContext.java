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

/**
 * 交付给流处理器的只读调度上下文。
 *
 * <p>暴露有序的 {@link TimerScheduler}、处理器所属的分区，以及用于解析截止时间的 {@link TimerClock}。本上下文始终启用异步调度。
 */
public final class SchedulerContext {

  private final TimerScheduler scheduler;
  private final int partitionId;
  private final TimerClock clock;

  public SchedulerContext(
      final TimerScheduler scheduler, final int partitionId, final TimerClock clock) {
    this.scheduler = scheduler;
    this.partitionId = partitionId;
    this.clock = clock;
  }

  /** 返回本上下文的有序定时调度器。 */
  public TimerScheduler scheduler() {
    return scheduler;
  }

  /** 返回本上下文调度所属的分区 id。 */
  public int partitionId() {
    return partitionId;
  }

  /** 返回 true：本上下文已启用异步调度。 */
  public boolean asyncEnabled() {
    return true;
  }

  /** 返回用于解析调度截止时间的时钟。 */
  public TimerClock clock() {
    return clock;
  }
}
