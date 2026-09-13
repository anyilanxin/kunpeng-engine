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
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * @author zxuanhong
 * @since
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
        brokerContext.getBrokerCfg(),
        brokerContext.getClusterCfg(),
        brokerContext.getActorSchedulingService(),
        brokerContext.getAtomixCluster(),
        this,
        brokerContext.getMeterRegistry());
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
