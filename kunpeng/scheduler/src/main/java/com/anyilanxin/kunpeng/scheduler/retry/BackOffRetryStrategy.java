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

import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.Loggers;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;

/** 指数退避重试：1s 起步 ×2 封顶 maxBackOff; 异常也退避重试 */
public final class BackOffRetryStrategy implements RetryStrategy {

  private static final Logger LOG = Loggers.ACTOR_LOGGER;

  private final ActorControl actor;
  private final Duration maxBackOff;
  private final java.time.Duration initialBackOff = Duration.ofSeconds(1);

  public BackOffRetryStrategy(
      final com.anyilanxin.kunpeng.scheduler.Actor actor, final Duration maxBackOff) {
    this(actor.getControl(), maxBackOff);
  }

  public BackOffRetryStrategy(final ActorControl actor, final Duration maxBackOff) {
    this.actor = actor;
    this.maxBackOff = maxBackOff;
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(final OperationToRetry callable) {
    return runWithRetry(callable, () -> false);
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(
      final OperationToRetry callable, final BooleanSupplier terminateCondition) {
    final CompletableActorFuture<Boolean> result = new CompletableActorFuture<>();
    attempt(callable, terminateCondition, result, initialBackOff);
    return result;
  }

  private void attempt(
      final OperationToRetry callable,
      final BooleanSupplier terminateCondition,
      final CompletableActorFuture<Boolean> result,
      final Duration currentBackOff) {
    actor.run(
        () -> {
          boolean success;
          try {
            success = callable.run();
          } catch (final Exception e) {
            LOG.warn("Retry operation failed, backing off {}", currentBackOff, e);
            scheduleRetry(callable, terminateCondition, result, currentBackOff);
            return;
          }
          if (success) {
            result.complete(true);
          } else if (terminateCondition.getAsBoolean()) {
            result.complete(false);
          } else {
            scheduleRetry(callable, terminateCondition, result, currentBackOff);
          }
        });
  }

  private void scheduleRetry(
      final OperationToRetry callable,
      final BooleanSupplier terminateCondition,
      final CompletableActorFuture<Boolean> result,
      final Duration currentBackOff) {
    final Duration next =
        currentBackOff.multipliedBy(2).compareTo(maxBackOff) > 0
            ? maxBackOff
            : currentBackOff.multipliedBy(2);
    actor.schedule(next, () -> attempt(callable, terminateCondition, result, next));
  }
}
