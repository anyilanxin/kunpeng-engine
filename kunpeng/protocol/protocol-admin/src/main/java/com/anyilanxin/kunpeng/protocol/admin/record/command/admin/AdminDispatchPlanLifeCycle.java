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
package com.anyilanxin.kunpeng.protocol.admin.record.command.admin;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_0;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 管理面调度计划生命周期，描述计划从创建、执行到完成或失败的状态流转。
 *
 * @author zxuanhong
 * @since
 */
public enum AdminDispatchPlanLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CHANGE_REPLICATION((short) 1, PROCESS_INDEX_0),

  PLAN_DELAYED((short) 2, PROCESS_INDEX_1),

  PLAN_CREATING((short) 3, PROCESS_INDEX_2),

  PLAN_CREATED((short) 4, PROCESS_INDEX_3),

  CANCELING((short) 5, PROCESS_INDEX_4),

  CANCELED((short) 6, PROCESS_INDEX_5),

  EXECUTING((short) 7, PROCESS_INDEX_6),

  EXECUTED((short) 8, PROCESS_INDEX_7),

  COMPLETING((short) 9, PROCESS_INDEX_8),

  COMPLETED((short) 10, PROCESS_INDEX_9),

  FAILING((short) 11, PROCESS_INDEX_10),

  FAILED((short) 12, PROCESS_INDEX_11);

  private final short value;
  private final short processIndex;

  AdminDispatchPlanLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CHANGE_REPLICATION;
      case 2 -> PLAN_DELAYED;
      case 3 -> PLAN_CREATING;
      case 4 -> PLAN_CREATED;
      case 5 -> CANCELING;
      case 6 -> CANCELED;
      case 7 -> EXECUTING;
      case 8 -> EXECUTED;
      case 9 -> COMPLETING;
      case 10 -> COMPLETED;
      case 11 -> FAILING;
      case 12 -> FAILED;
      default -> UNKNOWN;
    };
  }

  public static AdminDispatchPlanLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CHANGE_REPLICATION;
      case 2 -> PLAN_DELAYED;
      case 3 -> PLAN_CREATING;
      case 4 -> PLAN_CREATED;
      case 5 -> CANCELING;
      case 6 -> CANCELED;
      case 7 -> EXECUTING;
      case 8 -> EXECUTED;
      case 9 -> COMPLETING;
      case 10 -> COMPLETED;
      case 11 -> FAILING;
      case 12 -> FAILED;
      default -> NULL_VAL;
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isState() {
    return switch (this) {
      case NULL_VAL, CHANGE_REPLICATION, PLAN_CREATING, CANCELING, EXECUTING, COMPLETING, FAILING ->
          false;
      case PLAN_DELAYED, PLAN_CREATED, CANCELED, EXECUTED, COMPLETED, FAILED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.ADMIN_DISPATCH;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_0;
  }
}
