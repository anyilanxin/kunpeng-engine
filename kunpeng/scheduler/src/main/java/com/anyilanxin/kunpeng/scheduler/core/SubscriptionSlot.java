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

/** 订阅槽：条件/定时器/future 完成的可轮询载体（owner 线程轮询, 到期执行绑定的动作） */
public abstract class SubscriptionSlot {

  protected final Runnable action;
  protected boolean cancelled;

  protected SubscriptionSlot(final Runnable action) {
    this.action = action;
  }

  /**
   * @return 到期则消费并返回 true
   */
  public abstract boolean pollDue();

  /**
   * @return 到期与否（不消费; 休眠前复查用）
   */
  public abstract boolean peekDue();

  public Runnable getAction() {
    return action;
  }

  public void cancel() {
    cancelled = true;
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
