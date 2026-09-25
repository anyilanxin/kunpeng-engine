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
package com.anyilanxin.kunpeng.broker.client.business;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.ApiRequestEncoder;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;

/**
 * 业务面 broker 请求抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BrokerRequest<VALUE extends RequestRecordValue>
    implements ClientRequest<VALUE> {
  private long key = ApiRequestEncoder.keyNullValue();
  private final RecordType requestType;
  private final ValueType valueType;
  private final ValueLifeCycle lifeCycle;
  private int partitionId = NOT_SPECIFIED_PARTITION;
  private final VALUE value;

  public BrokerRequest(
      final RecordType requestType,
      final ValueType valueType,
      final ValueLifeCycle lifeCycle,
      final VALUE value) {
    this.requestType = requestType;
    this.valueType = valueType;
    this.lifeCycle = lifeCycle;
    this.value = value;
  }

  @Override
  public VALUE getValue() {
    return value;
  }

  @Override
  public void setKey(final long key) {
    this.key = key;
  }

  @Override
  public long key() {
    return key;
  }

  @Override
  public int partitionId() {
    return partitionId;
  }

  @Override
  public void setPartitionId(final int partitionId) {
    this.partitionId = partitionId;
  }

  @Override
  public RecordType requestType() {
    return requestType;
  }

  @Override
  public ValueType valueType() {
    return valueType;
  }

  @Override
  public ValueLifeCycle lifeCycle() {
    return lifeCycle;
  }
}
