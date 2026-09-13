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
package com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition;

import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionContent;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition.logstorage.BusinessRaftEventStore;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 业务分区状态迁移（transition）的内容载体，承载迁移过程中的并发控制、当前任期（term）与当前角色信息。
 *
 * @author zxuanhong
 * @since
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

  private ConcurrencyControl concurrencyControl;
  private long currentTerm;
  private RaftServer.Role currentRole;
  private EventLog eventLog;
  private BusinessRaftEventStore logStorage;

  public BusinessTransitionContent(
      final RaftSnapshotProvider<KvStore<BusinessRepositoryColumnFamilies>> snapshotProvider,
      final MeterRegistry meterRegistry,
      final RaftPartition raftPartition,
      final BrokerCfg brokerCfg,
      final ActorSchedulingService schedulingService,
      final TimerClock clock,
      final MessagingService messagingService) {
    this.clock = clock;
    this.messagingService = messagingService;
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

  public int getMaxFragmentSize() {
    return maxFragmentSize;
  }

  public BrokerCfg getBrokerCfg() {
    return brokerCfg;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  @Override
  public void setCurrentRole(final RaftServer.Role currentRole) {
    this.currentRole = currentRole;
  }
}
