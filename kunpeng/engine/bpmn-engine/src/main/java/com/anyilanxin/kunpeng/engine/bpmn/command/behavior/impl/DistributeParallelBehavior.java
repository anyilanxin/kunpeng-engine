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
    // 摘除编号与索引列族的计账体系一致：目标列表存分区 id，ACK 必须回填分区 id（ getSourceId 是
    // source 编号，两个体系错位会让 ackDistribute 永远摘不掉目标）
    distributeRecord.setCurrentDistributeIndex(writer.getPartitionId());
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
    // 同 distributeParallelAck：摘除编号与计账体系（分区 id）保持一致
    distributeRecord.setCurrentDistributeIndex(writer.getPartitionId());
    commandSender.sendCommand(
        resourceId,
        DistributeParallelLifeCycle.DISTRIBUTE_AFTER_ACK,
        record.getOperationReferenceKey(),
        distributeRecord);
  }

  public void distributeParallel(final DistributeParallelRecord record, final int partitionId) {
    final int partitionSource = commandSender.getPartitionSource(partitionId);
    commandSender.sendCommand(
        partitionSource,
        record.getDistributeRecordLifeCycle(),
        record.getDistributeRecordId(),
        record.getDistributeId(),
        record.getDistributeRecord());
  }

  public void distributeParallelAfter(
      final DistributeParallelRecord record, final int partitionId) {
    final int partitionSource = commandSender.getPartitionSource(partitionId);
    commandSender.sendCommand(
        partitionSource,
        record.getFollowUpLifeCycle(),
        record.getDistributeRecordId(),
        record.getDistributeId(),
        record.getDistributeRecord());
  }
}
