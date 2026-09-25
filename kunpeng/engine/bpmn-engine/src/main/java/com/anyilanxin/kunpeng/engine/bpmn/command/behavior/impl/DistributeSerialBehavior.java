package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeSerialProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.ImmutableDistributeSerialRepository;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialBehavior {
  private final LogEventWriter writer;
  private final InterPartitionCommandSender commandSender;
  private final ImmutableDistributeSerialRepository distributeSerial;

  public DistributeSerialBehavior(final LogEventWriter writer) {
    this.writer = writer;
    commandSender = writer.getCommandSender();
    final ImmutableBusinessRepository repository = writer.getRepository();
    distributeSerial = repository.distributeSerialRepository();
  }

  public void addDistributeSerial(
      final LogEventDistributeSerialProcessor serialProcessor,
      final long distributeRecordId,
      final boolean needDistributeAckConfirm,
      final boolean needDistributeCompleteConfirm,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    final RecordMetadata metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(DistributeSerialLifeCycle.CREATE_DISTRIBUTE)
        .recordType(RecordType.COMMAND)
        .valueType(ValueType.DISTRIBUTE_SERIAL)
        .recordVersion(1)
        .brokerVersion(LogEventWriter.BROKER_VERSION)
        .batchOperationReference(-1)
        .operationReference(-1)
        .requestId(requestId);
    final DistributeSerialRecord distributeRecord = new DistributeSerialRecord();
    distributeRecord.setDistributeRecordValueType(
        serialProcessor.processRecordDistributeLifeCycle().getValueType());
    distributeRecord.setDistributeRecordLifeCycle(
        serialProcessor.processRecordDistributeLifeCycle());
    distributeRecord.setDistributeRecordAckConfirmLifeCycle(
        serialProcessor.processRecordDistributeAckConfirmLifeCycle());
    distributeRecord.setNeedDistributeAckConfirm(needDistributeAckConfirm);
    distributeRecord.setDistributeRecordCompleteConfirmLifeCycle(
        serialProcessor.processRecordDistributeCompleteConfirmLifeCycle());
    distributeRecord.setNeedDistributeCompleteConfirm(needDistributeCompleteConfirm);
    distributeRecord.setDistributeRecordId(distributeRecordId);
    distributeRecord.setDistributeRecord(recordValue);
    writer.addCommandWrite(distributeRecord, metadata);
  }

  public <T extends UnifiedRecordValue> void distributeSerialOkAck(
      final LogEventDistributeSerialProcessor processor,
      final BusinessLogRecord<T> record,
      final UnifiedRecordValue recordValue) {
    final int resourceId = Protocol.decodeResourceId(record.getOperationReferenceKey());
    final DistributeSerialRecord distributeRecord = new DistributeSerialRecord();
    distributeRecord.setDistributeRecordValueType(
        processor.processRecordDistributeLifeCycle().getValueType());
    distributeRecord.setDistributeRecordLifeCycle(processor.processRecordDistributeLifeCycle());
    distributeRecord.setDistributeRecordAckConfirmLifeCycle(
        processor.processRecordDistributeAckConfirmLifeCycle());
    distributeRecord.setDistributeRecordCompleteConfirmLifeCycle(
        processor.processRecordDistributeCompleteConfirmLifeCycle());
    distributeRecord.setDistributeRecordId(record.getKey());
    distributeRecord.setDistributeId(record.getOperationReferenceKey());
    distributeRecord.setCurrentDistributeSourceId(writer.getSourceId());
    distributeRecord.setDistributeAckRecord(recordValue);
    distributeRecord.setDistributeAckSuccess(true);
    commandSender.sendCommand(
        resourceId,
        DistributeSerialLifeCycle.DISTRIBUTE_ACK,
        record.getOperationReferenceKey(),
        distributeRecord);
  }

  public <T extends UnifiedRecordValue> void distributeSerialFailAck(
      final LogEventDistributeSerialProcessor processor, final BusinessLogRecord<T> record) {
    final int resourceId = Protocol.decodeResourceId(record.getOperationReferenceKey());
    final DistributeSerialRecord distributeRecord = new DistributeSerialRecord();
    distributeRecord.setDistributeRecordValueType(
        processor.processRecordDistributeLifeCycle().getValueType());
    distributeRecord.setDistributeRecordLifeCycle(processor.processRecordDistributeLifeCycle());
    distributeRecord.setDistributeRecordAckConfirmLifeCycle(
        processor.processRecordDistributeAckConfirmLifeCycle());
    distributeRecord.setDistributeRecordCompleteConfirmLifeCycle(
        processor.processRecordDistributeCompleteConfirmLifeCycle());
    distributeRecord.setDistributeRecordId(record.getKey());
    distributeRecord.setDistributeId(record.getOperationReferenceKey());
    distributeRecord.setCurrentDistributeSourceId(writer.getSourceId());
    distributeRecord.setDistributeAckSuccess(false);
    commandSender.sendCommand(
        resourceId,
        DistributeSerialLifeCycle.DISTRIBUTE_ACK,
        record.getOperationReferenceKey(),
        distributeRecord);
  }

  public void distributeSerial(final DistributeSerialRecord record, final int sourceId) {
    commandSender.sendCommand(
        sourceId,
        record.getDistributeRecordLifeCycle(),
        record.getDistributeRecordId(),
        record.getDistributeId(),
        record.getDistributeRecord());
  }

  public <T extends UnifiedRecordValue> void distributeSerialConfirmResult(
      final BusinessLogRecord<T> record, final DistributeSerialLifeCycle lifeCycle) {
    final long operationReferenceKey = record.getOperationReferenceKey();
    final DistributeSerialRecord distribute = distributeSerial.getDistribute(operationReferenceKey);
    writer.addCommand(operationReferenceKey, lifeCycle, -1, distribute);
  }
}
