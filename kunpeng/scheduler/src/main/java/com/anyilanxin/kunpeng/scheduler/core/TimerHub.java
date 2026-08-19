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

import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import java.util.concurrent.TimeUnit;
import org.agrona.DeadlineTimerWheel;
import org.agrona.collections.Long2ObjectHashMap;

/**
 * runner 私有定时轮：Agrona DeadlineTimerWheel(1ms/32 ticks) + 槽位表。 到期置 pending 并唤醒 cell；recurring
 * 到期即重排下一轮（fixed-rate 近似）。 取消跨线程路由到 owner runner 的回调队列。
 */
public final class TimerHub {

  private static final int MAX_EXPIRY_PER_ROUND = 1000;

  private final DeadlineTimerWheel wheel =
      new DeadlineTimerWheel(TimeUnit.MILLISECONDS, System.currentTimeMillis(), 1, 32);
  private final Long2ObjectHashMap<TimerSlot> slots = new Long2ObjectHashMap<>();
  private final CellRunner owner;
  private final SchedulerMetrics metrics;

  public TimerHub(final CellRunner owner, final SchedulerMetrics metrics) {
    this.owner = owner;
    this.metrics = metrics == null ? SchedulerMetrics.noop() : metrics;
  }

  /** 定时器句柄：取消路由到 owner */
  public final class TimerHandle implements ScheduledTimer {
    private final TimerSlot slot;

    private TimerHandle(final TimerSlot slot) {
      this.slot = slot;
    }

    @Override
    public void cancel() {
      slot.cancel();
      cancelTimer(slot.getTimerId());
    }
  }

  /** 供 runner 主循环调用（owner 线程） */
  public TimerSlot schedule(
      final ActorCell cell,
      final long deadlineMillis,
      final Runnable action,
      final boolean recurring,
      final long periodMillis) {
    final long timerId = wheel.scheduleTimer(deadlineMillis);
    metrics.incTimerScheduled();
    final TimerSlot slot = new TimerSlot(timerId, action);
    slot.setRecurring(recurring);
    slot.setPeriodMillis(periodMillis);
    slot.setCell(cell);
    slots.put(timerId, slot);
    cell.addSubscription(slot);
    return slot;
  }

  public ScheduledTimer handle(final TimerSlot slot) {
    return new TimerHandle(slot);
  }

  /** 到期扫描（owner 线程; clock.update 后调用） */
  public void pollExpired(final long nowMillis, final ActorCellWaker waker) {
    wheel.poll(
        nowMillis,
        (timeUnit, timeNow, timerId) -> {
          final TimerSlot slot = slots.remove(timerId);
          if (slot == null || slot.isCancelled()) {
            return true;
          }
          if (slot.isRecurring()) {
            // 到期即重排下一轮（槽位与句柄保持同一身份）
            final long nextId = wheel.scheduleTimer(timeNow + slot.getPeriodMillis());
            slot.setTimerId(nextId);
            slots.put(nextId, slot);
          }
          slot.fire();
          metrics.incTimerFired();
          waker.wakeup(slot.getCell());
          return true;
        },
        MAX_EXPIRY_PER_ROUND);
  }

  /** 取消：owner 直杀, 否则路由回调队列 */
  public void cancelTimer(final long timerId) {
    if (owner != null) {
      metrics.incTimerCancelled();
      if (owner.isOnOwnerThread()) {
        slots.remove(timerId);
        wheel.cancelTimer(timerId);
      } else {
        owner
            .getCallbacks()
            .submit(
                () -> {
                  slots.remove(timerId);
                  wheel.cancelTimer(timerId);
                });
      }
    }
  }

  /** 到期时唤醒目标 cell 的回调 */
  public interface ActorCellWaker {
    void wakeup(ActorCell cell);
  }
}
