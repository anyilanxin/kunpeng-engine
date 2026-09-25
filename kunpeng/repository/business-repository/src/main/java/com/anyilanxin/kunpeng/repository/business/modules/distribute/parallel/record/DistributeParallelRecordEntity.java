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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelRecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelState;
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
 * 并行分发 Entity：并行分发 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeParallelRecordEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[DistributeParallelRecordEntity]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16
  private final LongProperty distributeIdProp = new LongProperty(1, "DISTRIBUTE_ID", -1);
  private final EnumProperty<DistributeParallelState> stateProp =
      new EnumProperty<>(
          2, STATE, DistributeParallelState.class, DistributeParallelState.ACTIVATED);
  private final LongProperty startTimeProp = new LongProperty(3, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(4, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(5, DURATION, -1);
  private final EnumProperty<DistributeParallelLifeCycle> lifeCycleProp =
      new EnumProperty<>(
          6, LIFE_CYCLE, DistributeParallelLifeCycle.class, DistributeParallelLifeCycle.NULL_VAL);
  private final IntegerProperty currentDistributeIndexProp =
      new IntegerProperty(7, "CURRENT_DISTRIBUTE_INDEX", -1);
  private final LongProperty distributeRecordIdProp = new LongProperty(8, "DISTRIBUTE_RECORD_ID");
  private final EnumProperty<DistributeParallelRecordType> distributeRecordTypeProp =
      new EnumProperty<>(
          9,
          "DISTRIBUTE_RECORD_TYPE",
          DistributeParallelRecordType.class,
          DistributeParallelRecordType.NULL_VAL);
  private final EnumProperty<ValueType> distributeRecordValueTypeProp =
      new EnumProperty<>(10, "DISTRIBUTE_RECORD_VALUE_TYPE", ValueType.class, ValueType.UNKNOW);
  private final ShortProperty distributeRecordLifeCycleProp =
      new ShortProperty(11, "DISTRIBUTE_RECORD_LIFE_CYCLE", (short) -1);
  private final ObjectProperty<UnifiedRecordValue> distributeRecordProp =
      new ObjectProperty<>(12, "DISTRIBUTE_RECORD", new UnifiedRecordValue(10));
  private final BooleanProperty haveFollowUpProp = new BooleanProperty(13, "HAVE_FOLLOW_UP", false);
  private final ShortProperty followUpLifeCycleProp =
      new ShortProperty(14, "FOLLOW_UP_LIFE_CYCLE", (short) -1);
  private final ArrayProperty<IntegerValue> distributeIndexProp =
      new ArrayProperty<>(15, "DISTRIBUTE_INDEX", IntegerValue::new);
  private final IntegerProperty distributeAfterIndexProp =
      new IntegerProperty(16, "DISTRIBUTE_AFTER_INDEX", -1);
  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DistributeParallelRecordEntity() {
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

  public void wrap(final DistributeParallelRecord record) {
    setDistributeId(record.getDistributeId())
        .setState(record.getState())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDuration(record.getDuration())
        .setLifeCycle(record.getLifeCycle())
        .setCurrentDistributeIndex(record.getCurrentDistributeIndex())
        .setDistributeRecordId(record.getDistributeRecordId())
        .setDistributeRecordType(record.getDistributeRecordType())
        .setDistributeRecordValueType(record.getDistributeRecordValueType())
        .setDistributeRecordLifeCycle(record.getDistributeRecordLifeCycle())
        .setDistributeRecord(record.getDistributeRecord())
        .setHaveFollowUp(record.isHaveFollowUp())
        .setFollowUpLifeCycle(record.getFollowUpLifeCycle())
        .setDistributeIndex(record.getDistributeIndex())
        .setDistributeAfterIndex(record.getDistributeAfterIndex());
  }

  public DistributeParallelRecord unwrap(final DistributeParallelRecord distributeRecord) {
    distributeRecord.reset();
    return distributeRecord
        .setDistributeId(getDistributeId())
        .setState(getState())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDuration(getDuration())
        .setLifeCycle(getLifeCycle())
        .setCurrentDistributeIndex(getCurrentDistributeIndex())
        .setDistributeRecordId(getDistributeRecordId())
        .setDistributeRecordType(getDistributeRecordType())
        .setDistributeRecordValueType(getDistributeRecordValueType())
        .setDistributeRecordLifeCycle(getDistributeRecordLifeCycle())
        .setDistributeRecord(getDistributeRecord())
        .setHaveFollowUp(isHaveFollowUp())
        .setFollowUpLifeCycle(getFollowUpLifeCycle())
        .setDistributeIndex(getDistributeIndex())
        .setDistributeAfterIndex(getDistributeAfterIndex());
  }

  public long getDistributeId() {
    return distributeIdProp.getValue();
  }

  public DistributeParallelRecordEntity setDistributeId(final long distributeId) {
    distributeIdProp.setValue(distributeId);
    return this;
  }

  public DistributeParallelState getState() {
    return stateProp.getValue();
  }

  public DistributeParallelRecordEntity setState(final DistributeParallelState state) {
    stateProp.setValue(state);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public DistributeParallelRecordEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public DistributeParallelRecordEntity setEndTime(final long endTime) {
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

  public DistributeParallelRecordEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public DistributeParallelLifeCycle getLifeCycle() {
    return lifeCycleProp.getValue();
  }

  public DistributeParallelRecordEntity setLifeCycle(final DistributeParallelLifeCycle lifeCycle) {
    lifeCycleProp.setValue(lifeCycle);
    return this;
  }

  public int getCurrentDistributeIndex() {
    return currentDistributeIndexProp.getValue();
  }

  public DistributeParallelRecordEntity setCurrentDistributeIndex(
      final int currentDistributeIndex) {
    currentDistributeIndexProp.setValue(currentDistributeIndex);
    return this;
  }

  public long getDistributeRecordId() {
    return distributeRecordIdProp.getValue();
  }

  public DistributeParallelRecordEntity setDistributeRecordId(final long distributeRecordId) {
    distributeRecordIdProp.setValue(distributeRecordId);
    return this;
  }

  public DistributeParallelRecordType getDistributeRecordType() {
    return distributeRecordTypeProp.getValue();
  }

  public DistributeParallelRecordEntity setDistributeRecordType(
      final DistributeParallelRecordType distributeRecordType) {
    distributeRecordTypeProp.setValue(distributeRecordType);
    return this;
  }

  public ValueType getDistributeRecordValueType() {
    return distributeRecordValueTypeProp.getValue();
  }

  public DistributeParallelRecordEntity setDistributeRecordValueType(
      final ValueType distributeRecordValueType) {
    distributeRecordValueTypeProp.setValue(distributeRecordValueType);
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

  public DistributeParallelRecordEntity setDistributeRecordLifeCycle(
      final ValueLifeCycle distributeRecordLifeCycle) {
    distributeRecordLifeCycleProp.setValue(distributeRecordLifeCycle.value());
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

  public DistributeParallelRecordEntity setDistributeRecord(
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

  public boolean isHaveFollowUp() {
    return haveFollowUpProp.getValue();
  }

  public DistributeParallelRecordEntity setHaveFollowUp(final boolean haveFollowUp) {
    haveFollowUpProp.setValue(haveFollowUp);
    return this;
  }

  public ValueLifeCycle getFollowUpLifeCycle() {
    if (getDistributeRecordValueType() != ValueType.UNKNOW
        && followUpLifeCycleProp.getValue() != -1) {
      return ValueLifeCycle.fromProtocolValue(
          getDistributeRecordValueType(), followUpLifeCycleProp.getValue());
    }
    return ValueLifeCycle.UNKNOWN;
  }

  public DistributeParallelRecordEntity setFollowUpLifeCycle(
      final ValueLifeCycle followUpLifeCycle) {
    followUpLifeCycleProp.setValue(followUpLifeCycle.value());
    setHaveFollowUp(followUpLifeCycle != ValueLifeCycle.UNKNOWN);
    return this;
  }

  public List<Integer> getDistributeIndex() {
    return StreamSupport.stream(distributeIndexProp.spliterator(), false)
        .map(IntegerValue::getValue)
        .collect(Collectors.toList());
  }

  public DistributeParallelRecordEntity setDistributeIndex(final List<Integer> distributeIndexSet) {
    final List<Integer> distributeIndexList = new ArrayList<>(distributeIndexSet);
    distributeIndexList.sort(Integer::compareTo);
    distributeIndexProp.reset();
    for (final Integer distributeIndex : distributeIndexList) {
      distributeIndexProp.add().setValue(distributeIndex);
    }
    return this;
  }

  public int getDistributeAfterIndex() {
    return distributeAfterIndexProp.getValue();
  }

  public DistributeParallelRecordEntity setDistributeAfterIndex(final int distributeAfterIndex) {
    distributeAfterIndexProp.setValue(distributeAfterIndex);
    return this;
  }
}
