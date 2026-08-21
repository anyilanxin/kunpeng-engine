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
package com.anyilanxin.kunpeng.utils.scheduler.future;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** 异步 Future（包装 CompletableFuture 的 Actor 风格接口） */
public interface ActorFuture<T> {

  CompletableFuture<T> toCompletableFuture();

  /** 完成时回调（成功/失败） */
  default ActorFuture<T> onComplete(final BiConsumer<T, Throwable> callback) {
    toCompletableFuture().whenComplete(callback);
    return this;
  }

  /** 成功时映射 */
  default <U> ActorFuture<U> andThen(final Function<T, ActorFuture<U>> mapper) {
    final CompletableFuture<U> out = new CompletableFuture<>();
    toCompletableFuture()
        .thenCompose(
            value -> {
              try {
                return mapper.apply(value).toCompletableFuture();
              } catch (final Exception e) {
                final var failed = new CompletableFuture<U>();
                failed.completeExceptionally(e);
                return failed;
              }
            })
        .whenComplete(
            (v, e) -> {
              if (e != null) {
                out.completeExceptionally(e);
              } else {
                out.complete(v);
              }
            });
    return new CompletableActorFuture<>(out);
  }

  /** 失败时回调 */
  default ActorFuture<T> onError(final Consumer<Throwable> handler) {
    toCompletableFuture()
        .exceptionally(
            error -> {
              handler.accept(error);
              return null;
            });
    return this;
  }

  /** 阻塞等待结果 */
  default T join() {
    return toCompletableFuture().join();
  }
}
