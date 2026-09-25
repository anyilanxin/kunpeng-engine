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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.bootstrap.step.commandapi;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 业务命令 API 服务的 broker 启动步骤：构建 actor 并提交调度；分区 leader 的日志绑定由业务 transition 步骤完成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessCommandApiServiceStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Business Command Api Service";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final MessagingService apiMessagingService =
        brokerStartupContext.getAtomixCluster().getMessagingService();
    final CommandApiServiceImpl service =
        new CommandApiServiceImpl(
            apiMessagingService, brokerStartupContext.getRequestIdGenerator()::nextId);
    concurrencyControl.runOnCompletion(
        brokerStartupContext.getActorSchedulingService().submitActor(service),
        proceed(
            () -> {
              brokerStartupContext.setBusinessCommandApiService(service);
              startupFuture.complete(brokerStartupContext);
            },
            startupFuture));
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final CommandApiServiceImpl service = brokerShutdownContext.getBusinessCommandApiService();
    if (service == null) {
      shutdownFuture.complete(brokerShutdownContext);
      return;
    }
    concurrencyControl.runOnCompletion(
        service.closeAsync(),
        proceed(
            () -> {
              brokerShutdownContext.setBusinessCommandApiService(null);
              shutdownFuture.complete(brokerShutdownContext);
            },
            shutdownFuture));
  }
}
