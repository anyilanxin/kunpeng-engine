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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 串行分发完成命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialCompleteProcessor extends AbstractDistributeSerialProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(DistributeSerialCompleteProcessor.class);
  final LogEventWriter writer;

  public DistributeSerialCompleteProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeSerialRecord> record) {
    LOG.debug("distributeCompleteProcessor {}", record.getValue().getDistributeId());
    final DistributeSerialRecord value = record.getValue();
    writer.addEvent(
        value.getDistributeId(),
        DistributeSerialLifeCycle.DISTRIBUTE_COMPLETED,
        record.getRequestId(),
        value);

    if (value.needDistributeCompleteConfirm()) {
      writer.addCommand(
          value.getDistributeRecordId(),
          value.getDistributeRecordCompleteConfirmLifeCycle(),
          record.getRequestId(),
          value.getDistributeRecord());
    }
  }

  @Override
  public DistributeSerialLifeCycle valueLifeCycle() {
    return DistributeSerialLifeCycle.DISTRIBUTE_COMPLETE;
  }
}
