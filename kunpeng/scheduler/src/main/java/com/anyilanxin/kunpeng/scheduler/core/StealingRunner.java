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

import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import com.anyilanxin.kunpeng.scheduler.clock.DefaultActorClock;
import java.util.concurrent.ThreadFactory;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.MDC;

/**
 * 平台线程载体：固定线程 + 偷取主循环。
 *
 * <p>主循环：跨线程回调排空 → 时钟步进 + 定时轮扫描 → 本队列取 cell（空则偷）→ 执行 （MDC 注入 actor 上下文）→ idle 退避。
 */
public final class StealingRunner implements CellRunner {

  private final RunnerPool pool;
  private final Thread thread;
  private final RunnerQueue queue = new RunnerQueue();
  private final CallbackQueue callbacks;
  private final TimerHub timers;
  private final BlockingRunner blocking;
  private final ActorClock clock;
  private final IdleStrategy idle = new BackoffIdleStrategy(100, 100, 1, 1_000_000L);
  private volatile boolean stopping;

  public StealingRunner(
      final RunnerPool pool,
      final String name,
      final ThreadFactory threadFactory,
      final int callbacksCapacity,
      final BlockingRunner blocking,
      final ActorClock sharedClock,
      final SchedulerMetrics metrics) {
    this.pool = pool;
    this.callbacks = new CallbackQueue(callbacksCapacity);
    this.timers = new TimerHub(this, metrics);
    this.blocking = blocking;
    this.clock = sharedClock != null ? sharedClock : new DefaultActorClock();
    this.thread = threadFactory.newThread(this::runBody);
    this.thread.setName(name);
  }

  public void start() {
    thread.start();
  }

  public RunnerQueue getQueue() {
    return queue;
  }

  void signalStop() {
    stopping = true;
    hint();
  }

  void join() {
    try {
      thread.join(30_000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void runBody() {
    final var context = CarrierContext.bind(clock);
    while (!stopping) {
      callbacks.drain();
      if (clock.update()) {
        timers.pollExpired(clock.getTimeMillis(), ActorCell::wakeSignal);
      }
      ActorCell cell = queue.poll();
      if (cell == null) {
        cell = steal();
      }
      if (cell != null) {
        execute(context, cell);
        idle.reset();
      } else {
        idle.idle();
      }
    }
    CarrierContext.unbind();
  }

  private ActorCell steal() {
    final RunnerPool victims = pool;
    final int size = victims.runnerCount();
    if (size <= 1) {
      return null;
    }
    int start = java.util.concurrent.ThreadLocalRandom.current().nextInt(size);
    for (int i = 0; i < size; i++) {
      final StealingRunner victim = victims.runnerAt((start + i) % size);
      if (victim != this) {
        final ActorCell cell = victim.queue.poll();
        if (cell != null) {
          return cell;
        }
      }
    }
    return null;
  }

  private void execute(final CarrierContext context, final ActorCell cell) {
    cell.claimedBy(this);
    context.setCurrentControl(cell.getControl());
    injectMdc(cell);
    try {
      cell.drain();
    } catch (final Throwable t) {
      // drain 内部已按相位处理; 此处兜底
      cell.failNow(t);
    } finally {
      context.setCurrentControl(null);
      clearMdc();
    }
  }

  private void injectMdc(final ActorCell cell) {
    final var context = cell.getContext();
    if (context != null && !context.isEmpty()) {
      context.forEach(MDC::put);
    }
  }

  private void clearMdc() {
    MDC.clear();
  }

  // ===== CellRunner =====

  @Override
  public TimerHub getTimers() {
    return timers;
  }

  @Override
  public BlockingRunner getBlocking() {
    return blocking;
  }

  @Override
  public CallbackQueue getCallbacks() {
    return callbacks;
  }

  @Override
  public boolean isOnOwnerThread() {
    return Thread.currentThread() == thread;
  }

  @Override
  public void hint() {
    idle.reset();
    java.util.concurrent.locks.LockSupport.unpark(thread);
  }

  @Override
  public String getName() {
    return thread.getName();
  }

  ActorClock getClock() {
    return clock;
  }
}
