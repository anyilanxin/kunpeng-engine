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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.cluster.business.PartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.business.RaftPartitionFactory;
import com.anyilanxin.kunpeng.cluster.business.step.transition.PartitionTransition;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.clusterleader.DefaultClusterLeaderManageService;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;

/**
 * 管理分区启动流程上下文，承载管理 Raft 分组启动各步骤所需的依赖与运行期状态（Raft 分区、分区转换等）。
 *
 * @author zxuanhong
 * @since
 */
public class AdminPartitionStartupContext
    implements PartitionStartupContext<AdminTransitionContent> {
  private final ConcurrencyControl concurrencyControl;
  private final ActorSchedulingService schedulingService;
  private final RaftPartitionFactory raftPartitionFactory;
  private final PartitionMetadata partitionMetadata;
  private final RaftSnapshotProvider snapshotProvider;
  private final EntryValidator entryValidator = new AdminEntryValidator();
  private final MeterRegistry meterRegistry;
  private final PartitionManagementService managementService;
  private final BrokerCfg brokerCfg;
  private final CommandApiService commandApiHandle;
  private final TimerClock timerClock;

  private PartitionTransition<AdminTransitionContent> partitionTransition;
  private Path partitionDirectory;
  private RaftPartition raftPartition;
  private DefaultClusterLeaderManageService clusterLeaderManageService;
  private final ClusterMetaStore clusterMetaStore;
  private final ClusterDispatchClient dispatchClient;
  private final DefaultClusterSwimTopologyService brokerTopologyService;
  private final ClusterTopologyService clusterTopologyService;

  public AdminPartitionStartupContext(
      final ClusterMetaStore clusterMetaStore,
      final ActorSchedulingService schedulingService,
      final ConcurrencyControl concurrencyControl,
      final RaftPartitionFactory raftPartitionFactory,
      final PartitionMetadata partitionMetadata,
      final RaftSnapshotProvider snapshotProvider,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final BrokerCfg brokerCfg,
      final CommandApiService commandApiHandle,
      final ClusterDispatchClient dispatchClient,
      final DefaultClusterSwimTopologyService brokerTopologyService,
      final ClusterTopologyService clusterTopologyService,
      final TimerClock timerClock) {
    this.timerClock = timerClock;
    this.brokerCfg = brokerCfg;
    this.brokerTopologyService = brokerTopologyService;
    this.clusterTopologyService = clusterTopologyService;
    this.dispatchClient = dispatchClient;
    this.clusterMetaStore = clusterMetaStore;
    this.commandApiHandle = commandApiHandle;
    this.schedulingService = schedulingService;
    this.concurrencyControl = concurrencyControl;
    this.raftPartitionFactory = raftPartitionFactory;
    this.partitionMetadata = partitionMetadata;
    this.snapshotProvider = snapshotProvider;
    this.meterRegistry = meterRegistry;
    this.managementService = managementService;
  }

  @Override
  public RaftPartitionFactory getRaftPartitionFactory() {
    return raftPartitionFactory;
  }

  @Override
  public PartitionMetadata getPartitionMetadata() {
    return partitionMetadata;
  }

  @Override
  public RaftSnapshotProvider getSnapshotProvider() {
    return snapshotProvider;
  }

  @Override
  public EntryValidator getEntryValidator() {
    return entryValidator;
  }

  @Override
  public Path getPartitionDirectory() {
    return partitionDirectory;
  }

  @Override
  public PartitionManagementService getPartitionManagementService() {
    return managementService;
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
  public RaftPartition getRaftPartition() {
    return raftPartition;
  }

  @Override
  public void setRaftPartition(final RaftPartition raftPartition) {
    this.raftPartition = raftPartition;
    if (raftPartition != null) {
      partitionDirectory = raftPartition.rootDirectory();
    } else {
      partitionDirectory = null;
    }
  }

  @Override
  public ActorSchedulingService getActorSchedulingService() {
    return schedulingService;
  }

  @Override
  public PartitionTransition<AdminTransitionContent> getPartitionTransition() {
    return partitionTransition;
  }

  @Override
  public void setPartitionTransition(
      final PartitionTransition<AdminTransitionContent> partitionTransition) {
    this.partitionTransition = partitionTransition;
  }

  public DefaultClusterLeaderManageService getClusterLeaderManageService() {
    return clusterLeaderManageService;
  }

  public void setClusterLeaderManageService(
      final DefaultClusterLeaderManageService clusterLeaderManageService) {
    this.clusterLeaderManageService = clusterLeaderManageService;
  }

  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  public CommandApiService getCommandApiHandle() {
    return commandApiHandle;
  }

  public TimerClock getTimerClock() {
    return timerClock;
  }

  public ClusterMetaStore getClusterMetaStore() {
    return clusterMetaStore;
  }

  public ClusterDispatchClient getDispatchClient() {
    return dispatchClient;
  }

  public DefaultClusterSwimTopologyService getBrokerTopologyService() {
    return brokerTopologyService;
  }

  public ClusterTopologyService getClusterTopologyService() {
    return clusterTopologyService;
  }
}
