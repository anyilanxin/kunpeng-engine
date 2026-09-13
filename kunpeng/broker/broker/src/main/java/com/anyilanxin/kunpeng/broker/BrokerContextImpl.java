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

import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;

/** BrokerContext 的实现类，持有 AtomixCluster 与 API 消息服务。 */
final class BrokerContextImpl implements BrokerContext {

  private final AtomixCluster atomixCluster;
  private final MessagingService apiMessagingService;
  private final BrokerCfg brokerCfg;
  private final ClusterCfg clusterCfg;
  private final ActorSchedulingService schedulingService;
  private final MeterRegistry meterRegistry;

  BrokerContextImpl(
      final AtomixCluster atomixCluster,
      final BrokerCfg brokerCfg,
      final ClusterCfg clusterCfg,
      final ActorSchedulingService schedulingService,
      final MeterRegistry meterRegistry) {
    this.atomixCluster = requireNonNull(atomixCluster);
    apiMessagingService = requireNonNull(atomixCluster.getMessagingService());
    this.brokerCfg = requireNonNull(brokerCfg);
    this.clusterCfg = requireNonNull(clusterCfg);
    this.schedulingService = requireNonNull(schedulingService);
    this.meterRegistry = meterRegistry;
  }

  @Override
  public AtomixCluster getAtomixCluster() {
    return atomixCluster;
  }

  @Override
  public MessagingService getApiMessagingService() {
    return apiMessagingService;
  }

  @Override
  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  @Override
  public ClusterCfg getClusterCfg() {
    return clusterCfg;
  }

  @Override
  public ActorSchedulingService getActorSchedulingService() {
    return schedulingService;
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  @Override
  public String getNodeId() {
    return "";
  }
}
