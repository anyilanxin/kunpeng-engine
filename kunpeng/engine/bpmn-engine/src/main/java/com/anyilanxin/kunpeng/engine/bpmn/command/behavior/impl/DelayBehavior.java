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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.delay.DelayEventCommandRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayRecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.ImmutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.Optional;

/**
 * 延迟事件行为：定时延迟的调度与触发语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DelayBehavior {
  private final LogEventWriter writer;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableBatchRepository batch;
  private final ImmutableDelayRepository delay;

  public DelayBehavior(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    batch = repository.batchRepository();
    delay = repository.delayRepository();
  }

  public void addDelayEvent(
      final long delayId,
      final long delayRecordId,
      final ValueType delayRecordValueType,
      final ValueLifeCycle delayRecordLifeCycle,
      final UnifiedRecordValue delayRecord) {
    final DelayEventCommandRecord commandRecord = new DelayEventCommandRecord();
    commandRecord.setDelayId(delayId);
    commandRecord.setDelayRecordLifeCycle(delayRecordLifeCycle);
    commandRecord.setDelayRecord(delayRecord);
    commandRecord.setDelayRecordId(delayRecordId);
    commandRecord.setDelayRecordType(DelayRecordType.EVENT);
    commandRecord.setDelayRecordValueType(delayRecordValueType);
    commandRecord.setStartTime(writer.millis());
    commandRecord.setState(DelayState.CREATE);
    writer.addEvent(delayId, DelayLifeCycle.CREATE, -1, commandRecord);
  }

  public void addDelayCommand(
      final long relevanceId,
      final long delayRecordId,
      final ValueType delayRecordValueType,
      final ValueLifeCycle delayRecordLifeCycle,
      final UnifiedRecordValue delayRecord) {
    final DelayEventCommandRecord commandRecord = new DelayEventCommandRecord();
    commandRecord.setDelayId(relevanceId);
    commandRecord.setDelayRecordLifeCycle(delayRecordLifeCycle);
    commandRecord.setDelayRecord(delayRecord);
    commandRecord.setDelayRecordId(delayRecordId);
    commandRecord.setDelayRecordType(DelayRecordType.COMMAND);
    commandRecord.setDelayRecordValueType(delayRecordValueType);
    commandRecord.setStartTime(writer.millis());
    commandRecord.setState(DelayState.CREATE);
    writer.addEvent(relevanceId, DelayLifeCycle.CREATE, -1, commandRecord);
  }

  public boolean consume(final long relevanceId) {
    final Optional<DelayEventCommandRecord> query = delay.query(relevanceId);
    if (query.isPresent()) {
      final DelayEventCommandRecord commandRecord = query.get();
      commandRecord.setEndTime(writer.millis());
      commandRecord.setState(DelayState.CONSUME);
      final DelayRecordType delayRecordType = commandRecord.getDelayRecordType();
      if (delayRecordType == DelayRecordType.COMMAND) {
        writer.addCommand(
            commandRecord.getDelayRecordId(),
            commandRecord.getDelayRecordLifeCycle(),
            -1,
            commandRecord.getDelayRecord());
      } else if (delayRecordType == DelayRecordType.EVENT) {
        writer.addEvent(
            commandRecord.getDelayRecordId(),
            commandRecord.getDelayRecordLifeCycle(),
            -1,
            commandRecord.getDelayRecord());
      }
      writer.addEvent(relevanceId, DelayLifeCycle.CONSUME, -1, commandRecord);
      return true;
    }
    return false;
  }

  public boolean haveDelayConsume(final long relevanceId) {
    return delay.have(relevanceId);
  }
}
