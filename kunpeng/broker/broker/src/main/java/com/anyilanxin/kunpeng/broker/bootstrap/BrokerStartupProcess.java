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

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.bootstrap.step.*;
import com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi.CommandApiServiceStep;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorStep;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupProcess;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/** 编排 broker 启动步骤的执行流程，依次运行各启动/关闭步骤并产出 BrokerContext。 */
public final class BrokerStartupProcess {
  protected static final Logger LOGGER = BrokerLoggers.BROKER_LOGGER;
  private final StartupProcess<BrokerStartupContext> startupProcess;
  private final BrokerStartupContext context;
  private final ConcurrencyControl concurrencyControl;

  public BrokerStartupProcess(final BrokerStartupContext brokerStartupContext) {
    concurrencyControl = brokerStartupContext.getConcurrencyControl();
    context = brokerStartupContext;
    final var decoratedSteps = buildStartupSteps(brokerStartupContext.getBrokerConfiguration());
    startupProcess = new StartupProcess<>(LOGGER, decoratedSteps);
  }

  private List<StartupStep<BrokerStartupContext>> buildStartupSteps(final BrokerCfg config) {
    final var result = new ArrayList<StartupStep<BrokerStartupContext>>();
    result.add(new ClusterConfigStep());
    result.add(new ClusterClockStep());
    result.add(new ClusterTopologyStep());
    result.add(new NodeIdGeneratorStep());
    result.add(new ClusterDispatchClientStep());
    result.add(new CommandApiServiceStep());
    result.add(new ClusterRaftStep());
    result.add(new ClusterAdminStep());
    return result;
  }

  public ActorFuture<Void> start() {
    final ActorFuture<Void> result = concurrencyControl.createFuture();
    final var startupFuture = startupProcess.startup(concurrencyControl, context);
    concurrencyControl.runOnCompletion(
        startupFuture,
        (_, error) -> {
          if (error != null) {
            result.completeExceptionally(error);
          } else {
            result.complete(null);
          }
        });
    return result;
  }

  public ActorFuture<Void> stop() {
    final ActorFuture<Void> result = concurrencyControl.createFuture();
    final var shutdownFuture = startupProcess.shutdown(concurrencyControl, context);
    concurrencyControl.runOnCompletion(
        shutdownFuture,
        (_, error) -> {
          if (error != null) {
            result.completeExceptionally(error);
          } else {
            result.complete(null);
          }
        });
    return result;
  }
}
