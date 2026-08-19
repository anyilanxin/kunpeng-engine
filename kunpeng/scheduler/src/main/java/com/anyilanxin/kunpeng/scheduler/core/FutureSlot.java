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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.function.BiConsumer;

/** future 完成订阅：future.block(cell) 完成时唤醒, 轮询到 isDone 执行消费 */
public final class FutureSlot extends SubscriptionSlot {

  private final ActorFuture<?> future;
  private final BiConsumer<Object, Throwable> consumer;
  private volatile boolean done;
  private volatile boolean consumed;

  public FutureSlot(final ActorFuture<?> future, final BiConsumer<Object, Throwable> consumer) {
    super(null);
    this.future = future;
    this.consumer = consumer;
  }

  public ActorFuture<?> getFuture() {
    return future;
  }

  public void markDone() {
    done = true;
  }

  /** 未消费（消费者在 cell 订阅列表中, 尚未轮询执行） */
  public boolean isConsumed() {
    return consumed;
  }

  /** 关闭清理兜底: 订阅列表将被清空, 未消费的续接永远不会再被轮询—— 必须异常完成消费方, 否则上游 future 完成静默丢失（外层链永挂且无任何日志）。 */
  public void failConsumer(final Throwable error) {
    if (cancelled || consumed) {
      return;
    }
    consumed = true;
    consumer.accept(null, error);
  }

  @Override
  public boolean peekDue() {
    return !cancelled && !consumed && (done || future.isDone());
  }

  @Override
  public boolean pollDue() {
    if (cancelled || consumed) {
      return false;
    }
    if (done || future.isDone()) {
      consumed = true;
      return true;
    }
    return false;
  }

  public void runConsumer() {
    if (future.isCompletedExceptionally()) {
      consumer.accept(null, future.getException());
    } else {
      Object value = null;
      try {
        value = future.get();
      } catch (final Exception ignored) {
        // isDone 后 get 不会阻塞
      }
      consumer.accept(value, null);
    }
  }
}
