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
package com.anyilanxin.kunpeng.modules.common.endpoints;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;

/**
 * 调度计划执行明细（管理面与业务面通用）。
 *
 * @author zxuanhong
 * @since
 */
public record DispatchPlanExecution(
    long dispatchPlanExecutionId,
    int executionOrder,
    long dueDate,
    long dispatchStartTime,
    long dispatchEndTime,
    String dispatchExecutionState,
    String dispatchMessage,
    String dispatchMemberId,
    String partitionType,
    String executionType,
    String executionMemberId,
    long dispatchPlanId) {

  public static DispatchPlanExecution of(final AdminDispatchPlanExecutionRecord record) {
    return new DispatchPlanExecution(
        record.getDispatchPlanExecutionId(),
        record.getExecutionOrder(),
        record.getDueDate(),
        record.getDispatchStartTime(),
        record.getDispatchEndTime(),
        name(record.getDispatchExecutionState()),
        record.getDispatchMessage(),
        record.getDispatchMemberId(),
        name(record.getPartitionType()),
        name(record.getExecutionType()),
        record.executionMemberId(),
        record.getDispatchPlanId());
  }

  public static DispatchPlanExecution of(final BusinessDispatchPlanExecutionRecord record) {
    return new DispatchPlanExecution(
        record.getDispatchPlanExecutionId(),
        record.getExecutionOrder(),
        record.getDueDate(),
        record.getDispatchStartTime(),
        record.getDispatchEndTime(),
        name(record.getDispatchExecutionState()),
        record.getDispatchMessage(),
        record.getDispatchMemberId(),
        name(record.getPartitionType()),
        name(record.getExecutionType()),
        record.executionMemberId(),
        record.getDispatchPlanId());
  }

  private static String name(final Enum<?> value) {
    return value == null ? null : value.name();
  }
}
