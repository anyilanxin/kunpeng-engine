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
package com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator;

import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;

public class NodeIdGeneratorStep implements StartupStep<BrokerStartupContext> {

  @Override
  public String getName() {
    return "Node Id Generator";
  }

  @Override
  public ActorFuture<BrokerStartupContext> startup(
      final BrokerStartupContext brokerStartupContext) {
    final ActorFuture<BrokerStartupContext> started =
        brokerStartupContext.getConcurrencyControl().createFuture();
    final AtomixCluster atomixCluster = brokerStartupContext.getAtomixCluster();
    final ClusterLeaderFoundService leaderFoundService = atomixCluster.getLeaderFoundService();
    final MessagingService messagingService = atomixCluster.getMessagingService();
    final ClusterMembershipService membershipService = atomixCluster.getMembershipService();
    final NodeIdGeneratorServiceImpl service =
        new NodeIdGeneratorServiceImpl(
            brokerStartupContext.getClusterMetaStore(),
            brokerStartupContext.getConcurrencyControl(),
            leaderFoundService,
            membershipService,
            messagingService);
    service
        .start()
        .onComplete(
            (unused, throwable) -> {
              if (throwable != null) {
                started.completeExceptionally(throwable);
              } else {
                brokerStartupContext.setRequestIdGenerator(service);
                started.complete(brokerStartupContext);
              }
            });
    return started;
  }

  @Override
  public ActorFuture<BrokerStartupContext> shutdown(
      final BrokerStartupContext brokerStartupContext) {
    final ActorFuture<BrokerStartupContext> stopFuture =
        brokerStartupContext.getConcurrencyControl().createFuture();
    brokerStartupContext.setRequestIdGenerator(null);
    stopFuture.complete(brokerStartupContext);
    return stopFuture;
  }
}
