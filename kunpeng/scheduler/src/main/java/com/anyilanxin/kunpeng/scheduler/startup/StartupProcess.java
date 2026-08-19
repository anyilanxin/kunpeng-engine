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
package com.anyilanxin.kunpeng.scheduler.startup;

import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.Loggers;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.exception.StartupProcessException;
import com.anyilanxin.kunpeng.scheduler.exception.StartupProcessShutdownException;
import com.anyilanxin.kunpeng.scheduler.exception.StartupProcessStepException;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/** 顺序启动链 + 逆序关闭；失败聚合 StartupProcessException */
public final class StartupProcess<CONTEXT> {

  private static final Logger DEFAULT_LOG = Loggers.SCHEDULER_LOGGER;
  private static final Duration STEP_STALL_WARN_AFTER = Duration.ofSeconds(30);

  private final Logger logger;
  private final List<StartupStep<CONTEXT>> steps;
  private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
  private volatile ActorFuture<CONTEXT> currentStartupFuture;

  public StartupProcess(final List<StartupStep<CONTEXT>> steps) {
    this(DEFAULT_LOG, steps);
  }

  public StartupProcess(final Logger logger, final List<? extends StartupStep<CONTEXT>> steps) {
    this.logger = logger;
    this.steps = List.copyOf(steps);
  }

  public ActorFuture<CONTEXT> startup(
      final ConcurrencyControl concurrencyControl, final CONTEXT context) {
    final CompletableActorFuture<CONTEXT> result = new CompletableActorFuture<>();
    proceed(concurrencyControl, context, 0, result);
    return result;
  }

  private void proceed(
      final ConcurrencyControl control,
      final CONTEXT context,
      final int index,
      final CompletableActorFuture<CONTEXT> result) {
    if (shutdownRequested.get()) {
      result.completeExceptionally(new StartupProcessShutdownException("Startup interrupted"));
      return;
    }
    if (index >= steps.size()) {
      logger.info("Startup process completed");
      result.complete(context);
      return;
    }
    final StartupStep<CONTEXT> step = steps.get(index);
    logger.info("Startup {}", step.getName());
    final ActorFuture<CONTEXT> stepFuture;
    try {
      stepFuture = step.startup(context);
    } catch (final Exception e) {
      failWith(control, context, index, e, result);
      return;
    }
    final ScheduledTimer stallWatch = watchStall(control, step);
    control.runOnCompletion(
        stepFuture,
        (ctx, error) -> {
          cancelWatch(stallWatch);
          if (error != null) {
            logger.info("Startup step {} failed", step.getName(), error);
            failWith(control, context, index, error, result);
          } else {
            logger.info("Startup step {} completed", step.getName());
            proceed(control, ctx, index + 1, result);
          }
        });
  }

  /** 步骤停摆自报告: 超时未完成则告警（定位启动链卡点的诊断设施） */
  private ScheduledTimer watchStall(
      final ConcurrencyControl control, final StartupStep<CONTEXT> step) {
    try {
      return control.schedule(
          STEP_STALL_WARN_AFTER,
          () ->
              logger.warn(
                  "Startup step {} still running after {}s",
                  step.getName(),
                  STEP_STALL_WARN_AFTER.toSeconds()));
    } catch (final RuntimeException e) {
      // schedule 需 owner 线程; 非常规调用形态下放弃 watchdog
      return null;
    }
  }

  private void cancelWatch(final ScheduledTimer timer) {
    if (timer != null) {
      try {
        timer.cancel();
      } catch (final RuntimeException ignored) {
        // 取消失败只影响一次多余的告警
      }
    }
  }

  private void failWith(
      final ConcurrencyControl control,
      final CONTEXT context,
      final int failedIndex,
      final Throwable error,
      final CompletableActorFuture<CONTEXT> result) {
    final var failedName = steps.get(failedIndex).getName();
    logger.warn("Aborting startup process due to exception during step {}", failedName, error);
    final var stepException = new StartupProcessStepException(failedName, error);
    // 逆序关闭已启动步骤
    shutdownStarted(control, context, failedIndex)
        .onComplete(
            (ignored, closeError) -> {
              final var aggregate = new StartupProcessException("Startup process failed");
              aggregate.addSuppressed(stepException);
              if (closeError != null) {
                aggregate.addSuppressed(closeError);
              }
              result.completeExceptionally(aggregate);
            });
  }

  /** 逆序关闭全部已启动步骤 */
  public ActorFuture<Void> shutdown(final ConcurrencyControl control, final CONTEXT context) {
    shutdownRequested.set(true);
    return shutdownStarted(control, context, steps.size());
  }

  private ActorFuture<Void> shutdownStarted(
      final ConcurrencyControl control, final CONTEXT context, final int upToIndex) {
    final CompletableActorFuture<Void> result = new CompletableActorFuture<>();
    shutdownReverse(control, context, upToIndex - 1, new ArrayList<>(), result);
    return result;
  }

  private void shutdownReverse(
      final ConcurrencyControl control,
      final CONTEXT context,
      final int index,
      final List<Throwable> errors,
      final CompletableActorFuture<Void> result) {
    if (index < 0) {
      if (errors.isEmpty()) {
        result.complete(null);
      } else {
        final var aggregate = new StartupProcessException("Shutdown failed");
        errors.forEach(aggregate::addSuppressed);
        result.completeExceptionally(aggregate);
      }
      return;
    }
    final StartupStep<CONTEXT> step = steps.get(index);
    logger.info("Shutdown {}", step.getName());
    final ActorFuture<CONTEXT> stepFuture;
    try {
      stepFuture = step.shutdown(context);
    } catch (final Exception e) {
      logger.error("Shutdown step {} failed", step.getName(), e);
      errors.add(e);
      shutdownReverse(control, context, index - 1, errors, result);
      return;
    }
    control.runOnCompletion(
        stepFuture,
        (ctx, error) -> {
          if (error != null) {
            logger.error("Shutdown step {} failed", step.getName(), error);
            errors.add(error);
          }
          shutdownReverse(control, context, index - 1, errors, result);
        });
  }

  public List<String> getStepNames() {
    final List<String> names = new ArrayList<>(steps.size());
    steps.forEach(step -> names.add(step.getName()));
    return names;
  }
}
