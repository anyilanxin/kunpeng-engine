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

/** 定时器订阅：到期由 TimerHub 置 pending 并唤醒 cell；recurring 到期即重排下一轮 */
public final class TimerSlot extends SubscriptionSlot {

  private volatile boolean pending;
  private volatile long timerId;
  private volatile boolean recurring;
  private volatile long periodMillis;
  private volatile ActorCell cell;

  public TimerSlot(final long timerId, final Runnable action) {
    super(action);
    this.timerId = timerId;
  }

  public long getTimerId() {
    return timerId;
  }

  public void setTimerId(final long timerId) {
    this.timerId = timerId;
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
