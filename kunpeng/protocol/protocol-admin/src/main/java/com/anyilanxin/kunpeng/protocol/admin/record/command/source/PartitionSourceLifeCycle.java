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
package com.anyilanxin.kunpeng.protocol.admin.record.command.source;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_25;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面分区来源生命周期，描述单条分区来源从应用、转移到完成的状态流转。
 *
 * @author zxuanhong
 * @since
 */
public enum PartitionSourceLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  APPLYING((short) 1, PROCESS_INDEX_66),

  APPLIED((short) 2, PROCESS_INDEX_67),

  TRANSFERRED_REMOVE((short) 5, PROCESS_INDEX_70),

  TRANSFERRING_ADD((short) 3, PROCESS_INDEX_68),

  TRANSFERRED_ADD((short) 4, PROCESS_INDEX_69);

  private final short value;
  private final short processIndex;

  PartitionSourceLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> APPLYING;
      case 2 -> APPLIED;
      case 3 -> TRANSFERRING_ADD;
      case 4 -> TRANSFERRED_ADD;
      case 5 -> TRANSFERRED_REMOVE;
      default -> UNKNOWN;
    };
  }

  public static PartitionSourceLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> APPLYING;
      case 2 -> APPLIED;
      case 3 -> TRANSFERRING_ADD;
      case 4 -> TRANSFERRED_ADD;
      case 5 -> TRANSFERRED_REMOVE;
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
      case NULL_VAL, APPLYING, TRANSFERRING_ADD -> false;
      case APPLIED, TRANSFERRED_ADD, TRANSFERRED_REMOVE -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.PARTITION_SOURCE;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_25;
  }
}
