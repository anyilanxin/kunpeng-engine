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
package com.anyilanxin.kunpeng.utils.scheduler;

import com.anyilanxin.kunpeng.utils.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单线程 Actor：全部变异串行于内部执行器；读操作任意线程。
 *
 * <p>最小实现，满足快照传输层的并发模型需求。
 */
public abstract class Actor implements AsyncClosable {

  protected final ScheduledExecutorService scheduler;
  protected String actorName = getClass().getSimpleName();
  private final AtomicBoolean closed = new AtomicBoolean();

  protected Actor() {
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              final Thread thread = new Thread(runnable, actorName);
              thread.setDaemon(true);
              return thread;
            });
  }

  /** Actor 名称（子类覆写或直接赋值 actorName） */
  public abstract String getName();

  /** 在 actor 线程上执行 */
  public void run(final Runnable action) {
    if (!closed.get()) {
      scheduler.execute(action);
    }
  }

  /** 在 actor 线程上延时执行 */
  public void schedule(final Duration delay, final Runnable action) {
    if (!closed.get()) {
      scheduler.schedule(action, delay.toNanos(), TimeUnit.NANOSECONDS);
    }
  }

  /** 启动回调（子类覆写；actor 线程上调用） */
  protected void onActorStarting() {}

  /** 关闭回调（子类覆写；actor 线程上调用） */
  protected void onActorClosing() {}

  @Override
  public ActorFuture<Void> closeAsync() {
    if (!closed.compareAndSet(false, true)) {
      return CompletableActorFuture.completed(null);
    }
    final var future = new CompletableActorFuture<Void>();
    scheduler.execute(
        () -> {
          try {
            onActorClosing();
          } finally {
            scheduler.shutdown();
            future.complete(null);
          }
        });
    return future;
  }
}
