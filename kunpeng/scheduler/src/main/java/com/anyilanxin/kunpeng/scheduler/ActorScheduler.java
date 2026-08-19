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
package com.anyilanxin.kunpeng.scheduler;

import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import com.anyilanxin.kunpeng.scheduler.core.SchedulerImpl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/** 调度器门面：builder 构建, start/submitActor/close */
public final class ActorScheduler implements ActorSchedulingService {

  private final SchedulerImpl impl;

  private ActorScheduler(final ActorSchedulerBuilder builder) {
    this.impl =
        new SchedulerImpl(
            builder.schedulerName,
            builder.cpuThreads,
            builder.ioThreads,
            builder.callbacksCapacity,
            builder.threadFactory,
            builder.clock,
            builder.meterRegistry);
  }

  public static ActorSchedulerBuilder newActorScheduler() {
    return new ActorSchedulerBuilder();
  }

  public static ActorScheduler newDefaultActorScheduler() {
    return newActorScheduler().build();
  }

  public void start() {
    impl.start();
  }

  @Override
  public ActorFuture<Void> submitActor(final Actor actor) {
    return submitActor(actor, SchedulingHints.CPU_BOUND);
  }

  @Override
  public ActorFuture<Void> submitActor(final Actor actor, final SchedulingHints schedulingHints) {
    return impl.submitActor(actor, schedulingHints);
  }

  @Override
  public String actorSchedulerName() {
    return impl.actorSchedulerName();
  }

  public Future<Void> stop() {
    impl.stop();
    return java.util.concurrent.CompletableFuture.completedFuture(null);
  }

  public void setBlockingTasksShutdownTime(final Duration shutdownTime) {
    impl.setBlockingTasksShutdownTime(shutdownTime);
  }

  /** 关闭 = stop */
  public void close() {
    impl.stop();
  }

  /** 构建器（默认: cpu=max(1,cores-2), io=2, callbacks=24576） */
  public static final class ActorSchedulerBuilder {

    private String schedulerName = "kp-scheduler";
    private int cpuThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private int ioThreads = 2;
    private int callbacksCapacity = 24_576;
    private ThreadFactory threadFactory = SchedulerImpl.defaultThreadFactory();
    private ActorClock clock;
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public ActorSchedulerBuilder setSchedulerName(final String schedulerName) {
      this.schedulerName = schedulerName;
      return this;
    }

    public ActorSchedulerBuilder setCpuBoundActorThreadCount(final int count) {
      this.cpuThreads = count;
      return this;
    }

    public ActorSchedulerBuilder setIoBoundActorThreadCount(final int count) {
      this.ioThreads = count;
      return this;
    }

    public ActorSchedulerBuilder setSubmittedCallbacksQueueSize(final int size) {
      this.callbacksCapacity = size;
      return this;
    }

    public ActorSchedulerBuilder setActorThreadFactory(final ThreadFactory threadFactory) {
      this.threadFactory = threadFactory;
      return this;
    }

    public ActorSchedulerBuilder setActorClock(final ActorClock clock) {
      this.clock = clock;
      return this;
    }

    public ActorSchedulerBuilder setMeterRegistry(
        final io.micrometer.core.instrument.MeterRegistry meterRegistry) {
      this.meterRegistry = meterRegistry;
      return this;
    }

    public int getCpuBoundActorThreadCount() {
      return cpuThreads;
    }

    public int getIoBoundActorThreadCount() {
      return ioThreads;
    }

    public ActorScheduler build() {
      return new ActorScheduler(this);
    }
  }
}
