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
package com.anyilanxin.kunpeng.protocol.admin.record.command.business;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_4;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面调度计划执行明细生命周期，描述单条调度动作从创建、执行到完成或失败的状态流转。
 *
 * @author zxuanhong
 * @since
 */
public enum BusinessDispatchPlanExecutionLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  EXECUTING((short) 1, PROCESS_INDEX_36),

  EXECUTED((short) 2, PROCESS_INDEX_37),

  ACKNOWLEDGE((short) 3, PROCESS_INDEX_38),

  ACKNOWLEDGE_TIMED_OUT((short) 4, PROCESS_INDEX_39),

  SUCCEED((short) 5, PROCESS_INDEX_40),

  FAILED((short) 6, PROCESS_INDEX_41);

  private final short value;
  private final short processIndex;

  BusinessDispatchPlanExecutionLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> EXECUTING;
      case 2 -> EXECUTED;
      case 3 -> ACKNOWLEDGE;
      case 4 -> ACKNOWLEDGE_TIMED_OUT;
      case 5 -> SUCCEED;
      case 6 -> FAILED;
      default -> UNKNOWN;
    };
  }

  public static BusinessDispatchPlanExecutionLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> EXECUTING;
      case 2 -> EXECUTED;
      case 3 -> ACKNOWLEDGE;
      case 4 -> ACKNOWLEDGE_TIMED_OUT;
      case 5 -> SUCCEED;
      case 6 -> FAILED;
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
      case NULL_VAL, EXECUTING, ACKNOWLEDGE, ACKNOWLEDGE_TIMED_OUT -> false;
      case EXECUTED, SUCCEED, FAILED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.BUSINESS_DISPATCH_EXECUTION;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_4;
  }
}
