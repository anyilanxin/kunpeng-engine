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
package com.anyilanxin.kunpeng.broker.bootstrap.step;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.BusinessMessaging;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.CommPortType;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MemberNodeInfo;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronMessagingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 业务面消息服务的 broker 启动步骤：启动独立端口的业务消息实例并把端口写入本地成员属性广播（对端经元数据二次拉取补全后可寻址）。
 *
 * <p>服务 Bean 由 BusinessMessagingConfiguration 构建（此处经 BeanFactory 获取，保持与 Spring 装配解耦），服务停止由 Bean 的
 * destroyMethod 负责，本步骤只管理启动时序与端口广播。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessMessagingServiceStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Business Messaging Service";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    brokerStartupContext
        .getConcurrencyControl()
        .run(
            () -> {
              try {
                final AeronMessagingService service =
                    brokerStartupContext.getBeanFactory().getBean(AeronMessagingService.class);
                BusinessMessaging.startAndAdvertise(
                    service, brokerStartupContext.getAtomixCluster().getMembershipService());
                brokerStartupContext.setBusinessMessagingService(service);
                startupFuture.complete(brokerStartupContext);
              } catch (final Exception e) {
                startupFuture.completeExceptionally(e);
              }
            });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    brokerShutdownContext
        .getConcurrencyControl()
        .run(
            () -> {
              // 服务停止交由 Bean destroyMethod; 此处仅摘除端口注册项并清理上下文
              final AeronMessagingService service =
                  brokerShutdownContext.getBusinessMessagingService();
              if (service != null) {
                MemberNodeInfo.removePort(
                    brokerShutdownContext
                        .getAtomixCluster()
                        .getMembershipService()
                        .getLocalMember()
                        .properties(),
                    CommPortType.BUSINESS);
              }
              brokerShutdownContext.setBusinessMessagingService(null);
              shutdownFuture.complete(brokerShutdownContext);
            });
  }
}
