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

import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;

/**
 * broker 响应（面无关）：统一封装解码结果 + 已物化的记录实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerResponse<VALUE extends ResponseRecordValue> {
  private final VALUE value;
  private final boolean success;
  private final boolean hasData;
  private final int code;
  private final String message;

  public BrokerResponse(final ApiResponseReader responseReader, final VALUE value) {
    hasData = responseReader.hasData();
    success = responseReader.success();
    code = responseReader.code();
    message = responseReader.message();
    this.value = value;
  }

  public VALUE getValue() {
    return value;
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean isError() {
    return !success;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }

  public boolean hasData() {
    return hasData;
  }
}
