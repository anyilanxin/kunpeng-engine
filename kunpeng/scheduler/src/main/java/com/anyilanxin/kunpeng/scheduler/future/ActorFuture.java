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
package com.anyilanxin.kunpeng.scheduler.future;

import com.anyilanxin.kunpeng.scheduler.core.ActorCell;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * actor future：完成回调可回投 actor 上下文；actor 线程内未完成 get() 抛异常（非阻塞纪律）。
 *
 * @param <V> 结果类型
 */
public interface ActorFuture<V> extends Future<V>, BiConsumer<V, Throwable> {

  void complete(V value);

  void completeExceptionally(String failure, Throwable throwable);

  void completeExceptionally(Throwable throwable);

  void onComplete(BiConsumer<V, Throwable> consumer);

  void onComplete(BiConsumer<V, Throwable> consumer, Executor executor);

  @Override
  default void accept(final V value, final Throwable throwable) {
    if (throwable != null) {
      completeExceptionally(throwable);
    } else {
      complete(value);
    }
  }

  /** 非阻塞语义的 get：checked 转 unchecked 重抛 */
  V join();

  V join(long timeout, java.util.concurrent.TimeUnit timeUnit);

  /** 仅供调度器使用：完成时唤醒目标 cell */
  void block(ActorCell onCompletion);

  boolean isCompletedExceptionally();

  Throwable getException();

  <U> ActorFuture<U> andThen(Supplier<ActorFuture<U>> next, Executor executor);

  <U> ActorFuture<U> andThen(Function<V, ActorFuture<U>> next, Executor executor);

  <U> ActorFuture<U> andThen(BiFunction<V, Throwable, ActorFuture<U>> next, Executor executor);

  <U> ActorFuture<U> thenApply(Function<V, U> next, Executor executor);

  <U> ActorFuture<U> thenApply(final Function<V, U> next);

  default void onSuccess(final java.util.function.Consumer<V> handler) {
    onComplete(
        (v, error) -> {
          if (error == null) {
            handler.accept(v);
          }
        });
  }

  default void onSuccess(final java.util.function.Consumer<V> handler, final Executor executor) {
    onComplete(
        (v, error) -> {
          if (error == null) {
            handler.accept(v);
          }
        },
        executor);
  }

  default void onError(final java.util.function.Consumer<Throwable> handler) {
    onComplete(
        (v, error) -> {
          if (error != null) {
            handler.accept(error);
          }
        });
  }

  default void onError(
      final java.util.function.Consumer<Throwable> handler, final Executor executor) {
    onComplete(
        (v, error) -> {
          if (error != null) {
            handler.accept(error);
          }
        },
        executor);
  }

  /** JDK 桥接：回调在完成该 future 的线程执行 */
  default java.util.concurrent.CompletableFuture<V> toCompletableFuture() {
    final var result = new java.util.concurrent.CompletableFuture<V>();
    onComplete(
        (v, error) -> {
          if (error != null) {
            result.completeExceptionally(error);
          } else {
            result.complete(v);
          }
        },
        Runnable::run);
    return result;
  }
}
