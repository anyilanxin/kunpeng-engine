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

/**
 * 定时器订阅：到期由 TimerHub 置 pending 并唤醒 cell；recurring 到期即重排下一轮。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TimerSlot extends SubscriptionSlot {

  private volatile boolean pending;
  // 截止时间、入堆序号与堆内下标只在 owner runner 线程上读写
  private long deadlineMillis;
  private long sequence;
  private int heapIndex = -1;
  private volatile boolean recurring;
  private volatile long periodMillis;
  private volatile ActorCell cell;

  public TimerSlot(final Runnable action) {
    super(action);
  }

  public long getDeadlineMillis() {
    return deadlineMillis;
  }

  /** 重设截止时间（recurring 到期重排下一轮时用），并按新的入堆序号排在同截止时间的后来者之后 */
  public void setDeadlineMillis(final long deadlineMillis, final long sequence) {
    this.deadlineMillis = deadlineMillis;
    this.sequence = sequence;
  }

  public long getSequence() {
    return sequence;
  }

  /** 在 {@link TimerQueue} 中的下标；不在堆中为 -1 */
  public int getHeapIndex() {
    return heapIndex;
  }

  public void setHeapIndex(final int heapIndex) {
    this.heapIndex = heapIndex;
  }

  public boolean isRecurring() {
    return recurring;
  }

  public void setRecurring(final boolean recurring) {
    this.recurring = recurring;
  }

  public long getPeriodMillis() {
    return periodMillis;
  }

  public void setPeriodMillis(final long periodMillis) {
    this.periodMillis = periodMillis;
  }

  public ActorCell getCell() {
    return cell;
  }

  public void setCell(final ActorCell cell) {
    this.cell = cell;
  }

  public void fire() {
    pending = true;
  }

  @Override
  public boolean peekDue() {
    return !cancelled && pending;
  }

  @Override
  public boolean pollDue() {
    if (cancelled) {
      return false;
    }
    if (pending) {
      pending = false;
      return true;
    }
    return false;
  }
}
