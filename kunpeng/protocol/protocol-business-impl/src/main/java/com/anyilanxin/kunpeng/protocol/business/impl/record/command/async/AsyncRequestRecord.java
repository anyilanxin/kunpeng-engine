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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.async;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;

/**
 * 异步请求 Record：异步操作的发起与关联信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class AsyncRequestRecord extends UnifiedRecordValue<AsyncRequestRecord>
    implements AsyncRequestRecordValue {
  // structpack-ids[AsyncRequestRecord]: 1,2,3,4
  private final LongProperty keyProp = new LongProperty(1, "KEY_");
  private final LongProperty requestIdProp = new LongProperty(2, "REQUEST_ID_");
  private final EnumProperty<ValueType> valueTypeProp =
      new EnumProperty<>(3, "VALUE_TYPE_", ValueType.class);
  private final ShortProperty valueLifeCycleProp = new ShortProperty(4, "VALUE_LIFE_CYCLE_");

  public AsyncRequestRecord() {
    super(4);
    declareProperty(keyProp)
        .declareProperty(requestIdProp)
        .declareProperty(valueTypeProp)
        .declareProperty(valueLifeCycleProp);
  }

  @Override
  public long getKey() {
    return keyProp.getValue();
  }

  public AsyncRequestRecord setKey(final long key) {
    keyProp.setValue(key);
    return this;
  }

  @Override
  public long getRequestId() {
    return requestIdProp.getValue();
  }

  public AsyncRequestRecord setRequestId(final long requestId) {
    requestIdProp.setValue(requestId);
    return this;
  }

  @Override
  public ValueType getValueType() {
    return valueTypeProp.getValue();
  }

  public AsyncRequestRecord setValueType(final ValueType valueType) {
    valueTypeProp.setValue(valueType);
    return this;
  }

  @Override
  public ValueLifeCycle getValueLifeCycle() {
    final short value = valueLifeCycleProp.getValue();
    return ValueLifeCycle.fromProtocolValue(getValueType(), value);
  }

  public AsyncRequestRecord setValueLifeCycle(final ValueLifeCycle valueLifeCycle) {
    valueLifeCycleProp.setValue(valueLifeCycle.value());
    return this;
  }

  @Override
  protected AsyncRequestRecord newRecord() {
    return new AsyncRequestRecord();
  }
}
