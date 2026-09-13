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

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;

/**
 * 业务调度计划记录契约，描述计划 ID、依次执行的调度明细列表以及计划所基于的分区组拓扑快照。
 *
 * @author zxuanhong
 * @since
 */
public interface DelayedRecordValue extends RecordValue {

  long getDelayedId();

  PartitionType getPartitionType();

  long getDueDate();

  DelayedType getDelayedType();

  long getDispatchPlanId();

  long getDispatchPlanExecutionId();
}
