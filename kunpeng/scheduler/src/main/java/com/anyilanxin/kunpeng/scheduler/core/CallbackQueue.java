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

import com.anyilanxin.kunpeng.scheduler.SchedulerLoggers;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * runner 的跨线程回调队列（定时器取消路由等）；主循环每轮排空。
 *
 * <p>无界：回调的消费端依赖 owner 线程约束（如 TimerHub 的 slots/wheel 非线程安全）, 有界队列满载时退化到调用方线程执行会破坏该约束——宁可不设上限, 以提交侧
 * hint 唤醒载体及时排空。
 */
public final class CallbackQueue {

  private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
  private final Runnable wakeHint;

  public CallbackQueue(final Runnable wakeHint) {
    this.wakeHint = wakeHint;
  }

  public void submit(final Runnable callback) {
    queue.offer(callback);
    wakeHint.run();
  }

  public void drain() {
    Runnable callback;
    while ((callback = queue.poll()) != null) {
      try {
        callback.run();
      } catch (final RuntimeException e) {
        // 回调异常不阻断排空, 但不能静默——否则定时器取消等路由失败无从排查
        SchedulerLoggers.SCHEDULER_LOGGER.error("跨线程回调执行失败", e);
      }
    }
  }
}
