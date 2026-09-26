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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 串行分发 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeSerialRecord extends UnifiedRecordValue<DistributeSerialRecord>
    implements DistributeSerialRecordValue {

  private final LongProperty distributeIdProp = new LongProperty(1, "DISTRIBUTE_ID", -1);
  private final EnumProperty<DistributeSerialState> stateProp =
      new EnumProperty<>(14, STATE, DistributeSerialState.class, DistributeSerialState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(15, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(16, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(17, DURATION, -1);
  private final EnumProperty<DistributeSerialLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          18, LIFE_CYCLE, DistributeSerialLifeCycle.class, DistributeSerialLifeCycle.NULL_VAL);
  private final LongProperty distributeRecordIdProp = new LongProperty(2, "DISTRIBUTE_RECORD_ID");
  private final ObjectProperty<UnifiedRecordValue> distributeRecordProp =
      new ObjectProperty<>(3, "DISTRIBUTE_RECORD", new UnifiedRecordValue(10));
  private final EnumProperty<ValueType> distributeRecordValueTypeProp =
      new EnumProperty<>(4, "DISTRIBUTE_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ArrayProperty<IntegerValue> distributeSourceIdProp =
      new ArrayProperty<>(5, "DISTRIBUTE_SOURCE_ID", IntegerValue::new);
  private final IntegerProperty currentDistributeSourceIdProp =
      new IntegerProperty(6, "CURRENT_DISTRIBUTE_SOURCE_ID", -1);
  private final ShortProperty distributeRecordLifeCycleProp =
      new ShortProperty(7, "DISTRIBUTE_RECORD_LIFE_CYCLE", (short) -1);
  private final ObjectProperty<UnifiedRecordValue> distributeAckRecordProp =
      new ObjectProperty<>(8, "DISTRIBUTE_ACK_RECORD", new UnifiedRecordValue(10));
  private final BooleanProperty distributeAckSuccessProp =
      new BooleanProperty(9, "DISTRIBUTE_ACK_SUCCESS", false);
  private final BooleanProperty needDistributeAckConfirmProp =
      new BooleanProperty(10, "NEED_DISTRIBUTE_ACK_CONFIRM", false);
  private final ShortProperty distributeRecordAckConfirmLifeCycleProp =
      new ShortProperty(11, "DISTRIBUTE_RECORD_ACK_CONFIRM_LIFE_CYCLE", (short) -1);
  private final BooleanProperty needDistributeCompleteConfirmProp =
      new BooleanProperty(12, "NEED_DISTRIBUTE_COMPLETE_CONFIRM", false);
  private final ShortProperty distributeRecordCompleteConfirmLifeCycleProp =
      new ShortProperty(13, "DISTRIBUTE_RECORD_COMPLETE_CONFIRM_LIFE_CYCLE", (short) -1);

  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DistributeSerialRecord() {
    super(18);
    // formatting:off
    declareProperty(distributeIdProp)
      .declareProperty(stateProp)
      .declareProperty(startTimeProp)
      .declareProperty(endTimeProp)
      .declareProperty(durationProp)
      .declareProperty(lifeCycleProp)
      .declareProperty(distributeRecordIdProp)
      .declareProperty(distributeRecordProp)
      .declareProperty(distributeRecordValueTypeProp)
      .declareProperty(distributeSourceIdProp)
      .declareProperty(currentDistributeSourceIdProp)
      .declareProperty(distributeRecordLifeCycleProp)
      .declareProperty(distributeAckRecordProp)
      .declareProperty(distributeAckSuccessProp)
      .declareProperty(needDistributeAckConfirmProp)
      .declareProperty(distributeRecordAckConfirmLifeCycleProp)
      .declareProperty(needDistributeCompleteConfirmProp)
      .declareProperty(distributeRecordCompleteConfirmLifeCycleProp);
    // formatting:on
  }

  @Override
  public long getDistributeId() {
    return distributeIdProp.getValue();
  }

  public DistributeSerialRecord setDistributeId(final long distributeId) {
    distributeIdProp.setValue(distributeId);
    return this;
  }

  @Override
  public DistributeSerialState getState() {
    return stateProp.getValue();
  }

  public DistributeSerialRecord setState(final DistributeSerialState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public DistributeSerialRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public DistributeSerialRecord setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  @Override
  public long getDuration() {
    return durationProp.getValue();
  }

  public DistributeSerialRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public DistributeSerialLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public DistributeSerialRecord setLifeCycle(final DistributeSerialLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public long getDistributeRecordId() {
    return distributeRecordIdProp.getValue();
  }

  public DistributeSerialRecord setDistributeRecordId(final long distributeRecordId) {
    distributeRecordIdProp.setValue(distributeRecordId);
    return this;
  }

  @Override
  public UnifiedRecordValue getDistributeRecord() {
    final var valueType = getDistributeRecordValueType();
    if (valueType == ValueType.UNKNOW) {
      return null;
    }
    final var distributeRecord = distributeRecordProp.getValue();
    if (distributeRecord.isEmpty()) {
      return distributeRecord;
    }
    return VALUE_MAPPER.copyValue(getDistributeRecordLifeCycle(), distributeRecord);
  }

  public DistributeSerialRecord setDistributeRecord(final UnifiedRecordValue distributeRecord) {
    distributeRecordProp.reset();
    if (distributeRecord == null) {
      return this;
    }
    final var valueBuffer = new UnsafeBuffer(0, 0);
    final int encodedLength = distributeRecord.getLength();
    valueBuffer.wrap(new byte[encodedLength]);
    distributeRecord.write(valueBuffer, 0);
    distributeRecordProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  @Override
  public ValueType getDistributeRecordValueType() {
    return distributeRecordValueTypeProp.getValue();
  }

  public DistributeSerialRecord setDistributeRecordValueType(
      final ValueType distributeRecordValueType) {
    distributeRecordValueTypeProp.setValue(distributeRecordValueType);
    return this;
  }

  @Override
  public List<Integer> getDistributeSourceId() {
    return StreamSupport.stream(distributeSourceIdProp.spliterator(), false)
        .map(IntegerValue::getValue)
        .collect(Collectors.toList());
  }

  public DistributeSerialRecord setDistributeSourceId(final List<Integer> distributeSourceIdSet) {
    final List<Integer> sourceIdList = new ArrayList<>(distributeSourceIdSet);
    sourceIdList.sort(Integer::compareTo);
    distributeSourceIdProp.reset();
    for (final Integer sourceId : sourceIdList) {
      distributeSourceIdProp.add().setValue(sourceId);
    }
    return this;
  }

  @Override
  public int getCurrentDistributeSourceId() {
    return currentDistributeSourceIdProp.getValue();
  }

  public DistributeSerialRecord setCurrentDistributeSourceId(final int currentDistributeSourceId) {
    currentDistributeSourceIdProp.setValue(currentDistributeSourceId);
    return this;
  }

  @Override
  public ValueLifeCycle getDistributeRecordLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecord setDistributeRecordLifeCycle(
      final ValueLifeCycle distributeRecordLifeCycle) {
    distributeRecordLifeCycleProp.setValue(distributeRecordLifeCycle.value());
    return this;
  }

  @Override
  public UnifiedRecordValue getDistributeAckRecord() {
    final var valueType = getDistributeRecordValueType();
    if (valueType == ValueType.UNKNOW) {
      return null;
    }
    final var distributeAckRecord = distributeAckRecordProp.getValue();
    if (distributeAckRecord.isEmpty()) {
      return distributeAckRecord;
    }
    return VALUE_MAPPER.copyValue(getDistributeRecordLifeCycle(), distributeAckRecord);
  }

  public DistributeSerialRecord setDistributeAckRecord(
      final UnifiedRecordValue distributeAckRecord) {
    distributeAckRecordProp.reset();
    if (distributeAckRecord == null) {
      return this;
    }
    final var valueBuffer = new UnsafeBuffer(0, 0);
    final int encodedLength = distributeAckRecord.getLength();
    valueBuffer.wrap(new byte[encodedLength]);
    distributeAckRecord.write(valueBuffer, 0);
    distributeAckRecordProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  @Override
  public boolean distributeAckSuccess() {
    return distributeAckSuccessProp.getValue();
  }

  public DistributeSerialRecord setDistributeAckSuccess(final boolean distributeAckSuccess) {
    distributeAckSuccessProp.setValue(distributeAckSuccess);
    return this;
  }

  @Override
  public boolean needDistributeAckConfirm() {
    return needDistributeAckConfirmProp.getValue();
  }

  public DistributeSerialRecord setNeedDistributeAckConfirm(
      final boolean needDistributeAckConfirm) {
    needDistributeAckConfirmProp.setValue(needDistributeAckConfirm);
    return this;
  }

  @Override
  public ValueLifeCycle getDistributeRecordAckConfirmLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordAckConfirmLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordAckConfirmLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecord setDistributeRecordAckConfirmLifeCycle(
      final ValueLifeCycle distributeRecordAckConfirmLifeCycle) {
    distributeRecordAckConfirmLifeCycleProp.setValue(distributeRecordAckConfirmLifeCycle.value());
    return this;
  }

  @Override
  public boolean needDistributeCompleteConfirm() {
    return needDistributeCompleteConfirmProp.getValue();
  }

  public DistributeSerialRecord setNeedDistributeCompleteConfirm(
      final boolean needDistributeCompleteConfirm) {
    needDistributeCompleteConfirmProp.setValue(needDistributeCompleteConfirm);
    return this;
  }

  @Override
  public ValueLifeCycle getDistributeRecordCompleteConfirmLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordCompleteConfirmLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordCompleteConfirmLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecord setDistributeRecordCompleteConfirmLifeCycle(
      final ValueLifeCycle distributeRecordCompleteConfirmLifeCycle) {
    distributeRecordCompleteConfirmLifeCycleProp.setValue(
        distributeRecordCompleteConfirmLifeCycle.value());
    return this;
  }

  @Override
  protected DistributeSerialRecord newRecord() {
    return new DistributeSerialRecord();
  }
}
