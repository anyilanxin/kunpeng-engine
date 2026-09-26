/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
import com.anyilanxin.kunpeng.broker.gateway.EmbeddedGatewayService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.concurrent.CompletableFuture;

public class EmbeddedGatewayServiceStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Embedded Gateway";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final var embeddedGatewayService =
        new EmbeddedGatewayService(
            brokerStartupContext.getBrokerConfiguration(),
            brokerStartupContext.getActorSchedulingService(),
            brokerStartupContext.getConcurrencyControl(),
            brokerStartupContext.getBrokerClient(),
            brokerStartupContext.getMeterRegistry(),
            brokerStartupContext.getAtomixCluster(),
            brokerStartupContext.getAtomixCluster().getLeaderFoundService(),
            brokerStartupContext.getClusterTopologyService());
    final var embeddedGatewayServiceFuture = embeddedGatewayService.start();
    concurrencyControl.runOnCompletion(
        embeddedGatewayServiceFuture,
        (gateway, error) -> {
          if (error != null) {
            startupFuture.completeExceptionally(error);
            return;
          }
          brokerStartupContext.setEmbeddedGatewayService(embeddedGatewayService);
          startupFuture.complete(brokerStartupContext);
        });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {

    final var embeddedGatewayService = brokerShutdownContext.getEmbeddedGatewayService();

    if (embeddedGatewayService == null) {
      shutdownFuture.complete(brokerShutdownContext);
      return;
    }
    CompletableFuture.runAsync(embeddedGatewayService::close)
        .whenComplete(
            (ok, error) -> {
              if (error != null) {
                shutdownFuture.completeExceptionally(error);
              } else {
                concurrencyControl.run(
                    () ->
                        forwardExceptions(
                            () -> {
                              brokerShutdownContext.setEmbeddedGatewayService(null);
                              shutdownFuture.complete(brokerShutdownContext);
                            },
                            shutdownFuture));
              }
            });
  }
}
