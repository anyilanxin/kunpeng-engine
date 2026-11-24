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

/**
 * runner 私有定时器集合：{@link TimerQueue}（按截止时间排序的带索引最小堆）。 到期置 pending 并唤醒 cell；recurring
 * 到期即重排下一轮（fixed-rate 近似）。 取消跨线程路由到 owner runner 的回调队列。
 *
 * <p>同刻度定时器的入堆、触发、取消都是 O(log n)：大量定时器同一时刻到期时入堆总代价随数量线性增长， 最早到期时间直接看堆顶。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TimerHub {

  private final TimerQueue queue = new TimerQueue();
  private final CellRunner owner;
  private final SchedulerMetrics metrics;

  /** 入堆序号：同截止时间按入堆先后排序（先入先出） */
  private long sequence;

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
      cancelTimer(slot);
    }
  }

  /** 供 runner 主循环调用（owner 线程） */
  public TimerSlot schedule(
      final ActorCell cell,
      final long deadlineMillis,
      final Runnable action,
      final boolean recurring,
      final long periodMillis) {
    if (recurring && periodMillis <= 0) {
      // 周期非正的 recurring 每轮扫描都会重新到期，等于忙旋
      throw new IllegalArgumentException(
          "Recurring timer period must be positive, got " + periodMillis);
    }
    metrics.incTimerScheduled();
    final TimerSlot slot = new TimerSlot(action);
    slot.setRecurring(recurring);
    slot.setPeriodMillis(periodMillis);
    slot.setCell(cell);
    slot.setDeadlineMillis(deadlineMillis, sequence++);
    queue.offer(slot);
    cell.addSubscription(slot);
    return slot;
  }

  public ScheduledTimer handle(final TimerSlot slot) {
    return new TimerHandle(slot);
  }

  /**
   * 到期扫描（owner 线程; clock.update 后调用）：把截止时间不晚于 {@code nowMillis} 的定时器全部触发。
   * 时钟一次前进多毫秒时，期间到期的也一并触发，不会只看到其中一部分。
   */
  public void pollExpired(final long nowMillis, final ActorCellWaker waker) {
    while (!queue.isEmpty() && queue.peek().getDeadlineMillis() <= nowMillis) {
      final TimerSlot slot = queue.poll();
      if (slot.isCancelled()) {
        continue;
      }
      if (slot.isRecurring()) {
        // 到期即重排下一轮（槽位与句柄保持同一身份）
        slot.setDeadlineMillis(nowMillis + slot.getPeriodMillis(), sequence++);
        queue.offer(slot);
      }
      slot.fire();
      metrics.incTimerFired();
      waker.wakeup(slot.getCell());
    }
  }

  /** 最早到期 deadline（ms; 无定时器 Long.MAX_VALUE）。owner 线程调用, 供载体计算 park 时长 */
  public long nextDeadlineMillis() {
    final TimerSlot head = queue.peek();
    return head == null ? Long.MAX_VALUE : head.getDeadlineMillis();
  }

  /** 取消：owner 直杀, 否则路由回调队列（回调执行前若到期，扫描按已取消标志跳过） */
  public void cancelTimer(final TimerSlot slot) {
    if (owner != null) {
      metrics.incTimerCancelled();
      if (owner.isOnOwnerThread()) {
        queue.remove(slot);
      } else {
        owner.getCallbacks().submit(() -> queue.remove(slot));
      }
    }
  }

  /** 到期时唤醒目标 cell 的回调 */
  public interface ActorCellWaker {
    void wakeup(ActorCell cell);
  }
}
