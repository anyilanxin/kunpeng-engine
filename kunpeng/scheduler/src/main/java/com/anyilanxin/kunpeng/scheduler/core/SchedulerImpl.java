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

import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.SchedulingHints;
import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** 调度器实现：三组载体（CPU/IO/虚拟）+ 阻塞外包池聚合 */
public final class SchedulerImpl {

  private final String schedulerName;
  private final RunnerPool cpuPool;
  private final RunnerPool ioPool;
  private final VirtualPool virtualPool;
  private final BlockingRunner blockingRunner;
  private final ThreadFactory threadFactory;
  private final SchedulerMetrics metrics;
  private volatile Duration blockingShutdownTime = Duration.ofSeconds(300);

  public SchedulerImpl(
      final String schedulerName,
      final int cpuThreads,
      final int ioThreads,
      final int callbacksCapacity,
      final ThreadFactory threadFactory,
      final ActorClock sharedClock,
      final io.micrometer.core.instrument.MeterRegistry meterRegistry) {
    this.schedulerName = schedulerName;
    this.threadFactory = threadFactory;
    this.metrics = new SchedulerMetrics(meterRegistry, schedulerName);
    this.blockingRunner = new BlockingRunner(schedulerName, metrics);
    this.cpuPool =
        new RunnerPool(
            cpuThreads,
            schedulerName + "-cpu-scheduler",
            threadFactory,
            callbacksCapacity,
            blockingRunner,
            sharedClock,
            metrics);
    this.ioPool =
        new RunnerPool(
            ioThreads,
            schedulerName + "-io-scheduler",
            threadFactory,
            callbacksCapacity,
            blockingRunner,
            sharedClock,
            metrics);
    this.virtualPool = new VirtualPool(blockingRunner, metrics);
  }

  public void start() {
    cpuPool.start();
    ioPool.start();
  }

  public ActorFuture<Void> submitActor(final Actor actor, final SchedulingHints hints) {
    final ActorControl control = actor.getControl();
    final ActorCell cell = control.getCell();
    metrics.incActorSubmitted();
    switch (hints) {
      case IO_BOUND -> {
        cell.onScheduled(ioPool, metrics);
        ioPool.route(cell);
      }
      case VIRTUAL_THREAD -> virtualPool.submitTask(cell);
      default -> {
        cell.onScheduled(cpuPool, metrics);
        cpuPool.route(cell);
      }
    }
    return cell.getStartingFuture();
  }

  public void stop() {
    cpuPool.stop();
    ioPool.stop();
    virtualPool.stop();
    blockingRunner.shutdown(blockingShutdownTime);
  }

  public void setBlockingTasksShutdownTime(final Duration shutdownTime) {
    this.blockingShutdownTime = shutdownTime;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  /** Atomix 集成用: 与旧语义一致返回调度器名本身 */
  public String actorSchedulerName() {
    return schedulerName;
  }

  /** 默认线程工厂 */
  public static ThreadFactory defaultThreadFactory() {
    final var counter = new AtomicInteger();
    return r -> {
      final Thread thread = new Thread(r);
      thread.setDaemon(false);
      counter.incrementAndGet();
      return thread;
    };
  }
}
