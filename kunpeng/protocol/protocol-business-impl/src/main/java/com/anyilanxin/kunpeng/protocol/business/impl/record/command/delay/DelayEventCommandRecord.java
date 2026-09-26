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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.delay;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayEventCommandRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayRecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 延迟事件命令 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DelayEventCommandRecord extends UnifiedRecordValue<DelayEventCommandRecord>
    implements DelayEventCommandRecordValue {
  private final LongProperty delayIdProp = new LongProperty(1, "DELAY_ID", -1);
  private final EnumProperty<DelayState> stateProp = new EnumProperty<>(7, STATE, DelayState.class);
  private final LongProperty startTimeProp = new LongProperty(8, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(9, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(10, DURATION, -1);
  private final EnumProperty<DelayLifeCycle> lifeCycleProp =
      new EnumProperty<>(11, LIFE_CYCLE, DelayLifeCycle.class, DelayLifeCycle.NULL_VAL);
  private final LongProperty delayRecordIdProp = new LongProperty(2, "DELAY_RECORD_ID", -1);
  private final EnumProperty<DelayRecordType> delayRecordTypeProp =
      new EnumProperty<>(3, "DELAY_RECORD_TYPE", DelayRecordType.class, DelayRecordType.NULL_VAL);
  private final EnumProperty<ValueType> delayRecordValueTypeProp =
      new EnumProperty<>(4, "DELAY_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ShortProperty delayRecordLifeCycleProp =
      new ShortProperty(5, "DELAY_RECORD_LIFE_CYCLE", (short) -1);
  private final ObjectProperty<UnifiedRecordValue> delayRecordProp =
      new ObjectProperty<>(6, "DELAY_RECORD", new UnifiedRecordValue(10));
  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DelayEventCommandRecord() {
    super(11);
    declareProperty(delayIdProp)
        .declareProperty(stateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(lifeCycleProp)
        .declareProperty(delayRecordIdProp)
        .declareProperty(delayRecordTypeProp)
        .declareProperty(delayRecordValueTypeProp)
        .declareProperty(delayRecordLifeCycleProp)
        .declareProperty(delayRecordProp);
  }

  @Override
  public long getDelayId() {
    return delayIdProp.getValue();
  }

  public DelayEventCommandRecord setDelayId(final long delayId) {
    delayIdProp.setValue(delayId);
    return this;
  }

  @Override
  public DelayState getState() {
    return stateProp.getValue();
  }

  public DelayEventCommandRecord setState(final DelayState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public DelayEventCommandRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public DelayEventCommandRecord setEndTime(final long endTime) {
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    endTimeProp.setValue(endTime);

    return this;
  }

  @Override
  public long getDuration() {
    return durationProp.getValue();
  }

  public DelayEventCommandRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public DelayLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public DelayEventCommandRecord setLifeCycle(final DelayLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public DelayRecordType getDelayRecordType() {
    return delayRecordTypeProp.getValue();
  }

  public DelayEventCommandRecord setDelayRecordType(final DelayRecordType delayRecordType) {
    delayRecordTypeProp.setValue(delayRecordType);
    return this;
  }

  @Override
  public long getDelayRecordId() {
    return delayRecordIdProp.getValue();
  }

  public DelayEventCommandRecord setDelayRecordId(final long delayRecordId) {
    delayRecordIdProp.setValue(delayRecordId);
    return this;
  }

  @Override
  public ValueType getDelayRecordValueType() {
    return delayRecordValueTypeProp.getValue();
  }

  public DelayEventCommandRecord setDelayRecordValueType(final ValueType delayRecordValueType) {
    delayRecordValueTypeProp.setValue(delayRecordValueType);
    return this;
  }

  @Override
  public ValueLifeCycle getDelayRecordLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(
        getDelayRecordValueType(), delayRecordLifeCycleProp.getValue());
  }

  public DelayEventCommandRecord setDelayRecordLifeCycle(
      final ValueLifeCycle delayRecordLifeCycle) {
    delayRecordLifeCycleProp.setValue(delayRecordLifeCycle.value());
    return this;
  }

  @Override
  public UnifiedRecordValue getDelayRecord() {
    final var valueType = getDelayRecordValueType();
    if (valueType == ValueType.UNKNOW) {
      return null;
    }
    final var delayRecord = delayRecordProp.getValue();
    if (delayRecord.isEmpty()) {
      return delayRecord;
    }
    return VALUE_MAPPER.copyValue(getDelayRecordLifeCycle(), delayRecord);
  }

  public DelayEventCommandRecord setDelayRecord(final UnifiedRecordValue delayRecord) {
    if (delayRecord == null) {
      delayRecordProp.reset();
      return this;
    }
    final var valueBuffer = new UnsafeBuffer(0, 0);
    final int encodedLength = delayRecord.getLength();
    valueBuffer.wrap(new byte[encodedLength]);
    delayRecord.write(valueBuffer, 0);
    delayRecordProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  @Override
  protected DelayEventCommandRecord newRecord() {
    return new DelayEventCommandRecord();
  }
}
