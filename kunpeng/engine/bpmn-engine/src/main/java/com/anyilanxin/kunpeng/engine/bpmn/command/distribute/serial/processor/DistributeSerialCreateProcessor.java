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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.AbstractDistributeSerialProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 串行分发创建命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialCreateProcessor extends AbstractDistributeSerialProcessor {
  final LogEventWriter writer;

  public DistributeSerialCreateProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeSerialRecord> record) {
    final DistributeSerialRecord value = record.getValue();
    value.setDistributeId(writer.nextCurrentSourceKey());
    value.setStartTime(writer.millis());
    final int currentSourceId = writer.getSourceId();
    final Set<Integer> sourceIds = writer.getActivitySourceIds();
    final List<Integer> activitySourceIds = new ArrayList<>(sourceIds.size());
    for (final Integer sourceId : sourceIds) {
      if (sourceId != currentSourceId && sourceId != 1) {
        activitySourceIds.add(sourceId);
      }
    }
    value.setDistributeSourceId(activitySourceIds);
    writer.addEvent(
        value.getDistributeId(),
        DistributeSerialLifeCycle.CREATE_DISTRIBUTED,
        record.getRequestId(),
        value);
    if (activitySourceIds.isEmpty()) {
      writer.addCommand(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_COMPLETE,
          record.getRequestId(),
          value);
      return;
    }
    writer.addCommand(
        value.getDistributeId(),
        DistributeSerialLifeCycle.DISTRIBUTE_START,
        record.getRequestId(),
        value);
  }

  @Override
  public DistributeSerialLifeCycle valueLifeCycle() {
    return DistributeSerialLifeCycle.CREATE_DISTRIBUTE;
  }
}
