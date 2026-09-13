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
package com.anyilanxin.kunpeng.cluster.dispatch.api;

import static com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter.BROKER_VERSION;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decodeNodeSourceApply;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decodePartitionExecutionAck;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.CLUSTER_DISPATCH_TOPIC_ACK;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.CLUSTER_NODE_SOURCE_TOPIC;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.NodeSourceApplyRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionExecutionAckRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryFactory;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class ClusterDispatchService {
  private final MessagingService messagingService;
  private final ConcurrencyControl concurrencyControl;
  private EventLogWriter logStreamWriter;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private AdminImmutableRepository repository;
  private ImmutableRepositoryAdmin repositoryAdmin;
  private ImmutableRepositoryBusiness repositoryBusiness;
  private TransactionContext context;

  public ClusterDispatchService(
      final MessagingService messagingService, final ConcurrencyControl concurrencyControl) {
    this.messagingService = messagingService;
    this.concurrencyControl = concurrencyControl;
  }

  public ActorFuture<Void> start(
      final EventLogWriter logStreamWriter, final AdminRepositoryFactory repositoryFactory) {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          this.logStreamWriter = logStreamWriter;
          final AdminRepository repository = repositoryFactory.create();
          this.repository = repository;
          context = repository.getContext();
          repositoryAdmin = repository.repositoryAdmin();
          repositoryBusiness = repository.repositoryBusiness();
          messagingService.registerHandler(
              CLUSTER_DISPATCH_TOPIC_ACK, this::handleDispatchAck, concurrencyControl);
          messagingService.registerHandler(
              CLUSTER_NODE_SOURCE_TOPIC, this::handleNodeSource, concurrencyControl);
          future.complete(null);
        });
    return future;
  }

  public ActorFuture<Void> stop() {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          messagingService.unregisterHandler(CLUSTER_DISPATCH_TOPIC_ACK);
          messagingService.unregisterHandler(CLUSTER_NODE_SOURCE_TOPIC);
          repository = null;
          repositoryAdmin = null;
          repositoryBusiness = null;
          context = null;
          future.complete(null);
        });
    return future;
  }

  private void handleDispatchAck(final Address address, final byte[] bytes) {
    final PartitionExecutionAckRecord ackRecord = decodePartitionExecutionAck(bytes);
    final AppendResult result = writeCommand(ackRecord);
    if (result instanceof final AppendResult.Rejected rejected) {
      logWriteFailure(ackRecord, rejected);
    }
  }

  private void handleNodeSource(final Address address, final byte[] bytes) {
    final NodeSourceApplyRecord applyRecord = decodeNodeSourceApply(bytes);
    final AppendResult result = writeCommand(applyRecord);
    if (result instanceof final AppendResult.Rejected rejected) {
      logWriteFailure(applyRecord, rejected);
    }
  }

  private void logWriteFailure(
      final PartitionExecutionAckRecord ackRecord, final AppendResult.Rejected failure) {
    final AdminValueType valueType;
    final CommandValueLifeCycle lifeCycle;
    if (ackRecord.getPartitionType() == PartitionType.ADMIN) {
      valueType = AdminValueType.ADMIN_DISPATCH_EXECUTION;
      lifeCycle = AdminDispatchPlanExecutionLifeCycle.ACKNOWLEDGE;
    } else {
      valueType = AdminValueType.BUSINESS_DISPATCH_EXECUTION;
      lifeCycle = BusinessDispatchPlanExecutionLifeCycle.ACKNOWLEDGE;
    }
    LOGGER.error(
        "Failed to write command {} {} from {} to logstream (error = {})",
        valueType,
        lifeCycle,
        ackRecord.executionMemberId(),
        failure);
  }

  private AppendResult writeCommand(final NodeSourceApplyRecord record) {
    final UnifiedRecordValue recordValue;
    final AdminRecordMetadata metadata =
        new AdminRecordMetadata()
            .recordType(RecordType.COMMAND)
            .recordVersion(1)
            .brokerVersion(BROKER_VERSION)
            .valueLifeCycle(NodeSourceLifeCycle.APPLYING)
            .valueType(AdminValueType.NODE_SOURCE);

    final NodeSourceRecord recordMetadata = new NodeSourceRecord();
    recordMetadata.setMemberId(record.getMemberId());
    final AppendEntry appendEntry = RecordAppendEntryFactory.of(-1, metadata, recordMetadata);
    return logStreamWriter.tryAppend(WriteContext.INTER_PARTITION, appendEntry);
  }

  private void logWriteFailure(
      final NodeSourceApplyRecord applyRecord, final AppendResult.Rejected failure) {
    LOGGER.error(
        "Failed to write command {} {} from {} to logstream (error = {})",
        AdminValueType.NODE_SOURCE,
        NodeSourceLifeCycle.APPLYING,
        applyRecord.getMemberId(),
        failure);
  }

  private AppendResult writeCommand(final PartitionExecutionAckRecord record) {
    final UnifiedRecordValue recordValue;
    final AdminRecordMetadata recordMetadata;
    if (record.getPartitionType() == PartitionType.ADMIN) {
      recordValue = repositoryAdmin.getDispatchPlanExecution(record.getDispatchPlanExecutionId());
      recordMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(AdminDispatchPlanExecutionLifeCycle.ACKNOWLEDGE)
              .valueType(AdminValueType.ADMIN_DISPATCH_EXECUTION);
      // ack 的成败结果附着在命令值上传递，AcknowledgeProcessor 依据 dispatchExecutionState 分流
      if (recordValue instanceof final AdminDispatchPlanExecutionRecord execution) {
        execution.setDispatchExecutionState(
            record.isSuccess() ? DispatchExecutionState.SUCCEED : DispatchExecutionState.FAILED);
        execution.setDispatchMessage(record.getErrorMessage());
      }
    } else {
      recordValue =
          repositoryBusiness.getDispatchPlanExecution(
              record.getDispatchPlanId(), record.getDispatchPlanExecutionId());
      recordMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(BusinessDispatchPlanExecutionLifeCycle.ACKNOWLEDGE)
              .valueType(AdminValueType.BUSINESS_DISPATCH_EXECUTION);
      // ack 的成败结果附着在命令值上传递，AcknowledgeProcessor 依据 dispatchExecutionState 分流
      if (recordValue instanceof final BusinessDispatchPlanExecutionRecord execution) {
        execution.setDispatchExecutionState(
            record.isSuccess() ? DispatchExecutionState.SUCCEED : DispatchExecutionState.FAILED);
        execution.setDispatchMessage(record.getErrorMessage());
      }
    }
    if (recordValue == null) {
      return new AppendResult.Rejected(AppendResult.RejectionReason.INVALID_ARGUMENT);
    }
    final AppendEntry appendEntry =
        RecordAppendEntryFactory.of(
            record.getDispatchPlanExecutionId(), recordMetadata, recordValue);
    return logStreamWriter.tryAppend(WriteContext.INTER_PARTITION, appendEntry);
  }

  public void initAdmin(final PartitionMetadata metadata) {}
}
