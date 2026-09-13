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
package com.anyilanxin.kunpeng.protocol.admin.record.command.delayed;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_22;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 延迟调度生命周期，描述延迟调度计划从创建、触发到取消的状态流转。
 *
 * @author zxuanhong
 * @since
 */
public enum DelayedLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATED((short) 1, PROCESS_INDEX_56),

  TRIGGER((short) 2, PROCESS_INDEX_57),

  TRIGGERED((short) 3, PROCESS_INDEX_58),

  CANCELED((short) 4, PROCESS_INDEX_59);

  private final short value;
  private final short processIndex;

  DelayedLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATED;
      case 2 -> TRIGGER;
      case 3 -> TRIGGERED;
      case 4 -> CANCELED;
      default -> UNKNOWN;
    };
  }

  public static DelayedLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATED;
      case 2 -> TRIGGER;
      case 3 -> TRIGGERED;
      case 4 -> CANCELED;
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
      case NULL_VAL, TRIGGER -> false;
      case CREATED, TRIGGERED, CANCELED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.DELAYED;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_22;
  }
}
