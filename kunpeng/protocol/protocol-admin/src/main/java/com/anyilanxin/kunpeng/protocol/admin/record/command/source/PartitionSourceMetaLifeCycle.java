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

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_26;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面分区来源元数据生命周期，描述分区来源元数据从创建到更新的状态流转。
 *
 * @author zxuanhong
 * @since
 */
public enum PartitionSourceMetaLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATING((short) 1, PROCESS_INDEX_71),

  CREATED((short) 2, PROCESS_INDEX_72),

  UPDATING((short) 3, PROCESS_INDEX_73),

  UPDATED((short) 4, PROCESS_INDEX_74),
  ;

  private final short value;
  private final short processIndex;

  PartitionSourceMetaLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
      default -> UNKNOWN;
    };
  }

  public static PartitionSourceMetaLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
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
      case NULL_VAL, CREATING, UPDATING -> false;
      case CREATED, UPDATED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.PARTITION_SOURCE_META;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_26;
  }
}
