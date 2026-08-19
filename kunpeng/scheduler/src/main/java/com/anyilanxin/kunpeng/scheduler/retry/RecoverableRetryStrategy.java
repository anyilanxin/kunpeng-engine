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
import com.anyilanxin.kunpeng.scheduler.exception.RecoverableException;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;

/** 仅 RecoverableException 重试; 其他异常 → 异常完成 */
public final class RecoverableRetryStrategy implements RetryStrategy {

  private static final Logger LOG = Loggers.ACTOR_LOGGER;

  private final ActorControl actor;

  public RecoverableRetryStrategy(final com.anyilanxin.kunpeng.scheduler.Actor actor) {
    this(actor.getControl());
  }

  public RecoverableRetryStrategy(final ActorControl actor) {
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
    attempt(callable, terminateCondition, result);
    return result;
  }

  private void attempt(
      final OperationToRetry callable,
      final BooleanSupplier terminateCondition,
      final CompletableActorFuture<Boolean> result) {
    actor.run(
        () -> {
          boolean success;
          try {
            success = callable.run();
          } catch (final RecoverableException e) {
            LOG.warn("Recoverable failure, retrying", e);
            if (terminateCondition.getAsBoolean()) {
              result.complete(false);
              return;
            }
            actor.run(() -> attempt(callable, terminateCondition, result));
            return;
          } catch (final Exception e) {
            result.completeExceptionally(e);
            return;
          }
          if (success) {
            result.complete(true);
          } else if (terminateCondition.getAsBoolean()) {
            result.complete(false);
          } else {
            actor.run(() -> attempt(callable, terminateCondition, result));
          }
        });
  }
}
