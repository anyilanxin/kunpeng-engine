/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshotv2;

import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 测试用同步并发控制：任务即时执行、future 即时完成，供单线程确定性测试。 */
final class SynchronousConcurrencyControl implements ConcurrencyControl {

  @Override
  public <T> void runOnCompletion(final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    future.onComplete(callback);
  }

  @Override
  public <T> void runOnCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
    Throwable failure = null;
    for (final var future : futures) {
      future.join();
      if (future.isCompletedExceptionally() && failure == null) {
        failure = future.getException();
      }
    }
    callback.accept(failure);
  }

  @Override
  public ActorFuture<Void> run(final Runnable action) {
    action.run();
    return CompletableActorFuture.completed(null);
  }

  @Override
  public <T> ActorFuture<T> call(final Callable<T> callable) {
    try {
      return CompletableActorFuture.completed(callable.call());
    } catch (final Throwable t) {
      return CompletableActorFuture.completedExceptionally(t);
    }
  }

  @Override
  public ScheduledTimer schedule(final Duration delay, final Runnable runnable) {
    throw new UnsupportedOperationException("not used in tests");
  }
}
