/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.protocol.admin.record.command.common;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;

/**
 * 分区冻结命令记录契约（数据手术前置：关写入入口、停内部触发、两段位点排水）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionFreezeRecordValue extends PartitionExecutionRecordValue {
  /** 分区组 */
  String getPartitionGroup();

  /** 分区 id */
  int getPartitionId();

  /** 是否等待 sink 排干：销毁侧 true（引擎+sink 两段排水），存活侧 false（仅引擎静止） */
  boolean isDrain();
}
