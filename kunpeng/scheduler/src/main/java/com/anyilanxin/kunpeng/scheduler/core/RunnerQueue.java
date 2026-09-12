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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * runner 任务队列：MC 添加（任意提交线程）+ MC 消费（owner 优先 + 空闲 thief 偷取）。 物理移除；gate 保证同一 cell 至多一个排队实例, 无需 claim
 * 竞争。
 */
public final class RunnerQueue {

  private static final int SOFT_CAP = 4096;

  private final Queue<ActorCell> queue = new ConcurrentLinkedQueue<>();
  private final AtomicLong size = new AtomicLong();

  /**
   * 未饱和才入队（路由偏好用）。
   *
   * <p>check-then-act 下并发提交可能使计数短暂超限——软上限只是路由提示, 不承载正确性。
   *
   * @return false 表示软上限饱和, 调用方应换队列
   */
  public boolean offerIfNotSaturated(final ActorCell cell) {
    if (size.get() >= SOFT_CAP) {
      return false;
    }
    size.incrementAndGet();
    queue.offer(cell);
    return true;
  }

  /** 无条件入队（全池饱和时的兜底; 丢唤醒会让 cell 永久卡死 WAKING_UP, 宁可突破软上限） */
  public void offer(final ActorCell cell) {
    size.incrementAndGet();
    queue.offer(cell);
  }

  public ActorCell poll() {
    final ActorCell cell = queue.poll();
    if (cell != null) {
      size.decrementAndGet();
    }
    return cell;
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public long size() {
    return size.get();
  }
}
