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

/** 可完成的 ActorFuture */
public final class CompletableActorFuture<T> implements ActorFuture<T> {

  private final CompletableFuture<T> delegate;

  public CompletableActorFuture() {
    this.delegate = new CompletableFuture<>();
  }

  public CompletableActorFuture(final CompletableFuture<T> delegate) {
    this.delegate = delegate;
  }

  public static <T> CompletableActorFuture<T> completed(final T value) {
    return new CompletableActorFuture<>(CompletableFuture.completedFuture(value));
  }

  public static <T> CompletableActorFuture<T> completedExceptionally(final Throwable error) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return new CompletableActorFuture<>(future);
  }

  public boolean complete(final T value) {
    return delegate.complete(value);
  }

  public boolean completeExceptionally(final Throwable error) {
    return delegate.completeExceptionally(error);
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return delegate;
  }
}
