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
package com.anyilanxin.kunpeng.engine.bpmn.command;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeSerialBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志事件串行分发处理器抽象基类：本地处理后按序分发至各分区。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class LogEventDistributeSerialProcessor<T extends UnifiedRecordValue>
    implements LogEventProcessor<T> {
  private final LogEventWriter writer;
  protected final DistributeSerialBehavior distributeSerialBehavior;

  public LogEventDistributeSerialProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    distributeSerialBehavior = behavior.distributeSerialBehavior();
  }

  @Override
  public void processRecord(final BusinessLogRecord<T> record) {
    final ValueLifeCycle valueState = record.getValueState();
    if (valueState == processRecordNewLifeCycle()) {
      processRecordNew(record);
    } else if (valueState == processRecordDistributeLifeCycle()) {
      processRecordDistribute(record);
    } else if (valueState == processRecordDistributeAckConfirmLifeCycle()) {
      processRecordDistributeAckConfirm(record);
    } else if (valueState == processRecordDistributeCompleteConfirmLifeCycle()) {
      processRecordDistributeCompleteConfirm(record);
    }
  }

  @Override
  public ValueLifeCycle[] valueLifeCycles() {
    final Set<ValueLifeCycle> valueLifeCycleList = new HashSet<>();
    valueLifeCycleList.add(processRecordNewLifeCycle());
    valueLifeCycleList.add(processRecordDistributeLifeCycle());
    valueLifeCycleList.add(processRecordDistributeCompleteConfirmLifeCycle());
    valueLifeCycleList.add(processRecordDistributeAckConfirmLifeCycle());
    return valueLifeCycleList.toArray(new ValueLifeCycle[] {});
  }

  public abstract ValueLifeCycle processRecordNewLifeCycle();

  public abstract ValueLifeCycle processRecordDistributeLifeCycle();

  public abstract ValueLifeCycle processRecordDistributeCompleteConfirmLifeCycle();

  public abstract ValueLifeCycle processRecordDistributeAckConfirmLifeCycle();

  public abstract void processRecordNew(final BusinessLogRecord<T> record);

  public abstract void processRecordDistribute(final BusinessLogRecord<T> record);

  public void processRecordDistributeCompleteConfirm(final BusinessLogRecord<T> record) {}

  public void processRecordDistributeAckConfirm(final BusinessLogRecord<T> record) {
    ackConfirmToContinue(record);
  }

  protected void ackConfirmToContinue(final BusinessLogRecord<T> record) {
    distributeSerialBehavior.distributeSerialConfirmResult(
        record, DistributeSerialLifeCycle.DISTRIBUTE_START);
  }

  protected void ackConfirmToDistributeComplete(final BusinessLogRecord<T> record) {
    distributeSerialBehavior.distributeSerialConfirmResult(
        record, DistributeSerialLifeCycle.DISTRIBUTE_COMPLETE);
  }
}
