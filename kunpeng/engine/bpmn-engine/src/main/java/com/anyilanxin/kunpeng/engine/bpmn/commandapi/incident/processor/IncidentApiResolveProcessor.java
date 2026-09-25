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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.incident.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.incident.IncidentApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.incident.resolve.IncidentResolveRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.incident.CommandApiIncidentValueLifeCycle;

/**
 * 事件解决 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IncidentApiResolveProcessor extends IncidentApiAbstractProcessor {

  public IncidentApiResolveProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<IncidentResolveRequestRecord> record) {
    final long key = record.getKey();
    final IncidentRecord incidentRecord = incident.getRecord(key);
    if (incidentRecord == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "incident 不存在:" + key);
      return;
    }
    writer.addCommand(IncidentLifeCycle.RESOLVE, record.getRequestId(), incidentRecord);
  }

  @Override
  public CommandApiIncidentValueLifeCycle processState() {
    return CommandApiIncidentValueLifeCycle.RESOLVE_REQUEST;
  }
}
