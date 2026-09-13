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
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.DefaultClusterMetaStore;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;

/** 集群管理（Cluster Admin）相关的 broker 启动步骤。 */
public final class ClusterConfigStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Cluster Config Storage";
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
              final BrokerCfg brokerCfg = brokerStartupContext.getBrokerConfiguration();
              try {
                final DefaultClusterMetaStore clusterMetaStore =
                    new DefaultClusterMetaStore(Path.of(brokerCfg.getData().getDirectory()));
                brokerStartupContext.setClusterMetaStore(clusterMetaStore);
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
              final ClusterMetaStore clusterMetaStore = brokerShutdownContext.getClusterMetaStore();
              if (clusterMetaStore != null) {
                clusterMetaStore.close();
              }
              brokerShutdownContext.setClusterMetaStore(null);
              shutdownFuture.complete(brokerShutdownContext);
            });
  }
}
