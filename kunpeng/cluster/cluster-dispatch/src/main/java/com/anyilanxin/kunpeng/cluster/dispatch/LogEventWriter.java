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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.BusinessRaftCfg;
import com.anyilanxin.kunpeng.configuration.broker.ManageRaftCfg;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.common.VersionInfo;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryFactory;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zxuanhong
 * @since
 */
public class LogEventWriter {
  // Broker 版本在启动时就已固定；解析 semver 字符串代价不小（正则 + 3 次 Integer.parseInt + 对象分配），
  // 因此只解析一次并缓存，而不是每次写事件都重新解析。
  public static final VersionInfo BROKER_VERSION = VersionInfo.parse(VersionUtil.getVersion());
  private final AdminRepository repository;
  private final ProcessingCollectSupplier collectSupplier;
  private final int sourceId;
  private final ImmutableRepositoryKey repositoryKey;
  private final List<Integer> agentSourceIds;
  private final MeterRegistry meterRegistry;
  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();
  private final TimerClock clock;
  private final List<SchedulerCheckerAware> checkerAwareList = new ArrayList<>();
  private final AdminRepositoryFactory repositoryFactory;
  private final ClusterMetaStore clusterMetaStore;
  private final ClusterMembershipService membershipService;
  private final ClusterDispatchClient dispatchClient;
  private final BrokerCfg brokerCfg;
  private final ClusterTopologyService clusterTopologyService;
  private DelayedDelayChecker delayChecker;

  public LogEventWriter(
      final AdminRepositoryFactory repositoryFactory,
      final TimerClock clock,
      final AdminRepository repository,
      final ProcessingCollectSupplier collectSupplier,
      final PartitionSourceMetadata partitionSourceMetadata,
      final MeterRegistry meterRegistry,
      final ClusterMetaStore clusterMetaStore,
      final ClusterMembershipService membershipService,
      final ClusterDispatchClient dispatchClient,
      final BrokerCfg brokerCfg,
      final ClusterTopologyService clusterTopologyService) {
    this.brokerCfg = brokerCfg;
    this.clusterTopologyService = clusterTopologyService;
    this.membershipService = membershipService;
    this.dispatchClient = dispatchClient;
    this.clusterMetaStore = clusterMetaStore;
    this.repository = repository;
    this.meterRegistry = meterRegistry;
    this.repositoryFactory = repositoryFactory;
    this.clock = clock;
    this.collectSupplier = collectSupplier;
    sourceId = partitionSourceMetadata.sourceId();
    agentSourceIds = new ArrayList<>(partitionSourceMetadata.agentSourceIds());

    initCheckerAware(repositoryFactory);
    repositoryKey = repository.repositoryKey();
  }

  private void initCheckerAware(final AdminRepositoryFactory repositoryFactory) {
    delayChecker = new DelayedDelayChecker(repositoryFactory.create().repositoryDelayed(), clock);
    checkerAwareList.add(delayChecker);
  }

  public void addCommand(
      final AdminValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    addCommand(-1, lifeCycle, requestId, recordValue);
  }

  public void addCommandWrite(
      final UnifiedRecordValue recordValue, final AdminRecordMetadata metadata) {
    collectSupplier.get().addCommandWrite(recordValue, metadata);
  }

  public void addCommand(
      final long key,
      final AdminValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    final AdminRecordMetadata metadata = new AdminRecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .operationReference(-1)
        .requestId(requestId);
    collectSupplier.get().addCommand(key, valueMapper.copyValue(lifeCycle, recordValue), metadata);
  }

  public void addCommand(
      final long key,
      final AdminValueLifeCycle lifeCycle,
      final long requestId,
      final long operationReference,
      final UnifiedRecordValue recordValue) {
    final AdminRecordMetadata metadata = new AdminRecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .operationReference(operationReference)
        .requestId(requestId);
    collectSupplier.get().addCommand(key, valueMapper.copyValue(lifeCycle, recordValue), metadata);
  }

  public void addSideEffect(final SideEffectProducer producer) {
    collectSupplier.get().addSideEffect(producer);
  }

  public void addEvent(
      final long key,
      final AdminValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    // addEvent（写日志）和 addState（应用到 repository）都只读取元数据字段，
    // 因此在单线程的引擎 actor 下共享同一个实例是安全的。
    final AdminRecordMetadata metadata = new AdminRecordMetadata();

    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.EVENT)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .operationReference(-1)
        .requestId(requestId);
    final UnifiedRecordValue value = valueMapper.copyValue(lifeCycle, recordValue);
    collectSupplier.get().addEvent(key, value, metadata);
    collectSupplier.get().addState(key, value, metadata);
  }

  public void addEvent(
      final AdminValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    addEvent(-1, lifeCycle, requestId, recordValue);
  }

  public void adResponse(
      final CommandApiValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    collectSupplier.get().adResponse(lifeCycle, requestId, recordValue);
  }

  public void adErrorResponse(final long requestId, final int code, final String message) {
    collectSupplier.get().adErrorResponse(requestId, code, message);
  }

  public AdminImmutableRepository getRepository() {
    return repository;
  }

  public long nextKey() {
    return repositoryKey.nextKey();
  }

  public TimerClock clock() {
    return clock;
  }

  public long millis() {
    return clock.millis();
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

  public DelayedDelayChecker getDelayChecker() {
    return delayChecker;
  }

  public ManageRaftCfg getAdminRaft() {
    return brokerCfg.getManage();
  }

  public BusinessRaftCfg getBusinessRaft() {
    return brokerCfg.getRaft();
  }

  public ClusterTopologyService getClusterTopologyService() {
    return clusterTopologyService;
  }

  List<SchedulerCheckerAware> schedulerCheckerAwares() {
    return checkerAwareList;
  }
}
