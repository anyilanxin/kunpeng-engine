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
package com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * @author zxuanhong
 * @since
 */
public class CommandApiServiceStep extends AbstractBrokerStartupStep {
  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final MessagingService apiMessagingService =
        brokerStartupContext.getAtomixCluster().getMessagingService();
    final CommandApiServiceImpl service =
        new CommandApiServiceImpl(
            apiMessagingService, brokerStartupContext.getRequestIdGenerator());
    final ActorSchedulingService actorSchedulingService =
        brokerStartupContext.getActorSchedulingService();
    concurrencyControl.runOnCompletion(
        actorSchedulingService.submitActor(service),
        proceed(
            () -> {
              brokerStartupContext.setCommandApiService(service);
              startupFuture.complete(brokerStartupContext);
            },
            startupFuture));
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final var commandApiServiceActor = brokerShutdownContext.getCommandApiService();
    concurrencyControl.runOnCompletion(
        commandApiServiceActor.closeAsync(),
        proceed(
            () -> {
              brokerShutdownContext.setCommandApiService(null);
              shutdownFuture.complete(brokerShutdownContext);
            },
            shutdownFuture));
  }

  @Override
  public String getName() {
    return "Admin Command API";
  }
}
