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

import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * broker 响应编码写出器（面无关）：服务端应答编码前的结果暂存载体。
 *
 * <p>valueType/lifeCycle 为面别协议编号，由构造方从面别枚举换算。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerResponseWriter<T extends UnifiedRecordValue> {
  private T response;
  private final long requestId;
  private final int partitionId;
  private final int valueType;
  private final int lifeCycle;
  private boolean hasData;
  private int code;
  private String message;
  private boolean success;

  public BrokerResponseWriter(
      final long requestId, final int partitionId, final int valueType, final int lifeCycle) {
    this.requestId = requestId;
    this.partitionId = partitionId;
    this.valueType = valueType;
    this.lifeCycle = lifeCycle;
  }

  public T getResponse() {
    return response;
  }

  public void setResponse(final T response) {
    this.response = response;
    success = true;
    hasData = true;
  }

  public void setError(final int code, final String message) {
    this.code = code;
    this.message = message;
    success = false;
  }

  public int getCode() {
    return code;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public boolean hasData() {
    return hasData;
  }

  public long getRequestId() {
    return requestId;
  }

  public int getPartitionId() {
    return partitionId;
  }

  public int getValueType() {
    return valueType;
  }

  public int getLifeCycle() {
    return lifeCycle;
  }
}
