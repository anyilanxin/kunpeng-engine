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
package com.anyilanxin.kunpeng.engine.bpmn.command.incident.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.incident.IncidentAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * 事件解决命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IncidentResolveProcessor extends IncidentAbstractProcessor {

  public IncidentResolveProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<IncidentRecord> record) {
    final IncidentRecord incidentRecord = record.getValue();
    @SuppressWarnings("rawtypes")
    final UnifiedRecordValue recordValue =
        switch (incidentRecord.getIncidentType()) {
          case ACTIVITY -> activityInstance.getRecord(incidentRecord.getActivityInstanceId());
          case PROCESS_INSTANCE -> processInstance.getRecord(incidentRecord.getProcessInstanceId());
          case USER_TASK -> userTask.getRecord(incidentRecord.getTaskId());
        };
    if (recordValue == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "incident 对应业务数据不存在");
      return;
    }
    writer.addEvent(
        incidentRecord.getIncidentId(),
        IncidentLifeCycle.RESOLVED,
        record.getRequestId(),
        incidentRecord);
    writer.addCommand(
        incidentRecord.getIncidentRecordLifeCycle(), record.getRequestId(), recordValue);
  }

  @Override
  public IncidentLifeCycle processState() {
    return IncidentLifeCycle.RESOLVE;
  }
}
