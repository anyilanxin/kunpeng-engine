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
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
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
  private final ImmutableUserTaskRepository userTask;

  public BatchBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    batch = repository.batchRepository();
    userTask = repository.userTaskRepository();
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

  /**
   * 并行多实例创建批：body 激活求值后挂载，执行器每轮创建一个子实例并自排（游标=已创建数， 名单快照与计数在 body 变量），被 maxBatch
   * 自然分段。创建类批无完成回调（batchAfter 不设置），耗尽即 BATCH_COMPLETED 终态。
   */
  public void createMiCreateBatch(final ActivityContent activityContext) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    batchRecord.setRequestId(activityContext.getRequestId());

    batchRecord.setBatchValueType(ValueType.ACTIVITY);
    batchRecord.setBatchLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    batchRecord.setBatchBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchType(BatchBusinessType.ACTIVITY_INSTANCE);
    batchRecord.setCurrentExecutionId(0);

    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_MI_CREATE,
        activityContext.getRequestId(),
        batchRecord);
  }

  /** 并行加签创建批：前/后加签并行展开挂载（名单载体按相位：BEFORE=父任务行名单、AFTER=活动实例变量）， 执行器每轮创建一个加签任务并自排，无 batchAfter。 */
  public void createTaskCreateBatch(final ActivityContent activityContext) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    batchRecord.setRequestId(activityContext.getRequestId());

    batchRecord.setBatchValueType(ValueType.ACTIVITY);
    batchRecord.setBatchLifeCycle(ActivityInstanceLifeCycle.UPDATING);
    batchRecord.setBatchBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchType(BatchBusinessType.USER_TASK);
    batchRecord.setCurrentExecutionId(0);

    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_TASK_CREATE,
        activityContext.getRequestId(),
        batchRecord);
  }

  /**
   * 加签整组终止批：活动终止时名下在途任务（原任务+加签任务）逐个终止（游标=已处理 taskId， 终态任务删行天然过滤），batchAfter 回活动 TERMINATED
   * 续终止链（外层引用透传，流程取消等外层批继续闭环）。
   */
  public void createTaskTerminateBatch(final ActivityContent activityContext) {
    final ProcessInstanceBatchRecord batchRecord = new ProcessInstanceBatchRecord();
    batchRecord.setBatchId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()));
    batchRecord.setRequestId(activityContext.getRequestId());

    batchRecord.setBatchValueType(ValueType.USER_TASK);
    batchRecord.setBatchLifeCycle(ActivityInstanceLifeCycle.TERMINATING);
    batchRecord.setBatchBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchType(BatchBusinessType.USER_TASK);
    batchRecord.setCurrentExecutionId(-1);

    batchRecord.setBatchAfterValueType(ValueType.ACTIVITY);
    batchRecord.setBatchAfterLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    batchRecord.setBatchAfterBusinessId(activityContext.getActivityInstanceId());
    batchRecord.setBatchAfterType(BatchBusinessType.ACTIVITY_INSTANCE);
    batchRecord.setBatchAfterBatchOperationReference(activityContext.getBatchOperationReference());
    batchRecord.setBatchAfterOperationReference(activityContext.getOperationReferenceKey());
    writer.addCommand(
        batchRecord.getBatchId(),
        ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE,
        activityContext.getRequestId(),
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
    } else if (correlationType == BatchBusinessType.USER_TASK) {
      value = userTask.getRecord(record.getBatchAfterBusinessId());
    } else {
      value = processInstance.getRecord(record.getBatchAfterBusinessId());
    }
    return value;
  }
}
