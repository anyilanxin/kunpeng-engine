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
package com.anyilanxin.kunpeng.repository.admin.modules.admin;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;

/**
 * @author zxuanhong
 * @since
 */
public interface MutableRepositoryAdmin extends ImmutableRepositoryAdmin {
  void saveDispatchPlan(final AdminDispatchPlanRecord dispatchPlan);

  void updateClusterMeta(final AdminClusterMetaRecord clusterMeta);

  void updateDispatchPlan(final AdminDispatchPlanRecord dispatchPlan);

  /** 明细开始执行：以 EXECUTED 状态落库作为在途标记（管理面无 ORDER 索引，靠状态区分未开始明细） */
  void dispatchPlanExecution(final AdminDispatchPlanExecutionRecord dispatchPlanExecution);

  /** 明细执行失败：终态落库（仅更新执行明细列族） */
  void dispatchPlanExecutionFail(
      final long key, final AdminDispatchPlanExecutionRecord dispatchPlanExecution);

  /** 明细执行成功：终态落库（仅更新执行明细列族） */
  void dispatchPlanExecutionSuccess(
      final long key, final AdminDispatchPlanExecutionRecord dispatchPlanExecution);
}
