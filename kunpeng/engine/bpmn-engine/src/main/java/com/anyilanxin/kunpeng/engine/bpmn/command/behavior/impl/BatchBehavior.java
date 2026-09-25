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

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.BatchBusinessType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.Optional;

/**
 * 批量操作行为：批量元素的处理语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchBehavior {
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBatchRepository batch;

  public BatchBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    batch = repository.batchRepository();
  }

  public void createProcessInstanceTerminatedBatch(
      final ProcessInstanceLifeCycle lifeCycle,
      final ProcessInstanceRecord value,
      final long requestId,
      final long operationReference,
      final long batchOperationReference) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(writer.nextCurrentSourceKey(value.getProcessInstanceId()));
    batchRecord.setRequestId(requestId);

    batchRecord.setBatchValueType(ValueType.PROCESS_INSTANCE);
    batchRecord.setBatchLifeCycle(lifeCycle);
    batchRecord.setBatchBusinessId(value.getProcessInstanceId());
    batchRecord.setBatchType(BatchBusinessType.PROCESS_INSTANCE);

    batchRecord.setBatchAfterValueType(ValueType.PROCESS_INSTANCE);
    batchRecord.setBatchAfterLifeCycle(lifeCycle);
    batchRecord.setBatchAfterBusinessId(value.getProcessInstanceId());
    batchRecord.setBatchAfterType(BatchBusinessType.PROCESS_INSTANCE);
    batchRecord.setBatchAfterBatchOperationReference(batchOperationReference);
    batchRecord.setBatchAfterOperationReference(operationReference);
    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_TERMINATE,
        requestId,
        batchRecord);
  }

  public void createActivityTerminatedBatch(
      final ActivityContent activityContext, final ActivityInstanceLifeCycle lifeCycle) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    batchRecord.setRequestId(activityContext.getRequestId());

    batchRecord.setBatchValueType(ValueType.ACTIVITY);
    batchRecord.setBatchLifeCycle(lifeCycle);
    batchRecord.setBatchBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchType(BatchBusinessType.ACTIVITY_INSTANCE);

    batchRecord.setBatchAfterValueType(ValueType.ACTIVITY);
    batchRecord.setBatchAfterLifeCycle(lifeCycle);
    batchRecord.setBatchAfterBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchAfterType(BatchBusinessType.ACTIVITY_INSTANCE);
    batchRecord.setBatchAfterBatchOperationReference(activityContext.getBatchOperationReference());
    batchRecord.setBatchAfterOperationReference(activityContext.getOperationReferenceKey());
    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_TERMINATE,
        activityContext.getRequestId(),
        batchRecord);
  }

  public void createActivityDirectTerminatedBatch(
      final long requestId,
      final ActivityInstanceRecord batchActivityInstanceRecord,
      final ValueLifeCycle batchLifeCycle,
      final ActivityInstanceRecord batchAfterActivityInstanceRecord,
      final ValueLifeCycle batchAfterLifeCycle) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(
        writer.nextCurrentSourceKey(batchAfterActivityInstanceRecord.getProcessInstanceId()));
    batchRecord.setRequestId(requestId);

    batchRecord.setBatchValueType(ValueType.ACTIVITY);
    batchRecord.setBatchLifeCycle(batchLifeCycle);
    batchRecord.setBatchBusinessId(batchActivityInstanceRecord.getActivityInstanceId());
    batchRecord.setBatchType(BatchBusinessType.ACTIVITY_INSTANCE);

    batchRecord.setBatchAfterValueType(ValueType.ACTIVITY);
    batchRecord.setBatchAfterLifeCycle(batchAfterLifeCycle);
    batchRecord.setBatchAfterBusinessId(batchAfterActivityInstanceRecord.getActivityInstanceId());
    batchRecord.setBatchAfterType(BatchBusinessType.ACTIVITY_INSTANCE);
    batchRecord.setBatchAfterBatchOperationReference(-1);
    batchRecord.setBatchAfterOperationReference(-1);
    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_DIRECT_TERMINATE,
        requestId,
        batchRecord);
  }

  public void handleTerminatedBatchReference(final ActivityContent activityContext) {
    final Optional<ProcessInstanceBatchRecord> query =
        batch.query(activityContext.getBatchOperationReference());
    query.ifPresent(
        batchRecord ->
            writer.addCommand(
                batchRecord.getBatchId(),
                ProcessInstanceBatchState.BATCH_COMPLETE,
                batchRecord.getRequestId(),
                batchRecord));
  }

  public void handleTerminatedBatchReference(final long batchOperationReference) {
    final Optional<ProcessInstanceBatchRecord> query = batch.query(batchOperationReference);
    query.ifPresent(
        batchRecord ->
            writer.addCommand(
                batchRecord.getBatchId(),
                ProcessInstanceBatchState.BATCH_COMPLETED,
                batchRecord.getRequestId(),
                batchRecord));
  }

  @SuppressWarnings("rawtypes")
  public UnifiedRecordValue getBatchAfterRecord(final ProcessInstanceBatchRecord record) {
    final UnifiedRecordValue value;
    final BatchBusinessType correlationType = record.getBatchAfterType();
    if (correlationType == BatchBusinessType.ACTIVITY_INSTANCE) {
      value = activityInstance.getRecord(record.getBatchAfterBusinessId());
    } else {
      value = processInstance.getRecord(record.getBatchAfterBusinessId());
    }
    return value;
  }
}
