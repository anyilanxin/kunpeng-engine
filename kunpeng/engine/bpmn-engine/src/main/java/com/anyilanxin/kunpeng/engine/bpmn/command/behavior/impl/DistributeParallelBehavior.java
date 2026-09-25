package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelBehavior {
  private final LogEventWriter writer;
  private final InterPartitionCommandSender commandSender;

  public DistributeParallelBehavior(final LogEventWriter writer) {
    this.writer = writer;
    commandSender = writer.getCommandSender();
  }

  public void addDistributeParallel(
      final long key,
      final ValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    addDistributeParallel(key, lifeCycle, ValueLifeCycle.UNKNOWN, requestId, recordValue);
  }

  public void addDistributeParallel(
      final long key,
      final ValueLifeCycle lifeCycle,
      final ValueLifeCycle afterValueState,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    final RecordMetadata metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(DistributeParallelLifeCycle.CREATE_DISTRIBUTE)
        .recordType(RecordType.COMMAND)
        .valueType(ValueType.DISTRIBUTE_PARALLEL)
        .recordVersion(1)
        .brokerVersion(LogEventWriter.BROKER_VERSION)
        .batchOperationReference(-1)
        .operationReference(-1)
        .requestId(requestId);
    final DistributeParallelRecord distributeRecord = new DistributeParallelRecord();
    distributeRecord.setDistributeRecordValueType(lifeCycle.getValueType());
    distributeRecord.setDistributeRecordLifeCycle(lifeCycle);
    distributeRecord.setFollowUpLifeCycle(afterValueState);
    distributeRecord.setDistributeRecordId(key);
    distributeRecord.setDistributeRecord(recordValue);
    writer.addCommandWrite(distributeRecord, metadata);
  }

  public <T extends UnifiedRecordValue> void distributeParallelAck(
      final BusinessLogRecord<T> record) {
    final int resourceId = Protocol.decodeResourceId(record.getOperationReferenceKey());
    final DistributeParallelRecord distributeRecord = new DistributeParallelRecord();
    distributeRecord.setDistributeRecordLifeCycle(record.getValueState());
    distributeRecord.setDistributeRecordValueType(record.getValueType());
    distributeRecord.setDistributeRecordId(record.getKey());
    distributeRecord.setDistributeId(record.getOperationReferenceKey());
    distributeRecord.setCurrentDistributeIndex(writer.getSourceId());
    commandSender.sendCommand(
        resourceId,
        DistributeParallelLifeCycle.DISTRIBUTE_ACK,
        record.getOperationReferenceKey(),
        distributeRecord);
  }

  public <T extends UnifiedRecordValue> void distributeParallelAfterAck(
      final BusinessLogRecord<T> record) {
    final int resourceId = Protocol.decodeResourceId(record.getOperationReferenceKey());
    final DistributeParallelRecord distributeRecord = new DistributeParallelRecord();
    distributeRecord.setDistributeRecordLifeCycle(record.getValueState());
    distributeRecord.setDistributeRecordValueType(record.getValueType());
    distributeRecord.setDistributeRecordId(record.getKey());
    distributeRecord.setDistributeId(record.getOperationReferenceKey());
    distributeRecord.setCurrentDistributeIndex(writer.getSourceId());
    commandSender.sendCommand(
        resourceId,
        DistributeParallelLifeCycle.DISTRIBUTE_AFTER_ACK,
        record.getOperationReferenceKey(),
        distributeRecord);
  }

  public void distributeParallel(final DistributeParallelRecord record, final int sourceId) {
    commandSender.sendCommand(
        sourceId,
        record.getDistributeRecordLifeCycle(),
        record.getDistributeRecordId(),
        record.getDistributeId(),
        record.getDistributeRecord());
  }

  public void distributeParallelAfter(final DistributeParallelRecord record, final int sourceId) {
    commandSender.sendCommand(
        sourceId,
        record.getFollowUpLifeCycle(),
        record.getDistributeRecordId(),
        record.getDistributeId(),
        record.getDistributeRecord());
  }
}
