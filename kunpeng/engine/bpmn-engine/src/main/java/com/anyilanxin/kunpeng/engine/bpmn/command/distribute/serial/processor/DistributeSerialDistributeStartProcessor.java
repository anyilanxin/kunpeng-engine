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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeSerialBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.AbstractDistributeSerialProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 串行分发启动命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialDistributeStartProcessor extends AbstractDistributeSerialProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(DistributeSerialDistributeStartProcessor.class);
  final LogEventWriter writer;
  private final DistributeSerialBehavior distributeSerialBehavior;

  public DistributeSerialDistributeStartProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeSerialBehavior = behavior.distributeSerialBehavior();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeSerialRecord> record) {
    LOG.debug(
        "Distributing data to other partitions for distribute {}",
        record.getValue().getDistributeId());
    final DistributeSerialRecord value = record.getValue();
    // 单趟遍历：找到第一个大于 currentIndex 的下标并判断其后是否还有其他，避免
    // 构建中间列表（原 stream().filter().toList() 会分配一个）。
    final int currentIndex = value.getCurrentDistributeSourceId();
    Integer first = null;
    for (final Integer sourceId : value.getDistributeSourceId()) {
      if (sourceId > currentIndex) {
        first = sourceId;
        break;
      }
    }
    if (first != null) {
      value.setCurrentDistributeSourceId(first);
      distributeSerialBehavior.distributeSerial(value, first);
      writer.addEvent(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_CONTINUED,
          record.getRequestId(),
          value);
    } else {
      writer.addCommand(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_COMPLETE,
          record.getRequestId(),
          value);
    }
  }

  @Override
  public DistributeSerialLifeCycle valueLifeCycle() {
    return DistributeSerialLifeCycle.DISTRIBUTE_START;
  }
}
