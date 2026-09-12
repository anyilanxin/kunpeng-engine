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
package com.anyilanxin.kunpeng.scheduler.retry;

import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * 可中止重试：callable 返回 false 且终止条件真 → complete(false); 任何异常 → 异常完成。
 *
 * <p>返回 false 时不立即重入 actor 邮箱，而是按指数退避（10ms 起步、×2、封顶 500ms）调度 下一次尝试，避免下游持续背压时以忙旋占满属主 actor，饿死同 actor
 * 上的其他任务。
 */
public final class AbortableRetryStrategy implements RetryStrategy {

  private static final Duration INITIAL_BACKOFF = Duration.ofMillis(10);
  private static final Duration MAX_BACKOFF = Duration.ofMillis(500);

  private final ActorControl actor;

  public AbortableRetryStrategy(final Actor actor) {
    this(actor.getControl());
  }

  public AbortableRetryStrategy(final ActorControl actor) {
    this.actor = actor;
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(final OperationToRetry callable) {
    return runWithRetry(callable, () -> false);
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(
      final OperationToRetry callable, final BooleanSupplier terminateCondition) {
    final CompletableActorFuture<Boolean> result = new CompletableActorFuture<>();
    attempt(callable, terminateCondition, result, INITIAL_BACKOFF);
    return result;
  }

  private void attempt(
      final OperationToRetry callable,
      final BooleanSupplier terminateCondition,
      final CompletableActorFuture<Boolean> result,
      final Duration backOff) {
    runGuarded(
        () -> {
          boolean success;
          try {
            success = callable.run();
          } catch (final Exception e) {
            result.completeExceptionally(e);
            return;
          }
          if (success) {
            result.complete(true);
          } else if (terminateCondition.getAsBoolean()) {
            result.complete(false);
          } else {
            scheduleRetry(callable, terminateCondition, result, backOff);
          }
        },
        result);
  }

  private void scheduleRetry(
      final OperationToRetry callable,
      final BooleanSupplier terminateCondition,
      final CompletableActorFuture<Boolean> result,
      final Duration backOff) {
    final Duration next = nextBackOff(backOff);
    if (actor.isClosed()) {
      completeIfPending(result, new IllegalStateException("Actor 已关闭，重试中止"));
      return;
    }
    actor.schedule(next, () -> attempt(callable, terminateCondition, result, next));
  }

  private static Duration nextBackOff(final Duration current) {
    final Duration doubled = current.multipliedBy(2);
    return doubled.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : doubled;
  }

  /** 提交被拒（actor 关闭/失败）或动作内逃逸 Error 使信封异常完成时转嫁给 result, 防止调用方在 result 上 join 永久阻塞。 */
  private void runGuarded(final Runnable action, final CompletableActorFuture<Boolean> result) {
    actor
        .run(action)
        .onComplete(
            (v, e) -> {
              if (e != null && !result.isDone()) {
                result.completeExceptionally(e);
              }
            },
            Runnable::run);
  }

  private static void completeIfPending(
      final CompletableActorFuture<Boolean> result, final Throwable error) {
    if (!result.isDone()) {
      result.completeExceptionally(error);
    }
  }
}
