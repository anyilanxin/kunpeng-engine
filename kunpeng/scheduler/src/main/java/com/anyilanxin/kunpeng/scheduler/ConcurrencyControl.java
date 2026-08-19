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
package com.anyilanxin.kunpeng.scheduler;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/** 并发控制：提交/调用/续接/定时（Actor 与其他持有者共用语义） */
public interface ConcurrencyControl extends Executor {

  <T> void runOnCompletion(final ActorFuture<T> future, final BiConsumer<T, Throwable> callback);

  <T> void runOnCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback);

  void run(final Runnable action);

  <T> ActorFuture<T> call(final Callable<T> callable);

  /** 延迟调度 */
  ScheduledTimer schedule(final Duration delay, final Runnable runnable);

  default <V> ActorFuture<V> createFuture() {
    return new CompletableActorFuture<>();
  }

  default <V> ActorFuture<V> createCompletedFuture() {
    return CompletableActorFuture.completed(null);
  }

  @Override
  default void execute(@NotNull final Runnable command) {
    run(command);
  }
}
