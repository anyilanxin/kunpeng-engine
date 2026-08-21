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
package com.anyilanxin.kunpeng.cluster.utils;

import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A {@link ConcurrencyControl} which executes everything immediately on the calling thread, for
 * testing purposes.
 */
public final class TestConcurrencyControl implements ConcurrencyControl {

  @Override
  public <T> void runOnCompletion(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    if (future.isDone()) {
      completeCallback(future, callback);
      return;
    }
    final var thread = new Thread(() -> completeCallback(future, callback), "test-callback");
    thread.setDaemon(true);
    thread.start();
  }

  private static <T> void completeCallback(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    try {
      callback.accept(future.get(), null);
    } catch (final Exception e) {
      final Throwable cause = e.getCause() != null ? e.getCause() : e;
      callback.accept(null, cause);
    }
  }

  @Override
  public <T> void runOnCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
    if (futures.isEmpty()) {
      callback.accept(null);
      return;
    }
    final var remaining = new java.util.concurrent.atomic.AtomicInteger(futures.size());
    final var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    for (final var future : futures) {
      runOnCompletion(
          future,
          (v, t) -> {
            if (t != null) {
              error.compareAndSet(null, t);
            }
            if (remaining.decrementAndGet() == 0) {
              callback.accept(error.get());
            }
          });
    }
  }

  @Override
  public void run(final Runnable action) {
    action.run();
  }

  @Override
  public <T> ActorFuture<T> call(final Callable<T> callable) {
    final CompletableActorFuture<T> future = new CompletableActorFuture<>();
    try {
      future.complete(callable.call());
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public ScheduledTimer schedule(final Duration delay, final Runnable runnable) {
    final java.util.concurrent.ScheduledFuture<?> scheduled =
        SCHEDULER.schedule(runnable, delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    return new ScheduledTimer() {
      private volatile boolean cancelled;

      @Override
      public void cancel() {
        cancelled = true;
        scheduled.cancel(false);
      }

      boolean isCancelled() {
        return cancelled;
      }
    };
  }

  private static final java.util.concurrent.ScheduledExecutorService SCHEDULER =
      java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            final var thread = new Thread(runnable, "test-concurrency-control-scheduler");
            thread.setDaemon(true);
            return thread;
          });
}
