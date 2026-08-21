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
package io.atomix.raft;

import io.atomix.cluster.MemberId;
import io.atomix.utils.concurrent.Scheduled;
import io.atomix.utils.concurrent.ScheduledFutureImpl;
import io.atomix.utils.concurrent.ThreadContext;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.jmock.lib.concurrent.DeterministicScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class DeterministicSingleThreadContext implements ThreadContext {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DeterministicSingleThreadContext.class);

  private final DeterministicScheduler deterministicScheduler;
  private final MemberId memberId;

  public DeterministicSingleThreadContext(
      final DeterministicScheduler executor, final MemberId memberId) {
    deterministicScheduler = executor;
    this.memberId = memberId;
  }

  public DeterministicScheduler getDeterministicScheduler() {
    return deterministicScheduler;
  }

  public static ThreadContext createContext(final MemberId memberId) {
    return new DeterministicSingleThreadContext(new DeterministicScheduler(), memberId);
  }

  @Override
  public Scheduled schedule(final long delay, final TimeUnit timeUnit, final Runnable command) {
    final var future =
        deterministicScheduler.schedule(new WrappedRunnable(command), delay, timeUnit);
    return new ScheduledFutureImpl<>(future);
  }

  @Override
  public Scheduled schedule(final Duration delay, final Runnable command) {
    final var future =
        deterministicScheduler.schedule(
            new WrappedRunnable(command), delay.toMillis(), TimeUnit.MILLISECONDS);
    return new ScheduledFutureImpl<>(future);
  }

  @Override
  public Scheduled schedule(
      final long initialDelay,
      final long interval,
      final TimeUnit timeUnit,
      final Runnable command) {
    final ScheduledFuture<?> future =
        deterministicScheduler.scheduleAtFixedRate(
            new WrappedRunnable(command), initialDelay, interval, timeUnit);
    return new ScheduledFutureImpl<>(future);
  }

  @Override
  public Scheduled schedule(
      final Duration initialDelay, final Duration interval, final Runnable command) {
    final ScheduledFuture<?> future =
        deterministicScheduler.scheduleAtFixedRate(
            new WrappedRunnable(command),
            initialDelay.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS);
    return new ScheduledFutureImpl<>(future);
  }

  @Override
  public void execute(final Runnable command) {
    deterministicScheduler.execute(new WrappedRunnable(command));
  }

  @Override
  public void checkThread() {
    // always assume running on the right context
  }

  @Override
  public void close() {
    // do nothing
  }

  private final class WrappedRunnable implements Runnable {

    private final Runnable command;

    WrappedRunnable(final Runnable command) {
      this.command = command;
    }

    @Override
    public void run() {
      try (final var ignored = MDC.putCloseable("actor-scheduler", memberId.toString())) {
        command.run();
      } catch (final Exception e) {
        LOGGER.error("Uncaught exception", e);
        Assertions.fail("Uncaught exception" + e);
      }
    }
  }
}
