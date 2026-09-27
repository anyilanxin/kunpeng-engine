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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.USER_TASK;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.UserTaskDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.JsonValues;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 用户任务：创建保存全量行（值自带 {@code start_time}），认领/转办/完成等事件只更新可变列；候选人序列化为 JSON 列。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class UserTaskRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(
          UserTaskLifeCycle.CREATED,
          UserTaskLifeCycle.UPDATED,
          UserTaskLifeCycle.ASSIGNEE,
          UserTaskLifeCycle.CLAIM,
          UserTaskLifeCycle.OWN,
          UserTaskLifeCycle.RESOLVE,
          UserTaskLifeCycle.DELEGATE,
          UserTaskLifeCycle.COMPLETING,
          UserTaskLifeCycle.COMPLETED,
          UserTaskLifeCycle.CANCELED,
          UserTaskLifeCycle.TERMINATED);

  @Override
  public ValueType valueType() {
    return ValueType.USER_TASK;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final UserTaskRecordValue value = (UserTaskRecordValue) record.getValue();
    if (record.getValueState() == UserTaskLifeCycle.CREATED) {
      final var model = new UserTaskDbModel();
      model.setTaskId(value.getTaskId());
      model.setActivityInstanceId(value.getActivityInstanceId());
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setBusinessKey(value.getBusinessKey());
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setTaskDefinitionKey(value.getTaskDefinitionKey());
      model.setTaskName(value.getTaskDefinitionName());
      model.setAssignee(value.getAssignee());
      model.setOwner(value.getOwner());
      model.setPriority(value.getPriority());
      model.setDueTime(RecordModelHandler.at(value.getDueDate()));
      model.setFollowUpTime(RecordModelHandler.at(value.getFollowUpDate()));
      model.setState(nameOf(value.getState()));
      model.setCandidateGroups(JsonValues.toJson(value.getCandidateGroups()));
      model.setCandidateUsers(JsonValues.toJson(value.getCandidateUsers()));
      model.setStartTime(RecordModelHandler.at(value.getStartTime()));
      model.setEndTime(RecordModelHandler.at(value.getEndTime()));
      model.setDuration(value.getDuration() > 0 ? value.getDuration() : null);
      model.setRevision(value.getRev());
      model.setResourceId(record.getResourceId());
      buffer.offer(RowChange.insert(USER_TASK, List.of(value.getTaskId()), model));
      return;
    }
    final var model = new UserTaskDbModel();
    model.setTaskId(value.getTaskId());
    model.setAssignee(value.getAssignee());
    model.setOwner(value.getOwner());
    model.setPriority(value.getPriority());
    model.setDueTime(RecordModelHandler.at(value.getDueDate()));
    model.setFollowUpTime(RecordModelHandler.at(value.getFollowUpDate()));
    model.setState(nameOf(value.getState()));
    model.setCandidateGroups(JsonValues.toJson(value.getCandidateGroups()));
    model.setCandidateUsers(JsonValues.toJson(value.getCandidateUsers()));
    model.setEndTime(RecordModelHandler.at(value.getEndTime()));
    model.setDuration(value.getDuration() > 0 ? value.getDuration() : null);
    model.setRevision(value.getRev());
    buffer.offer(RowChange.update(USER_TASK, List.of(value.getTaskId()), model));
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
