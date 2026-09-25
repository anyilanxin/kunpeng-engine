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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelRecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelState;
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
 * 并行分发 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeParallelRecord extends UnifiedRecordValue<DistributeParallelRecord>
    implements DistributeParallelRecordValue {
  // structpack-ids[DistributeParallelRecord]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16
  private final LongProperty distributeIdProp = new LongProperty(1, "DISTRIBUTE_ID", -1);
  private final EnumProperty<DistributeParallelState> stateProp =
      new EnumProperty<>(
          12, STATE, DistributeParallelState.class, DistributeParallelState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(13, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(14, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(15, DURATION, -1);
  private final EnumProperty<DistributeParallelLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          16, LIFE_CYCLE, DistributeParallelLifeCycle.class, DistributeParallelLifeCycle.NULL_VAL);
  private final IntegerProperty currentDistributeIndexProp =
      new IntegerProperty(2, "CURRENT_DISTRIBUTE_INDEX", -1);
  private final LongProperty distributeRecordIdProp = new LongProperty(3, "DISTRIBUTE_RECORD_ID");
  private final EnumProperty<DistributeParallelRecordType> distributeRecordTypeProp =
      new EnumProperty<>(
          4,
          "DISTRIBUTE_RECORD_TYPE",
          DistributeParallelRecordType.class,
          DistributeParallelRecordType.NULL_VAL);
  private final EnumProperty<ValueType> distributeRecordValueTypeProp =
      new EnumProperty<>(5, "DISTRIBUTE_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ShortProperty distributeRecordLifeCycleProp =
      new ShortProperty(6, "DISTRIBUTE_RECORD_LIFE_CYCLE", (short) -1);
  private final ObjectProperty<UnifiedRecordValue> distributeRecordProp =
      new ObjectProperty<>(7, "DISTRIBUTE_RECORD", new UnifiedRecordValue(10));
  private final BooleanProperty haveFollowUpProp = new BooleanProperty(8, "HAVE_FOLLOW_UP", false);
  private final ShortProperty followUpLifeCycleProp =
      new ShortProperty(9, "FOLLOW_UP_LIFE_CYCLE", (short) -1);
  private final ArrayProperty<IntegerValue> distributeIndexProp =
      new ArrayProperty<>(10, "DISTRIBUTE_INDEX", IntegerValue::new);
  private final IntegerProperty distributeAfterIndexProp =
      new IntegerProperty(11, "DISTRIBUTE_AFTER_INDEX", -1);
  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DistributeParallelRecord() {
    super(16);
    declareProperty(distributeIdProp)
        .declareProperty(stateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(lifeCycleProp)
        .declareProperty(currentDistributeIndexProp)
        .declareProperty(distributeRecordIdProp)
        .declareProperty(distributeRecordTypeProp)
        .declareProperty(distributeRecordValueTypeProp)
        .declareProperty(distributeRecordLifeCycleProp)
        .declareProperty(distributeRecordProp)
        .declareProperty(haveFollowUpProp)
        .declareProperty(followUpLifeCycleProp)
        .declareProperty(distributeIndexProp)
        .declareProperty(distributeAfterIndexProp);
  }

  @Override
  public long getDistributeId() {
    return distributeIdProp.getValue();
  }

  public DistributeParallelRecord setDistributeId(final long distributeId) {
    distributeIdProp.setValue(distributeId);
    return this;
  }

  @Override
  public DistributeParallelState getState() {
    return stateProp.getValue();
  }

  public DistributeParallelRecord setState(final DistributeParallelState state) {
    stateProp.setValue(state);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public DistributeParallelRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public DistributeParallelRecord setEndTime(final long endTime) {
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

  public DistributeParallelRecord setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  @Override
  public DistributeParallelLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public DistributeParallelRecord setLifeCycle(final DistributeParallelLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  @Override
  public int getCurrentDistributeIndex() {
    return currentDistributeIndexProp.getValue();
  }

  public DistributeParallelRecord setCurrentDistributeIndex(final int currentDistributeIndex) {
    currentDistributeIndexProp.setValue(currentDistributeIndex);
    return this;
  }

  @Override
  public long getDistributeRecordId() {
    return distributeRecordIdProp.getValue();
  }

  public DistributeParallelRecord setDistributeRecordId(final long distributeRecordId) {
    distributeRecordIdProp.setValue(distributeRecordId);
    return this;
  }

  @Override
  public DistributeParallelRecordType getDistributeRecordType() {
    return distributeRecordTypeProp.getValue();
  }

  public DistributeParallelRecord setDistributeRecordType(
      final DistributeParallelRecordType distributeRecordType) {
    distributeRecordTypeProp.setValue(distributeRecordType);
    return this;
  }

  @Override
  public ValueType getDistributeRecordValueType() {
    return distributeRecordValueTypeProp.getValue();
  }

  public DistributeParallelRecord setDistributeRecordValueType(
      final ValueType distributeRecordValueType) {
    distributeRecordValueTypeProp.setValue(distributeRecordValueType);
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

  public DistributeParallelRecord setDistributeRecordLifeCycle(
      final ValueLifeCycle distributeRecordLifeCycle) {
    distributeRecordLifeCycleProp.setValue(distributeRecordLifeCycle.value());
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

  public DistributeParallelRecord setDistributeRecord(final UnifiedRecordValue distributeRecord) {
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
  public boolean isHaveFollowUp() {
    return haveFollowUpProp.getValue();
  }

  public DistributeParallelRecord setHaveFollowUp(final boolean haveFollowUp) {
    haveFollowUpProp.setValue(haveFollowUp);
    return this;
  }

  @Override
  public ValueLifeCycle getFollowUpLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && followUpLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), followUpLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeParallelRecord setFollowUpLifeCycle(final ValueLifeCycle followUpLifeCycle) {
    followUpLifeCycleProp.setValue(followUpLifeCycle.value());
    setHaveFollowUp(followUpLifeCycle != ValueLifeCycle.UNKNOWN);
    return this;
  }

  @Override
  public List<Integer> getDistributeIndex() {
    return StreamSupport.stream(distributeIndexProp.spliterator(), false)
        .map(IntegerValue::getValue)
        .collect(Collectors.toList());
  }

  public DistributeParallelRecord setDistributeIndex(final List<Integer> distributeIndexSet) {
    final List<Integer> distributeIndexList = new ArrayList<>(distributeIndexSet);
    distributeIndexList.sort(Integer::compareTo);
    distributeIndexProp.reset();
    for (final Integer distributeIndex : distributeIndexList) {
      distributeIndexProp.add().setValue(distributeIndex);
    }
    return this;
  }

  @Override
  public int getDistributeAfterIndex() {
    return distributeAfterIndexProp.getValue();
  }

  public DistributeParallelRecord setDistributeAfterIndex(final int distributeAfterIndex) {
    distributeAfterIndexProp.setValue(distributeAfterIndex);
    return this;
  }

  @Override
  protected DistributeParallelRecord newRecord() {
    return new DistributeParallelRecord();
  }
}
