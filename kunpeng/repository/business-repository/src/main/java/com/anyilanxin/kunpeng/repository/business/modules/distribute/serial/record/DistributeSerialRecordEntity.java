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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 串行分发 Entity：串行分发 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeSerialRecordEntity extends UnpackedObject implements StoreValue {
  private final LongProperty distributeIdProp = new LongProperty(1, "DISTRIBUTE_ID", -1);
  private final EnumProperty<DistributeSerialState> stateProp =
      new EnumProperty<>(2, STATE, DistributeSerialState.class, DistributeSerialState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(3, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(4, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(5, DURATION, -1);
  private final EnumProperty<DistributeSerialLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          6, LIFE_CYCLE, DistributeSerialLifeCycle.class, DistributeSerialLifeCycle.NULL_VAL);
  private final LongProperty distributeRecordIdProp = new LongProperty(7, "DISTRIBUTE_RECORD_ID");
  private final ObjectProperty<UnifiedRecordValue> distributeRecordProp =
      new ObjectProperty<>(8, "DISTRIBUTE_RECORD", new UnifiedRecordValue(10));
  private final EnumProperty<ValueType> distributeRecordValueTypeProp =
      new EnumProperty<>(9, "DISTRIBUTE_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ArrayProperty<IntegerValue> distributeSourceIdProp =
      new ArrayProperty<>(10, "DISTRIBUTE_SOURCE_ID", IntegerValue::new);
  private final IntegerProperty currentDistributeSourceIdProp =
      new IntegerProperty(11, "CURRENT_DISTRIBUTE_SOURCE_ID", -1);
  private final ShortProperty distributeRecordLifeCycleProp =
      new ShortProperty(12, "DISTRIBUTE_RECORD_LIFE_CYCLE", (short) -1);
  private final BooleanProperty needDistributeAckConfirmProp =
      new BooleanProperty(13, "NEED_DISTRIBUTE_ACK_CONFIRM", false);
  private final ShortProperty distributeRecordAckConfirmLifeCycleProp =
      new ShortProperty(14, "DISTRIBUTE_RECORD_ACK_CONFIRM_LIFE_CYCLE", (short) -1);
  private final BooleanProperty needDistributeCompleteConfirmProp =
      new BooleanProperty(15, "NEED_DISTRIBUTE_COMPLETE_CONFIRM", false);
  private final ShortProperty distributeRecordCompleteConfirmLifeCycleProp =
      new ShortProperty(16, "DISTRIBUTE_RECORD_COMPLETE_CONFIRM_LIFE_CYCLE", (short) -1);

  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DistributeSerialRecordEntity() {
    super(16);
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
        .declareProperty(needDistributeAckConfirmProp)
        .declareProperty(distributeRecordAckConfirmLifeCycleProp)
        .declareProperty(needDistributeCompleteConfirmProp)
        .declareProperty(distributeRecordCompleteConfirmLifeCycleProp);
  }

  public void wrap(final DistributeSerialRecord record) {
    setDistributeId(record.getDistributeId())
        .setState(record.getState())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setLifeCycle(record.getLifeCycle())
        .setDistributeRecordId(record.getDistributeRecordId())
        .setDistributeRecord(record.getDistributeRecord())
        .setDistributeRecordValueType(record.getDistributeRecordValueType())
        .setDistributeSourceId(record.getDistributeSourceId())
        .setCurrentDistributeSourceId(record.getCurrentDistributeSourceId())
        .setDistributeRecordLifeCycle(record.getDistributeRecordLifeCycle())
        .setNeedDistributeAckConfirm(record.needDistributeAckConfirm())
        .setDistributeRecordAckConfirmLifeCycle(record.getDistributeRecordAckConfirmLifeCycle())
        .setNeedDistributeCompleteConfirm(record.needDistributeCompleteConfirm())
        .setDistributeRecordCompleteConfirmLifeCycle(
            record.getDistributeRecordCompleteConfirmLifeCycle());
  }

  public DistributeSerialRecord unwrap(final DistributeSerialRecord distributeRecord) {
    distributeRecord.reset();
    return distributeRecord
        .setDistributeId(getDistributeId())
        .setState(getState())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setLifeCycle(getLifeCycle())
        .setDistributeRecordId(getDistributeRecordId())
        .setDistributeRecord(getDistributeRecord())
        .setDistributeRecordValueType(getDistributeRecordValueType())
        .setDistributeSourceId(getDistributeSourceId())
        .setCurrentDistributeSourceId(getCurrentDistributeSourceId())
        .setDistributeRecordLifeCycle(getDistributeRecordLifeCycle())
        .setNeedDistributeAckConfirm(needDistributeAckConfirm())
        .setDistributeRecordAckConfirmLifeCycle(getDistributeRecordAckConfirmLifeCycle())
        .setNeedDistributeCompleteConfirm(needDistributeCompleteConfirm())
        .setDistributeRecordCompleteConfirmLifeCycle(getDistributeRecordCompleteConfirmLifeCycle());
  }

  public long getDistributeId() {
    return distributeIdProp.getValue();
  }

  public DistributeSerialRecordEntity setDistributeId(final long distributeId) {
    distributeIdProp.setValue(distributeId);
    return this;
  }

  public DistributeSerialState getState() {
    return stateProp.getValue();
  }

  public DistributeSerialRecordEntity setState(final DistributeSerialState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public DistributeSerialRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public DistributeSerialRecordEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public DistributeSerialRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public DistributeSerialLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public DistributeSerialRecordEntity setLifeCycle(final DistributeSerialLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public long getDistributeRecordId() {
    return distributeRecordIdProp.getValue();
  }

  public DistributeSerialRecordEntity setDistributeRecordId(final long distributeRecordId) {
    distributeRecordIdProp.setValue(distributeRecordId);
    return this;
  }

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

  public DistributeSerialRecordEntity setDistributeRecord(
      final UnifiedRecordValue distributeRecord) {
    distributeRecordProp.reset();
    if (distributeRecord == null) {
      return this;
    }
    final int encodedLength = distributeRecord.getLength();
    final var valueBuffer = new UnsafeBuffer(new byte[encodedLength]);
    distributeRecord.write(valueBuffer, 0);
    distributeRecordProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  public ValueType getDistributeRecordValueType() {
    return distributeRecordValueTypeProp.getValue();
  }

  public DistributeSerialRecordEntity setDistributeRecordValueType(
      final ValueType distributeRecordValueType) {
    distributeRecordValueTypeProp.setValue(distributeRecordValueType);
    return this;
  }

  public List<Integer> getDistributeSourceId() {
    return StreamSupport.stream(distributeSourceIdProp.spliterator(), false)
        .map(IntegerValue::getValue)
        .collect(Collectors.toList());
  }

  public DistributeSerialRecordEntity setDistributeSourceId(
      final List<Integer> distributeSourceIdSet) {
    final List<Integer> sourceIdList = new ArrayList<>(distributeSourceIdSet);
    sourceIdList.sort(Integer::compareTo);
    distributeSourceIdProp.reset();
    for (final Integer sourceId : sourceIdList) {
      distributeSourceIdProp.add().setValue(sourceId);
    }
    return this;
  }

  public int getCurrentDistributeSourceId() {
    return currentDistributeSourceIdProp.getValue();
  }

  public DistributeSerialRecordEntity setCurrentDistributeSourceId(
      final int currentDistributeSourceId) {
    currentDistributeSourceIdProp.setValue(currentDistributeSourceId);
    return this;
  }

  public ValueLifeCycle getDistributeRecordLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecordEntity setDistributeRecordLifeCycle(
      final ValueLifeCycle distributeRecordLifeCycle) {
    distributeRecordLifeCycleProp.setValue(distributeRecordLifeCycle.value());
    return this;
  }

  public boolean needDistributeAckConfirm() {
    return needDistributeAckConfirmProp.getValue();
  }

  public DistributeSerialRecordEntity setNeedDistributeAckConfirm(
      final boolean needDistributeAckConfirm) {
    needDistributeAckConfirmProp.setValue(needDistributeAckConfirm);
    return this;
  }

  public ValueLifeCycle getDistributeRecordAckConfirmLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordAckConfirmLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordAckConfirmLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecordEntity setDistributeRecordAckConfirmLifeCycle(
      final ValueLifeCycle distributeRecordAckConfirmLifeCycle) {
    distributeRecordAckConfirmLifeCycleProp.setValue(distributeRecordAckConfirmLifeCycle.value());
    return this;
  }

  public boolean needDistributeCompleteConfirm() {
    return needDistributeCompleteConfirmProp.getValue();
  }

  public DistributeSerialRecordEntity setNeedDistributeCompleteConfirm(
      final boolean needDistributeCompleteConfirm) {
    needDistributeCompleteConfirmProp.setValue(needDistributeCompleteConfirm);
    return this;
  }

  public ValueLifeCycle getDistributeRecordCompleteConfirmLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && distributeRecordCompleteConfirmLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), distributeRecordCompleteConfirmLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeSerialRecordEntity setDistributeRecordCompleteConfirmLifeCycle(
      final ValueLifeCycle distributeRecordCompleteConfirmLifeCycle) {
    distributeRecordCompleteConfirmLifeCycleProp.setValue(
        distributeRecordCompleteConfirmLifeCycle.value());
    return this;
  }
}
