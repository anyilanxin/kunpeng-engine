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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionContent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.DispatchProcessService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstorage.AdminRaftEventStore;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryProcessService;
import com.anyilanxin.kunpeng.repository.admin.RocksdbAdminRepositoryFactory;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 管理分区的分区转换（transition）内容，持有并发控制器、当前任期（term）与当前角色等转换期状态。
 *
 * @author zxuanhong
 * @since
 */
public class AdminTransitionContent
    implements TransitionContent<KvStore<AdminRepositoryColumnFamilies>> {
  private final RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> snapshotProvider;
  private final MeterRegistry meterRegistry;
  private final RaftPartition raftPartition;
  private final BrokerCfg brokerCfg;
  private final int maxFragmentSize;
  private final ActorSchedulingService schedulingService;
  private final CommandApiService commandApiHandle;
  private final TimerClock clock;
  private final MessagingService messagingService;
  private final ClusterMetaStore clusterMetaStore;
  private final ClusterMembershipService membershipService;
  private final ClusterDispatchClient dispatchClient;
  private final ClusterTopologyService clusterTopologyService;

  private long currentTerm;
  private RaftServer.Role currentRole;
  private KvStore<AdminRepositoryColumnFamilies> rocksdb;
  private RocksdbAdminRepositoryFactory rocksdbRepositoryFactory;
  private EventLog eventLog;
  private AdminRaftEventStore logStorage;
  private AdminRepositoryProcessService applierService;
  private DispatchProcessService dispatchProcessService;
  private ConcurrencyControl concurrencyControl;

  public AdminTransitionContent(
      final ClusterMetaStore clusterMetaStore,
      final RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> snapshotProvider,
      final MeterRegistry meterRegistry,
      final RaftPartition raftPartition,
      final BrokerCfg brokerCfg,
      final ActorSchedulingService schedulingService,
      final CommandApiService commandApiHandle,
      final TimerClock clock,
      final MessagingService messagingService,
      final ClusterMembershipService membershipService,
      final ClusterDispatchClient dispatchClient,
      final ClusterTopologyService clusterTopologyService) {
    this.clock = clock;
    this.dispatchClient = dispatchClient;
    this.membershipService = membershipService;
    this.clusterTopologyService = clusterTopologyService;
    this.clusterMetaStore = clusterMetaStore;
    this.messagingService = messagingService;
    this.commandApiHandle = commandApiHandle;
    this.brokerCfg = brokerCfg;
    this.schedulingService = schedulingService;
    this.snapshotProvider = snapshotProvider;
    this.meterRegistry = meterRegistry;
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

  public RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> getSnapshotProvider() {
    return snapshotProvider;
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

  @Override
  public void setCurrentRole(final RaftServer.Role currentRole) {
    this.currentRole = currentRole;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public void setRocksdb(final KvStore<AdminRepositoryColumnFamilies> rocksdb) {
    this.rocksdb = rocksdb;
  }

  public KvStore<AdminRepositoryColumnFamilies> getRocksdb() {
    return rocksdb;
  }

  public void setRepositoryFactory(final RocksdbAdminRepositoryFactory rocksdbRepositoryFactory) {
    this.rocksdbRepositoryFactory = rocksdbRepositoryFactory;
  }

  public RocksdbAdminRepositoryFactory getRepositoryFactory() {
    return rocksdbRepositoryFactory;
  }

  public void setEventLog(final EventLog eventLog) {
    this.eventLog = eventLog;
  }

  public EventLog getEventLog() {
    return eventLog;
  }

  public AdminRaftEventStore getEventStore() {
    return logStorage;
  }

  public void setEventStore(final AdminRaftEventStore logStorage) {
    this.logStorage = logStorage;
  }

  public RaftPartition getRaftPartition() {
    return raftPartition;
  }

  public PartitionId getRaftPartitionId() {
    return raftPartition.id();
  }

  public int getMaxFragmentSize() {
    return maxFragmentSize;
  }

  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  public ActorSchedulingService getSchedulingService() {
    return schedulingService;
  }

  public void setRepositoryProcessService(final AdminRepositoryProcessService applierService) {
    this.applierService = applierService;
  }

  public AdminRepositoryProcessService getRepositoryProcessService() {
    return applierService;
  }

  public void setDispatchProcessService(final DispatchProcessService dispatchProcessService) {
    this.dispatchProcessService = dispatchProcessService;
  }

  public DispatchProcessService getDispatchProcessService() {
    return dispatchProcessService;
  }

  public CommandApiService getCommandApiService() {
    return commandApiHandle;
  }

  public TimerClock getStreamClock() {
    return clock;
  }

  public MessagingService getMessagingService() {
    return messagingService;
  }

  public ClusterMetaStore getClusterMetaStore() {
    return clusterMetaStore;
  }

  public ClusterMembershipService getMembershipService() {
    return membershipService;
  }

  public ClusterDispatchClient getDispatchClient() {
    return dispatchClient;
  }

  public ClusterTopologyService getClusterTopologyService() {
    return clusterTopologyService;
  }
}
