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
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.MDC;

/** 虚拟线程载体：每 cell 绑定一条虚拟线程, 无偷取。gate WAKING_UP 时接管执行; cell 终态（CLOSED/FAILED）且休眠后退出线程。 */
public final class VirtualCarrier implements CellRunner, Runnable {

  private static final long PARK_NANOS = 1_000_000L; // 1ms

  private final VirtualPool pool;
  private final ActorCell cell;
  private final CallbackQueue callbacks = new CallbackQueue(1024);
  private final TimerHub timers;
  private final ActorClock clock = new DefaultActorClock();
  private volatile Thread thread;
  private volatile boolean stopping;
  private final java.util.concurrent.atomic.AtomicBoolean started =
      new java.util.concurrent.atomic.AtomicBoolean();

  public VirtualCarrier(final VirtualPool pool, final ActorCell cell) {
    this.pool = pool;
    this.cell = cell;
    this.timers = new TimerHub(this, pool.getMetrics());
  }

  public void start() {
    if (started.compareAndSet(false, true)) {
      thread = Thread.ofVirtual().name("kp-vt-actor-" + cell.getName()).start(this);
    }
  }

  void signalStop() {
    stopping = true;
    hint();
  }

  @Override
  public void run() {
    final var context = CarrierContext.bind(clock);
    while (!stopping) {
      callbacks.drain();
      if (clock.update()) {
        timers.pollExpired(clock.getTimeMillis(), ActorCell::wakeSignal);
      }
      final int gate = cell.getGate().state();
      if (gate == SchedulingGate.WAKING_UP) {
        cell.claimedBy(this);
        context.setCurrentControl(cell.getControl());
        injectMdc();
        try {
          cell.drain();
        } catch (final Throwable t) {
          cell.failNow(t);
        } finally {
          context.setCurrentControl(null);
          MDC.clear();
        }
        final Phases phase = cell.getPhase();
        if (phase.isTerminal() && cell.getGate().state() == SchedulingGate.WAITING) {
          break; // CLOSED/FAILED 且已休眠 → 退线程
        }
        continue;
      }
      LockSupport.parkNanos(PARK_NANOS);
    }
    CarrierContext.unbind();
  }

  private void injectMdc() {
    final Map<String, String> ctx = cell.getContext();
    if (ctx != null && !ctx.isEmpty()) {
      ctx.forEach(MDC::put);
    }
  }

  // ===== CellRunner =====

  @Override
  public TimerHub getTimers() {
    return timers;
  }

  @Override
  public BlockingRunner getBlocking() {
    return pool.getBlocking();
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
    final Thread t = thread;
    if (t != null) {
      LockSupport.unpark(t);
    }
  }

  @Override
  public String getName() {
    return "vt-" + cell.getName();
  }
}
