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

import com.anyilanxin.kunpeng.broker.BrokerContext;
import com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.Optional;

/**
 * broker 启动 actor：按顺序编排各启动步骤并驱动相位切换。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerStartupActor extends Actor {
  private final BrokerStartupProcess brokerStartupProcess;
  private final String nodeId;
  private final BrokerStartupContext brokerStartup;
  private final BrokerContext brokerContext;

  public BrokerStartupActor(final BrokerContext brokerContext) {
    this.brokerContext = brokerContext;
    brokerStartup = createBrokerStartup();
    nodeId = brokerContext.getNodeId();
    brokerStartupProcess = new BrokerStartupProcess(brokerStartup);
  }

  private BrokerStartupContext createBrokerStartup() {
    return new BrokerStartupContextImpl(
        brokerContext.getBrokerClient(),
        brokerContext.getBrokerCfg(),
        brokerContext.getClusterCfg(),
        brokerContext.getActorSchedulingService(),
        brokerContext.getAtomixCluster(),
        this,
        brokerContext.getMeterRegistry(),
        brokerContext.getBeanFactory(),
        brokerContext.getSinksConfig());
  }

  /** broker 侧 job 流派发器（观测用；对应启动步骤尚未执行时为空） */
  public Optional<JobStreamDispatcher> getJobStreamDispatcher() {
    return Optional.ofNullable(brokerStartup.getJobStreamDispatcher());
  }

  /** 内嵌网关订阅中心（观测用；未启用内嵌网关时为空） */
  public Optional<GatewayJobHub> getEmbeddedGatewayJobHub() {
    return Optional.ofNullable(brokerStartup.getEmbeddedGatewayService())
        .map(service -> service.get().jobHub());
  }

  @Override
  public String getName() {
    return "Startup-" + nodeId;
  }

  public ActorFuture<Void> start() {
    final ActorFuture<Void> result = createFuture();
    actor.run(
        () -> {
          actor.runOnCompletion(brokerStartupProcess.start(), result);
        });
    return result;
  }

  public ActorFuture<Void> stop() {
    final ActorFuture<Void> result = createFuture();
    actor.run(() -> actor.runOnCompletion(brokerStartupProcess.stop(), result));
    return result;
  }
}
