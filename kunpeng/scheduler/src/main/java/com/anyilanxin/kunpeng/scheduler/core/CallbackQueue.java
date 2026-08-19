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

import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;

/** runner 的跨线程回调队列（定时器取消路由等）；主循环每轮排空 */
public final class CallbackQueue {

  private final ManyToManyConcurrentArrayQueue<Runnable> queue;

  public CallbackQueue(final int capacity) {
    this.queue = new ManyToManyConcurrentArrayQueue<>(capacity);
  }

  public boolean submit(final Runnable callback) {
    if (!queue.offer(callback)) {
      // 有界满: 退化为直接执行（调用方线程）, 保证不丢
      callback.run();
      return false;
    }
    return true;
  }

  public void drain() {
    Runnable callback;
    while ((callback = queue.poll()) != null) {
      try {
        callback.run();
      } catch (final RuntimeException e) {
        // 回调异常不阻断排空
      }
    }
  }
}
