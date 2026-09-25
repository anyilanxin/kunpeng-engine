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
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.ImmutableDistributeSerialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 串行分发 ACK 命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialAckProcessor extends AbstractDistributeSerialProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(DistributeSerialAckProcessor.class);
  final LogEventWriter writer;
  private final ImmutableDistributeSerialRepository distribute;
  private final DistributeSerialBehavior distributeSerialBehavior;

  public DistributeSerialAckProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeSerialBehavior = behavior.distributeSerialBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    distribute = repository.distributeSerialRepository();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DistributeSerialRecord> record) {
    final DistributeSerialRecord value = record.getValue();
    final DistributeSerialRecord distributeRecord = distribute.getDistribute(record.getKey());
    if (distributeRecord == null) {
      return;
    }
    distributeRecord.setCurrentDistributeSourceId(value.getCurrentDistributeSourceId());

    LOG.debug("distributeAckProcessor fromSourceId:{}", value.getCurrentDistributeSourceId());
    writer.addEvent(
        value.getDistributeId(),
        DistributeSerialLifeCycle.DISTRIBUTE_ACKED,
        record.getRequestId(),
        distributeRecord);
    if (distributeRecord.needDistributeAckConfirm() && value.distributeAckSuccess()) {
      writer.addCommand(
          value.getDistributeRecordId(),
          value.getDistributeRecordAckConfirmLifeCycle(),
          record.getRequestId(),
          value.getDistributeId(),
          -1,
          value.getDistributeAckRecord());
    } else {
      writer.addCommand(
          value.getDistributeId(),
          DistributeSerialLifeCycle.DISTRIBUTE_START,
          record.getRequestId(),
          distributeRecord);
    }
  }

  @Override
  public DistributeSerialLifeCycle valueLifeCycle() {
    return DistributeSerialLifeCycle.DISTRIBUTE_ACK;
  }
}
