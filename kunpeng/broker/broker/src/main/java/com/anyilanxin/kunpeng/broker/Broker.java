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
package com.anyilanxin.kunpeng.broker;

import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupActor;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.utils.LogUtil;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import com.anyilanxin.kunpeng.utils.exception.UncheckedExecutionException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

/** Broker 核心类，负责组装并启动 broker 所需的各种组件，并管理其启动与关闭生命周期。 */
public final class Broker implements AutoCloseable {
  private static final String BROKER_ID_LOG_PROPERTY = "broker-id";
  private static final Logger LOGGER = BrokerLoggers.BROKER_LOGGER;
  private final Map<String, String> diagnosticContext;
  private boolean isClosed = false;
  private CompletableFuture<Broker> startFuture;
  private final BrokerStartupActor brokerStartupActor;
  private final BrokerCfg brokerCfg;
  private final ClusterCfg clusterCfg;
  private final BrokerContext brokerContext;

  public Broker(
      final BeanFactory beanFactory,
      final ActorSchedulingService schedulingService,
      final ClusterCfg clusterCfg,
      final BrokerCfg brokerCfg,
      final AtomixCluster atomixCluster,
      final MeterRegistry meterRegistry) {
    this.clusterCfg = clusterCfg;
    this.brokerCfg = brokerCfg;
    final String brokerId = String.format("Broker-%s", clusterCfg.getNodeId());
    diagnosticContext = Collections.singletonMap(BROKER_ID_LOG_PROPERTY, brokerId);
    brokerContext =
        new BrokerContextImpl(
            atomixCluster, brokerCfg, clusterCfg, schedulingService, meterRegistry);
    brokerStartupActor = new BrokerStartupActor(brokerContext);
    schedulingService.submitActor(brokerStartupActor);
  }

  public synchronized CompletableFuture<Broker> start() {
    if (startFuture == null) {
      logBrokerStart();
      startFuture = new CompletableFuture<>();
      LogUtil.doWithMDC(diagnosticContext, this::internalStart);
    }
    return startFuture;
  }

  private void logBrokerStart() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "Starting broker {} version {}", clusterCfg.getNodeId(), VersionUtil.getVersion());
    }
  }

  private void internalStart() {
    try {
      brokerStartupActor.start().join();
      startFuture.complete(this);
    } catch (final Exception bootStrapException) {
      LOGGER.error("Failed to start broker {}!", clusterCfg.getNodeId(), bootStrapException);
      final UncheckedExecutionException exception =
          new UncheckedExecutionException("Failed to start broker", bootStrapException);
      startFuture.completeExceptionally(exception);
      throw exception;
    }
  }

  public BrokerContext getBrokerContext() {
    return brokerContext;
  }

  @Override
  public void close() {
    LogUtil.doWithMDC(
        diagnosticContext,
        () -> {
          if (!isClosed && startFuture != null) {
            startFuture
                .thenAccept(
                    _ -> {
                      brokerStartupActor.stop().join();
                      isClosed = true;
                      LOGGER.info("Broker shut down.");
                    })
                .join();
          }
        });
  }
}
