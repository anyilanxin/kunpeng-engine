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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandReceiverActor;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandSenderService;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.logstorage.BusinessRaftEventStore;
import com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionContent;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.messaging.PartitionMessagingService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.engine.bpmn.EngineProcessService;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryProcessService;
import com.anyilanxin.kunpeng.repository.business.RocksdbBusinessRepositoryFactory;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.sink.config.SinksConfig;
import com.anyilanxin.kunpeng.sink.runtime.SinkService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.BeanFactory;

/**
 * 业务分区状态迁移（transition）的内容载体，承载迁移过程中的并发控制、当前任期（term）与当前角色信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessTransitionContent
    implements TransitionContent<KvStore<BusinessRepositoryColumnFamilies>> {
  private final RaftSnapshotProvider<KvStore<BusinessRepositoryColumnFamilies>> snapshotProvider;
  private final MeterRegistry meterRegistry;
  private final RaftPartition raftPartition;
  private final BrokerCfg brokerCfg;
  private final int maxFragmentSize;
  private final ActorSchedulingService schedulingService;
  private final TimerClock clock;
  private final MessagingService messagingService;
  private final BeanFactory beanFactory;
  private final SinksConfig sinksConfig;
  private final JobStreamDispatcher jobStreamDispatcher;
  private final CommandApiServiceImpl commandApiService;
  private final ClusterCommunicationService communicationService;
  private final ClusterTopologyService topologyService;
  private final PartitionMessagingService partitionCommunicationService;
  private final RaftPartitionSource partitionSource;

  // 分区级有状态组件（transition 过程中构建/关闭，引擎与分区命令服务持有）
  private InterPartitionCommandSenderService partitionCommandSender;
  private InterPartitionCommandReceiverActor partitionCommandReceiver;
  private EngineProcessService engineProcessService;

  private BusinessRepositoryProcessService applierService;
  private ConcurrencyControl concurrencyControl;
  private KvStore<BusinessRepositoryColumnFamilies> rocksdb;
  private RocksdbBusinessRepositoryFactory rocksdbRepositoryFactory;
  private long currentTerm;
  private RaftServer.Role currentRole;
  private EventLog eventLog;
  private BusinessRaftEventStore logStorage;
  private SinkService sinkService;

  public BusinessTransitionContent(
      final RaftPartitionSource partitionSource,
      final PartitionMessagingService partitionCommunicationService,
      final RaftSnapshotProvider<KvStore<BusinessRepositoryColumnFamilies>> snapshotProvider,
      final MeterRegistry meterRegistry,
      final RaftPartition raftPartition,
      final BrokerCfg brokerCfg,
      final ActorSchedulingService schedulingService,
      final TimerClock clock,
      final MessagingService messagingService,
      final BeanFactory beanFactory,
      final SinksConfig sinksConfig,
      final JobStreamDispatcher jobStreamDispatcher,
      final CommandApiServiceImpl commandApiService,
      final ClusterCommunicationService communicationService,
      final ClusterTopologyService topologyService) {
    this.partitionSource = partitionSource;
    this.partitionCommunicationService = partitionCommunicationService;
    this.clock = clock;
    this.sinksConfig = sinksConfig;
    this.beanFactory = beanFactory;
    this.messagingService = messagingService;
    this.brokerCfg = brokerCfg;
    this.schedulingService = schedulingService;
    this.snapshotProvider = snapshotProvider;
    this.meterRegistry = meterRegistry;
    this.jobStreamDispatcher = jobStreamDispatcher;
    this.commandApiService = commandApiService;
    this.communicationService = communicationService;
    this.topologyService = topologyService;
    this.raftPartition = raftPartition;
    maxFragmentSize = (int) brokerCfg.getRaft().getMaxMessageSizeInBytes();
  }

  @Override
  public ConcurrencyControl getConcurrencyControl() {
    return concurrencyControl;
  }

  @Override
  public void setConcurrencyControl(final ConcurrencyControl concurrencyControl) {
    this.concurrencyControl = concurrencyControl;
  }

  @Override
  public long getCurrentTerm() {
    return currentTerm;
  }

  @Override
  public void setCurrentTerm(final long currentTerm) {
    this.currentTerm = currentTerm;
  }

  @Override
  public RaftServer.Role getCurrentRole() {
    return currentRole;
  }

  public void setEventLog(final EventLog eventLog) {
    this.eventLog = eventLog;
  }

  public EventLog getEventLog() {
    return eventLog;
  }

  public BusinessRaftEventStore getEventStore() {
    return logStorage;
  }

  public void setEventStore(final BusinessRaftEventStore logStorage) {
    this.logStorage = logStorage;
  }

  public RaftPartition getRaftPartition() {
    return raftPartition;
  }

  public PartitionId getRaftPartitionId() {
    return raftPartition.id();
  }

  public CommandApiServiceImpl getCommandApiService() {
    return commandApiService;
  }

  public ClusterTopologyService getTopologyService() {
    return topologyService;
  }

  public ClusterCommunicationService getCommunicationService() {
    return communicationService;
  }

  /** 分区时钟（引擎与定时调度共用） */
  public TimerClock getTimerClock() {
    return clock;
  }

  public InterPartitionCommandSenderService getPartitionCommandSender() {
    return partitionCommandSender;
  }

  public void setPartitionCommandSender(final InterPartitionCommandSenderService sender) {
    partitionCommandSender = sender;
  }

  public InterPartitionCommandReceiverActor getPartitionCommandReceiver() {
    return partitionCommandReceiver;
  }

  public void setPartitionCommandReceiver(final InterPartitionCommandReceiverActor receiver) {
    partitionCommandReceiver = receiver;
  }

  public EngineProcessService getEngineProcessService() {
    return engineProcessService;
  }

  public void setEngineProcessService(final EngineProcessService engineProcessService) {
    this.engineProcessService = engineProcessService;
  }

  /** broker 层 job 派发能力服务（引擎侧装配与 leader 绑定使用） */
  public JobStreamDispatcher getJobStreamDispatcher() {
    return jobStreamDispatcher;
  }

  public SinksConfig getSinksConfig() {
    return sinksConfig;
  }

  public int getMaxFragmentSize() {
    return maxFragmentSize;
  }

  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public RaftSnapshotProvider<KvStore<BusinessRepositoryColumnFamilies>> getSnapshotProvider() {
    return snapshotProvider;
  }

  public void setRocksdb(final KvStore<BusinessRepositoryColumnFamilies> rocksdb) {
    this.rocksdb = rocksdb;
  }

  public KvStore<BusinessRepositoryColumnFamilies> getRocksdb() {
    return rocksdb;
  }

  public void setRepositoryFactory(
      final RocksdbBusinessRepositoryFactory rocksdbRepositoryFactory) {
    this.rocksdbRepositoryFactory = rocksdbRepositoryFactory;
  }

  public RocksdbBusinessRepositoryFactory getRepositoryFactory() {
    return rocksdbRepositoryFactory;
  }

  public ActorSchedulingService getSchedulingService() {
    return schedulingService;
  }

  public void setRepositoryProcessService(final BusinessRepositoryProcessService applierService) {
    this.applierService = applierService;
  }

  public BusinessRepositoryProcessService getRepositoryProcessService() {
    return applierService;
  }

  public SinkService getSinkService() {
    return sinkService;
  }

  public RaftPartitionSource getRaftPartitionSource() {
    return partitionSource;
  }

  public void setSinkService(final SinkService sinkService) {
    this.sinkService = sinkService;
  }

  public PartitionMessagingService getPartitionMessagingService() {
    return partitionCommunicationService;
  }

  @Override
  public void setCurrentRole(final RaftServer.Role currentRole) {
    this.currentRole = currentRole;
  }
}
