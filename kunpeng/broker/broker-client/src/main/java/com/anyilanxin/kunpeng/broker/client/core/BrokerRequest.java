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
package com.anyilanxin.kunpeng.broker.client.core;

import static com.anyilanxin.kunpeng.protocol.common.Protocol.DEPLOYMENT_PARTITION;

import com.anyilanxin.kunpeng.broker.client.core.protocol.ApiRequestEncoder;
import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;

/**
 * broker 请求抽象基类（面无关）：请求类别/值类型/生命状态均为协议编号。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BrokerRequest<VALUE extends RequestRecordValue> {
  public static final int RANDOM_PARTITION = 0;
  public static final int DISTRIBUTE_PARTITION = DEPLOYMENT_PARTITION;
  public static final int NOT_SPECIFIED_PARTITION = -1;

  private long key = ApiRequestEncoder.keyNullValue();
  private final int requestType;
  private final int valueType;
  private final int lifeCycle;
  private int partitionId = NOT_SPECIFIED_PARTITION;
  private final VALUE value;

  protected BrokerRequest(
      final int requestType, final int valueType, final int lifeCycle, final VALUE value) {
    this.requestType = requestType;
    this.valueType = valueType;
    this.lifeCycle = lifeCycle;
    this.value = value;
  }

  public VALUE getValue() {
    return value;
  }

  public void setKey(final long key) {
    this.key = key;
  }

  public long key() {
    return key;
  }

  public int partitionId() {
    return partitionId;
  }

  public void setPartitionId(final int partitionId) {
    this.partitionId = partitionId;
  }

  public int requestType() {
    return requestType;
  }

  public int valueType() {
    return valueType;
  }

  public int lifeCycle() {
    return lifeCycle;
  }
}
