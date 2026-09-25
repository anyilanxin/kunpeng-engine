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
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * 业务面 broker 响应编码写出器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerResponseWriter<T extends UnifiedRecordValue> {
  private T response;
  private final long requestId;
  private final int partitionId;
  private final ValueType valueType;
  private final ValueLifeCycle lifeCycle;
  private boolean hasData;
  private int code;
  private String message;
  private boolean success;

  public BrokerResponseWriter(
      final long requestId, final int partitionId, final ValueLifeCycle lifeCycle) {
    this.requestId = requestId;
    this.partitionId = partitionId;
    this.lifeCycle = lifeCycle;
    if (lifeCycle != null) {
      valueType = lifeCycle.getValueType();
    } else {
      valueType = null;
    }
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

  public ValueType getValueType() {
    return valueType;
  }

  public ValueLifeCycle getLifeCycle() {
    return lifeCycle;
  }
}
