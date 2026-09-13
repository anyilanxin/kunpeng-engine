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

import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;

/**
 * 调度计划执行明细记录契约，描述所属计划执行 ID、执行顺序、调度起止时间、执行状态与补充说明。
 *
 * @author zxuanhong
 * @since
 */
public interface AdminDispatchPlanExecutionRecordValue extends PartitionExecutionRecordValue {
  long getDueDate();

  int getExecutionOrder();

  byte[] getPlanData();

  long getDispatchStartTime();

  long getDispatchEndTime();

  DispatchExecutionState getDispatchExecutionState();

  String getDispatchMessage();

  /** 本条明细的目标执行成员 */
  String getDispatchMemberId();
}
