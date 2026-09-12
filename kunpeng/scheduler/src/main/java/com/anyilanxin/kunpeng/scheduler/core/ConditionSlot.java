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

import java.util.concurrent.atomic.AtomicInteger;

/** 条件订阅：signal 计数合并, 轮询消费（多次未消费 signal 合并为一轮执行） */
public final class ConditionSlot extends SubscriptionSlot {

  private final AtomicInteger signalCount = new AtomicInteger();
  private volatile int runCount;

  public ConditionSlot(final Runnable action) {
    super(action);
  }

  public void signal() {
    signalCount.incrementAndGet();
  }

  @Override
  public boolean peekDue() {
    return !cancelled && signalCount.get() > runCount;
  }

  @Override
  public boolean pollDue() {
    if (cancelled) {
      return false;
    }
    final int pending = signalCount.get();
    if (pending > runCount) {
      runCount = pending;
      return true;
    }
    return false;
  }
}
