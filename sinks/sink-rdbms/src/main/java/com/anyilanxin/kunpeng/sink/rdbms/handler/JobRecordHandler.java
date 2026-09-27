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
package com.anyilanxin.kunpeng.sink.rdbms.handler;

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.JOB;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.JobDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.JsonValues;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 任务（Job）：创建保存全量行（值自带 {@code start_time}），重试/超时/完成等事件只更新可变列； 全局/本地变量快照序列化为 JSON 列。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(
          JobLifeCycle.CREATED,
          JobLifeCycle.UPDATED,
          JobLifeCycle.REFUSED,
          JobLifeCycle.COMPLETED,
          JobLifeCycle.TIMED_OUT);

  @Override
  public ValueType valueType() {
    return ValueType.JOB;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final JobRecordValue value = (JobRecordValue) record.getValue();
    if (record.getValueState() == JobLifeCycle.CREATED) {
      final var model = new JobDbModel();
      model.setJobId(value.getJobId());
      model.setJobType(value.getJobType());
      model.setJobKind(nameOf(value.getJobKind()));
      model.setState(nameOf(value.getState()));
      model.setRetries(value.getRetries());
      model.setRetryBackoff(value.getRetryBackOff());
      model.setPriority(value.getPriority());
      model.setDueTime(RecordModelHandler.at(value.getDueDate()));
      model.setLockOwner(value.getLockOwner());
      model.setLockExpireTime(RecordModelHandler.at(value.getLockExpireTime()));
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setActivityInstanceId(value.getActivityInstanceId());
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setActivityKey(value.getActivityDefinitionKey());
      model.setTaskId(value.getTaskId() > 0 ? value.getTaskId() : null);
      model.setIncidentId(value.getIncidentId() > 0 ? value.getIncidentId() : null);
      model.setDeniedReason(value.getDeniedReason());
      model.setStartTime(RecordModelHandler.at(value.getStartTime()));
      model.setEndTime(RecordModelHandler.at(value.getEndTime()));
      model.setDuration(value.getDuration() > 0 ? value.getDuration() : null);
      model.setVariablesJson(JsonValues.toJson(value.getVariables()));
      model.setLocalVariablesJson(JsonValues.toJson(value.getLocalVariables()));
      model.setRevision(value.getRev());
      model.setResourceId(record.getResourceId());
      buffer.offer(RowChange.insert(JOB, List.of(value.getJobId()), model));
      return;
    }
    final var model = new JobDbModel();
    model.setJobId(value.getJobId());
    model.setState(nameOf(value.getState()));
    model.setRetries(value.getRetries());
    model.setRetryBackoff(value.getRetryBackOff());
    model.setPriority(value.getPriority());
    model.setDueTime(RecordModelHandler.at(value.getDueDate()));
    model.setLockOwner(value.getLockOwner());
    model.setLockExpireTime(RecordModelHandler.at(value.getLockExpireTime()));
    model.setIncidentId(value.getIncidentId() > 0 ? value.getIncidentId() : null);
    model.setDeniedReason(value.getDeniedReason());
    model.setEndTime(RecordModelHandler.at(value.getEndTime()));
    model.setDuration(value.getDuration() > 0 ? value.getDuration() : null);
    model.setVariablesJson(JsonValues.toJson(value.getVariables()));
    model.setLocalVariablesJson(JsonValues.toJson(value.getLocalVariables()));
    model.setRevision(value.getRev());
    buffer.offer(RowChange.update(JOB, List.of(value.getJobId()), model));
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
