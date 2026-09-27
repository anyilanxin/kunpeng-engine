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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.PROCESS_INSTANCE;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.model.ProcessInstanceDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import java.util.List;
import java.util.Set;

/**
 * 流程实例：激活类事件保存全量行（含 {@code start_time}，取记录时间戳——实例值本身不携带时间）； 其余状态事件只更新可变列。 {@code start_time} 不在
 * update 语句里，任何方言下后续事件都不会覆盖它。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ProcessInstanceRecordHandler implements RecordModelHandler {

  private static final Set<ValueLifeCycle> START_EVENTS =
      Set.of(ProcessInstanceLifeCycle.ACTIVATING, ProcessInstanceLifeCycle.ACTIVATED);

  private static final Set<ValueLifeCycle> END_EVENTS =
      Set.of(
          ProcessInstanceLifeCycle.COMPLETED,
          ProcessInstanceLifeCycle.CANCELED,
          ProcessInstanceLifeCycle.TERMINATED);

  private static final Set<ValueLifeCycle> EXPORTABLE =
      Set.of(
          ProcessInstanceLifeCycle.ACTIVATING,
          ProcessInstanceLifeCycle.ACTIVATED,
          ProcessInstanceLifeCycle.SUSPENDED,
          ProcessInstanceLifeCycle.COMPLETING,
          ProcessInstanceLifeCycle.COMPLETED,
          ProcessInstanceLifeCycle.CANCEL,
          ProcessInstanceLifeCycle.CANCELED,
          ProcessInstanceLifeCycle.TERMINATING,
          ProcessInstanceLifeCycle.TERMINATED);

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_INSTANCE;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return EXPORTABLE.contains(lifecycle);
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final ProcessInstanceRecordValue value = (ProcessInstanceRecordValue) record.getValue();
    final var lifecycle = record.getValueState();
    if (START_EVENTS.contains(lifecycle)) {
      final var model = new ProcessInstanceDbModel();
      model.setProcessInstanceId(value.getProcessInstanceId());
      model.setParentProcessInstanceId(positive(value.getParentProcessInstanceId()));
      model.setRootProcessInstanceId(positive(value.getRootProcessInstanceId()));
      model.setBusinessKey(value.getBusinessKey());
      model.setProcessDefinitionId(value.getProcessDefinitionId());
      model.setDefinitionKey(value.getProcessDefinitionKey());
      model.setDefinitionName(value.getProcessDefinitionName());
      model.setStartUser(value.getStartUserId());
      model.setState(nameOf(value.getState()));
      model.setStartTime(RecordModelHandler.at(record.getTimestamp()));
      model.setRevision(value.getRev());
      model.setResourceId(record.getResourceId());
      buffer.offer(
          RowChange.insert(PROCESS_INSTANCE, List.of(value.getProcessInstanceId()), model));
      return;
    }
    // 终态与非终态状态变化共用 update：end_time 仅终态事件携带，其余事件保持为空
    final var model = new ProcessInstanceDbModel();
    model.setProcessInstanceId(value.getProcessInstanceId());
    model.setState(nameOf(value.getState()));
    model.setEndTime(
        END_EVENTS.contains(lifecycle) ? RecordModelHandler.at(record.getTimestamp()) : null);
    model.setRevision(value.getRev());
    buffer.offer(RowChange.update(PROCESS_INSTANCE, List.of(value.getProcessInstanceId()), model));
  }

  private static Long positive(final long id) {
    return id > 0 ? id : null;
  }

  private static String nameOf(final Enum<?> state) {
    return state == null ? null : state.name();
  }
}
