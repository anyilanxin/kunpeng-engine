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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.MDC;

/** 虚拟线程载体：每 cell 绑定一条虚拟线程, 无偷取。gate WAKING_UP 时接管执行; cell 终态（CLOSED/FAILED）且休眠后退出线程。 */
public final class VirtualCarrier implements CellRunner, Runnable {

  private final VirtualPool pool;
  private final ActorCell cell;
  private final CallbackQueue callbacks;
  private final TimerHub timers;
  private final ActorClock clock = new DefaultActorClock();
  private volatile Thread thread;
  private volatile boolean stopping;
  private final AtomicBoolean started = new AtomicBoolean();

  public VirtualCarrier(final VirtualPool pool, final ActorCell cell) {
    this.pool = pool;
    this.cell = cell;
    this.callbacks = new CallbackQueue(this::hint);
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
      if (gate == SchedulingGate.WAKING_UP && cell.claimedBy(this)) {
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
      parkUntilNextEvent();
    }
    CarrierContext.unbind();
    // 退出即从池中移除, 否则 carriers 随历史 actor 无界增长, 且死载体无法重启;
    // 移除后再复查调度门: 若此刻 gate 已被置 WAKING_UP（唤醒路由到了本垂死载体）,
    // 该唤醒会随线程消亡而丢失——须重新路由让池创建新载体接手
    pool.onCarrierExited(this);
  }

  /**
   * 无工作: park 至最早定时器到期, 无定时器则无限期——所有事件源（gate 路由、跨线程回调、关停） 都经 hint() unpark 唤醒。取代旧的 1ms 轮询（万个空闲虚拟
   * actor 会造成每秒千万次唤醒风暴）。
   */
  private void parkUntilNextEvent() {
    final long nextDeadline = timers.nextDeadlineMillis();
    if (nextDeadline == Long.MAX_VALUE) {
      LockSupport.parkNanos(Long.MAX_VALUE);
      return;
    }
    final long waitMillis = nextDeadline - clock.getTimeMillis();
    if (waitMillis > 0) {
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitMillis));
    }
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

  ActorCell getCell() {
    return cell;
  }
}
