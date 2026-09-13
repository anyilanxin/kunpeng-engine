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
package com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandApiValueLifeCycle;

/**
 * 管理面调度对外 API 生命周期，副本变更、取消变更与调度查询的请求/响应成对状态。
 *
 * @author zxuanhong
 * @since
 */
public enum AdminDispatchApiValueLifeCycle implements CommandApiValueLifeCycle {
  CHANGE_REPLICATION_REQUEST((short) 0, PROCESS_INDEX_48, RECORD_INDEX_6),
  CHANGE_REPLICATION_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_7),
  CHANGE_CANCEL_REQUEST((short) 2, PROCESS_INDEX_49, RECORD_INDEX_8),
  CHANGE_CANCEL_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_9),
  DISPATCH_QUERY_REQUEST((short) 4, PROCESS_INDEX_50, RECORD_INDEX_10),
  DISPATCH_QUERY_RESPONSE((short) 5, NOT_PROCESS_INDEX, RECORD_INDEX_11);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  AdminDispatchApiValueLifeCycle(
      final short value, final short processIndex, final short recordIndex) {
    this.value = value;
    this.processIndex = processIndex;
    this.recordIndex = recordIndex;
  }

  public short getValueState() {
    return value;
  }

  public static CommandApiValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CHANGE_REPLICATION_REQUEST;
      case 1 -> CHANGE_REPLICATION_RESPONSE;
      case 2 -> CHANGE_CANCEL_REQUEST;
      case 3 -> CHANGE_CANCEL_RESPONSE;
      case 4 -> DISPATCH_QUERY_REQUEST;
      case 5 -> DISPATCH_QUERY_RESPONSE;
      default -> throw new IllegalStateException("Unexpected value: " + value);
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.ADMIN_DISPATCH_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
