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
package com.anyilanxin.kunpeng.broker.gateway;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.gateway.Gateway;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import org.agrona.CloseHelper;
import org.slf4j.Logger;

/**
 * 内嵌网关服务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class EmbeddedGatewayService implements AutoCloseable {
  private final Gateway gateway;
  private final BrokerClient brokerClient;
  private final ConcurrencyControl concurrencyControl;
  private static final Logger LOGGER = GatewayLoggers.GATEWAY_LOGGER;

  public EmbeddedGatewayService(
      final BrokerCfg configuration,
      final ActorSchedulingService actorScheduler,
      final ConcurrencyControl concurrencyControl,
      final BrokerClient brokerClient,
      final MeterRegistry meterRegistry,
      final AtomixCluster atomixCluster,
      final ClusterLeaderFoundService leaderService,
      final BrokerTopologyManager topologyManager) {
    this.concurrencyControl = concurrencyControl;
    this.brokerClient = brokerClient;
    gateway =
        new Gateway(
            actorScheduler,
            configuration.getGateway(),
            atomixCluster,
            meterRegistry,
            leaderService,
            brokerClient,
            topologyManager);
  }

  @Override
  public void close() {
    CloseHelper.closeAll(
        error -> LOGGER.warn("Error occurred while shutting down embedded gateway", error),
        gateway,
        brokerClient);
  }

  public Gateway get() {
    return gateway;
  }

  public ActorFuture<Gateway> start() {
    return gateway.start();
  }
}
