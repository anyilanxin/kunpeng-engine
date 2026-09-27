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

import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.ACTIVITY_INSTANCE;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.INCIDENT;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.MESSAGE_SUBSCRIPTION;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.PROCESS_INSTANCE;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.SIGNAL_SUBSCRIPTION;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.TIMER;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.USER_TASK;
import static com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables.VARIABLE;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupRecordValue;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;

/**
 * 历史清理：按 {@code process_instance_id} 连带删除该实例的全部运行轨迹（含定时器与订阅，避免实例结束后残留孤儿行）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class HistoryCleanupRecordHandler implements RecordModelHandler {

  @Override
  public ValueType valueType() {
    return ValueType.HISTORY_CLEANUP;
  }

  @Override
  public boolean accepts(final ValueLifeCycle lifecycle) {
    return lifecycle == HistoryCleanupLifeCycle.TRIGGERED;
  }

  @Override
  public void transition(final BusinessEventRecord<?> record, final ChangeBuffer buffer) {
    final HistoryCleanupRecordValue value = (HistoryCleanupRecordValue) record.getValue();
    final var processInstanceId = value.getProcessInstanceId();
    buffer.offer(
        RowChange.deleteByColumn(PROCESS_INSTANCE, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(ACTIVITY_INSTANCE, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(VARIABLE, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(USER_TASK, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(INCIDENT, "process_instance_id", processInstanceId));
    buffer.offer(RowChange.deleteByColumn(TIMER, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(MESSAGE_SUBSCRIPTION, "process_instance_id", processInstanceId));
    buffer.offer(
        RowChange.deleteByColumn(SIGNAL_SUBSCRIPTION, "process_instance_id", processInstanceId));
  }
}
