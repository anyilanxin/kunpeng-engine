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

import com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorServiceImpl;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.admin.ClusterAdminService;
import com.anyilanxin.kunpeng.cluster.manager.business.ClusterBusinessService;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import io.micrometer.core.instrument.MeterRegistry;

/** BrokerStartupContext 的实现类，承载 broker 启动/关闭所需的配置与依赖。 */
public final class BrokerStartupContextImpl implements BrokerStartupContext {
  private final BrokerCfg brokerCfg;
  private final ClusterCfg clusterCfg;
  private final ActorSchedulingService schedulingService;
  private final AtomixCluster atomixCluster;
  private final ConcurrencyControl concurrencyControl;
  private final MeterRegistry meterRegistry;
  private ClusterAdminService clusterManagerService;
  private ClusterBusinessService clusterBusinessService;
  private CommandApiServiceImpl commandApiService;
  private ClusterMetaStore clusterMetaStore;
  private NodeIdGeneratorServiceImpl idGenerator;
  private ClusterDispatchClient dispatchClient;
  private DefaultClusterSwimTopologyService clusterPartitionTopology;
  private ClusterTopologyService clusterTopologyService;
  private TimerClock timerClock;

  public BrokerStartupContextImpl(
      final BrokerCfg brokerCfg,
      final ClusterCfg clusterCfg,
      final ActorSchedulingService schedulingService,
      final AtomixCluster atomixCluster,
      final ConcurrencyControl concurrencyControl,
      final MeterRegistry meterRegistry) {
    this.clusterCfg = clusterCfg;
    this.meterRegistry = meterRegistry;
    this.brokerCfg = brokerCfg;
    this.schedulingService = schedulingService;
    this.atomixCluster = atomixCluster;
    this.concurrencyControl = concurrencyControl;
  }

  @Override
  public BrokerCfg getBrokerConfiguration() {
    return brokerCfg;
  }

  @Override
  public ClusterCfg getClusterConfiguration() {
    return clusterCfg;
  }

  @Override
  public ActorSchedulingService getActorSchedulingService() {
    return schedulingService;
  }

  @Override
  public AtomixCluster getAtomixCluster() {
    return atomixCluster;
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  @Override
  public ConcurrencyControl getConcurrencyControl() {
    return concurrencyControl;
  }

  @Override
  public ClusterAdminService getClusterManagerService() {
    return clusterManagerService;
  }

  @Override
  public void setClusterManagerService(final ClusterAdminService clusterManagerService) {
    this.clusterManagerService = clusterManagerService;
  }

  @Override
  public ClusterBusinessService getClusterBusinessService() {
    return clusterBusinessService;
  }

  @Override
  public void setClusterBusinessService(final ClusterBusinessService clusterBusinessService) {
    this.clusterBusinessService = clusterBusinessService;
  }

  @Override
  public CommandApiServiceImpl getCommandApiService() {
    return commandApiService;
  }

  @Override
  public void setCommandApiService(final CommandApiServiceImpl commandApiService) {
    this.commandApiService = commandApiService;
  }

  @Override
  public ClusterMetaStore getClusterMetaStore() {
    return clusterMetaStore;
  }

  @Override
  public void setClusterMetaStore(final ClusterMetaStore clusterMetaStore) {
    this.clusterMetaStore = clusterMetaStore;
  }

  @Override
  public NodeIdGeneratorServiceImpl getRequestIdGenerator() {
    return idGenerator;
  }

  @Override
  public void setRequestIdGenerator(final NodeIdGeneratorServiceImpl idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public ClusterDispatchClient getClusterDispatchClient() {
    return dispatchClient;
  }

  @Override
  public void setClusterDispatchClient(final ClusterDispatchClient dispatchClient) {
    this.dispatchClient = dispatchClient;
  }

  @Override
  public DefaultClusterSwimTopologyService getClusterPartitionTopology() {
    return clusterPartitionTopology;
  }

  @Override
  public void setClusterPartitionTopology(
      final DefaultClusterSwimTopologyService clusterPartitionTopology) {
    this.clusterPartitionTopology = clusterPartitionTopology;
  }

  @Override
  public ClusterTopologyService getClusterTopologyService() {
    return clusterTopologyService;
  }

  @Override
  public void setClusterTopologyService(final ClusterTopologyService clusterTopologyService) {
    this.clusterTopologyService = clusterTopologyService;
  }

  @Override
  public TimerClock getTimerClock() {
    return timerClock;
  }

  @Override
  public void setTimerClock(final TimerClock timerClock) {
    this.timerClock = timerClock;
  }
}
