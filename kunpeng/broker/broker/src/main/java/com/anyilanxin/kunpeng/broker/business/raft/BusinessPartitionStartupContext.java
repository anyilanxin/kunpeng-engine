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
package com.anyilanxin.kunpeng.broker.business.raft;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher;
import com.anyilanxin.kunpeng.cluster.business.PartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.business.RaftPartitionFactory;
import com.anyilanxin.kunpeng.cluster.business.step.transition.PartitionTransition;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.sink.config.SinksConfig;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import org.springframework.beans.factory.BeanFactory;

/**
 * 业务分区启动流程的上下文，聚合 Raft 分区工厂、分区元数据、快照提供者等启动依赖，并承载业务分区实例与分区状态迁移（transition）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessPartitionStartupContext
    implements PartitionStartupContext<BusinessTransitionContent> {
  private final ConcurrencyControl concurrencyControl;
  private final ActorSchedulingService schedulingService;
  private final RaftPartitionFactory raftPartitionFactory;
  private final PartitionMetadata metadata;
  private final RaftSnapshotProvider snapshotProvider;
  private final EntryValidator entryValidator = new BusinessEntryValidator();
  private final MeterRegistry meterRegistry;
  private final PartitionManagementService managementService;
  private final TimerClock timerClock;
  private final BrokerCfg brokerCfg;
  private final BeanFactory beanFactory;
  private final SinksConfig sinksConfig;
  private final JobStreamDispatcher jobStreamDispatcher;
  private final CommandApiServiceImpl commandApiService;
  private final com.anyilanxin.kunpeng.broker.topology.PartitionTopologyNotifier topologyNotifier;

  private PartitionTransition<BusinessTransitionContent> partitionTransition;
  private Path partitionDirectory;
  private RaftPartition raftPartition;
  private final DefaultClusterSwimTopologyService brokerTopologyService;

  public BusinessPartitionStartupContext(
      final ActorSchedulingService schedulingService,
      final ConcurrencyControl concurrencyControl,
      final RaftPartitionFactory raftPartitionFactory,
      final PartitionMetadata metadata,
      final RaftSnapshotProvider snapshotProvider,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final DefaultClusterSwimTopologyService brokerTopologyService,
      final TimerClock timerClock,
      final BrokerCfg brokerCfg,
      final BeanFactory beanFactory,
      final SinksConfig sinksConfig,
      final JobStreamDispatcher jobStreamDispatcher,
      final CommandApiServiceImpl commandApiService,
      final com.anyilanxin.kunpeng.broker.topology.PartitionTopologyNotifier topologyNotifier) {
    this.beanFactory = beanFactory;
    this.sinksConfig = sinksConfig;
    this.jobStreamDispatcher = jobStreamDispatcher;
    this.commandApiService = commandApiService;
    this.topologyNotifier = topologyNotifier;
    this.timerClock = timerClock;
    this.brokerCfg = brokerCfg;
    this.schedulingService = schedulingService;
    this.concurrencyControl = concurrencyControl;
    this.raftPartitionFactory = raftPartitionFactory;
    this.metadata = metadata;
    this.snapshotProvider = snapshotProvider;
    this.meterRegistry = meterRegistry;
    this.managementService = managementService;
    this.brokerTopologyService = brokerTopologyService;
  }

  @Override
  public RaftPartitionFactory getRaftPartitionFactory() {
    return raftPartitionFactory;
  }

  @Override
  public PartitionMetadata getPartitionMetadata() {
    return metadata;
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
  public PartitionTransition<BusinessTransitionContent> getPartitionTransition() {
    return partitionTransition;
  }

  @Override
  public void setPartitionTransition(
      final PartitionTransition<BusinessTransitionContent> partitionTransition) {
    this.partitionTransition = partitionTransition;
  }

  public TimerClock getTimerClock() {
    return timerClock;
  }

  public com.anyilanxin.kunpeng.broker.topology.PartitionTopologyNotifier getTopologyNotifier() {
    return topologyNotifier;
  }

  public CommandApiServiceImpl getCommandApiService() {
    return commandApiService;
  }

  /** broker 层 job 派发能力服务（raft 业务层经此绑定 leader 语义与业务落点） */
  public JobStreamDispatcher getJobStreamDispatcher() {
    return jobStreamDispatcher;
  }

  public SinksConfig getSinksConfig() {
    return sinksConfig;
  }

  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public DefaultClusterSwimTopologyService getBrokerTopologyService() {
    return brokerTopologyService;
  }
}
