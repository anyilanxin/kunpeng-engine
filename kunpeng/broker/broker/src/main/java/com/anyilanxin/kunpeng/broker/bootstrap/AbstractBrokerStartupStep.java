/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.bootstrap;

import static com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture.completedExceptionally;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;
import java.util.function.BiConsumer;
import org.slf4j.Logger;

/** broker 启动步骤的抽象基类，为基于 future 的启动/关闭流程提供公共辅助方法。 */
public abstract class AbstractBrokerStartupStep implements StartupStep<BrokerStartupContext> {
  protected static final Logger LOGGER = BrokerLoggers.BROKER_LOGGER;

  @Override
  public final ActorFuture<BrokerStartupContext> startup(
      final BrokerStartupContext brokerStartupContext) {
    return createFutureAndRun(
        brokerStartupContext,
        (concurrencyControl, future) ->
            startupInternal(brokerStartupContext, concurrencyControl, future));
  }

  @Override
  public final ActorFuture<BrokerStartupContext> shutdown(
      final BrokerStartupContext brokerShutdownContext) {
    return createFutureAndRun(
        brokerShutdownContext,
        (concurrencyControl, future) ->
            shutdownInternal(brokerShutdownContext, concurrencyControl, future));
  }

  protected abstract void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture);

  protected abstract void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture);

  /** 辅助方法：尝试创建 future 并执行 runnable；若在创建 future 过程中抛出异常， 则将该异常转发到占位 future。 */
  protected final ActorFuture<BrokerStartupContext> createFutureAndRun(
      final BrokerStartupContext brokerStartupContext,
      final BiConsumer<ConcurrencyControl, ActorFuture<BrokerStartupContext>> runnable) {
    try {
      final var concurrencyControl = brokerStartupContext.getConcurrencyControl();
      final ActorFuture<BrokerStartupContext> future = concurrencyControl.createFuture();

      forwardExceptions(() -> runnable.accept(concurrencyControl, future), future);
      return future;
    } catch (final Exception e) {
      LOGGER.error("Broker startup step failed", e);
      return completedExceptionally(e);
    }
  }

  /** 辅助方法：将同步代码块抛出的异常转发到 future 对象。 */
  protected final <V> void forwardExceptions(final Runnable r, final ActorFuture<V> future) {
    try {
      r.run();
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
  }

  /** 辅助方法：消费前一个 future 的结果。若前一个 future 异常完成，则将该异常转发给作为参数传入的 future；否则执行 runnable。 */
  protected final <V> BiConsumer<Void, Throwable> proceed(
      final Runnable r, final ActorFuture<V> future) {
    return (ok, error) -> {
      if (error != null) {
        future.completeExceptionally(error);
      } else {
        forwardExceptions(r, future);
      }
    };
  }
}
