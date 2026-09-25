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

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialAckRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 串行分发 ACK Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeSerialAckRecord extends UnifiedRecordValue<DistributeSerialAckRecord>
    implements DistributeSerialAckRecordValue {
  // structpack-ids[DistributeSerialAckRecord]: 1,2,3,4

  private final LongProperty idProp = new LongProperty(1, "ID", -1);
  private final ShortProperty recordLifeCycleProp =
      new ShortProperty(2, "RECORD_LIFE_CYCLE", (short) -1);
  private final ObjectProperty<UnifiedRecordValue> recordValueProp =
      new ObjectProperty<>(3, "RECORD_VALUE", new UnifiedRecordValue(10));
  private final BooleanProperty businessSuccessProp =
      new BooleanProperty(4, "BUSINESS_SUCCESS", false);

  private final PackerReader commandValueReader = new PackerReader();
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  public DistributeSerialAckRecord() {
    super(4);
    declareProperty(idProp)
        .declareProperty(recordLifeCycleProp)
        .declareProperty(recordValueProp)
        .declareProperty(businessSuccessProp);
  }

  @Override
  public long getId() {
    return idProp.getValue();
  }

  public DistributeSerialAckRecord setId(final long id) {
    idProp.setValue(id);
    return this;
  }

  @Override
  public CommandValueLifeCycle getRecordLifeCycle() {
    final short value = recordLifeCycleProp.getValue();
    if (value == -1) {
      return null;
    }
    final ValueLifeCycle lifeCycle = ValueLifeCycle.fromProtocolValue(ValueType.UNKNOW, value);
    if (lifeCycle instanceof final CommandValueLifeCycle commandValueLifeCycle) {
      return commandValueLifeCycle;
    }
    return null;
  }

  public DistributeSerialAckRecord setRecordLifeCycle(final CommandValueLifeCycle recordLifeCycle) {
    if (recordLifeCycle == null) {
      recordLifeCycleProp.setValue((short) -1);
    } else {
      recordLifeCycleProp.setValue(recordLifeCycle.value());
    }
    return this;
  }

  @Override
  public UnifiedRecordValue getRecordValue() {
    final var recordValue = recordValueProp.getValue();
    if (recordValue.isEmpty()) {
      return recordValue;
    }
    if (getRecordLifeCycle() == null) {
      return null;
    }
    return VALUE_MAPPER.copyValue(getRecordLifeCycle(), recordValue);
  }

  public DistributeSerialAckRecord setRecordValue(final UnifiedRecordValue recordValue) {
    recordValueProp.reset();
    if (recordValue == null) {
      return this;
    }
    final var valueBuffer = new UnsafeBuffer(0, 0);
    final int encodedLength = recordValue.getLength();
    valueBuffer.wrap(new byte[encodedLength]);
    recordValue.write(valueBuffer, 0);
    recordValueProp.read(commandValueReader.wrap(valueBuffer, 0, encodedLength));
    return this;
  }

  @Override
  public boolean isBusinessSuccess() {
    return businessSuccessProp.getValue();
  }

  public DistributeSerialAckRecord setBusinessSuccess(final boolean businessSuccess) {
    businessSuccessProp.setValue(businessSuccess);
    return this;
  }

  @Override
  protected DistributeSerialAckRecord newRecord() {
    return new DistributeSerialAckRecord();
  }
}
