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
package com.anyilanxin.kunpeng.repository.admin.modules.business;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;

/**
 * @author zxuanhong
 * @since
 */
public interface MutableRepositoryBusiness extends ImmutableRepositoryBusiness {

  void saveClusterMeta(final BusinessClusterMetaRecord clusterMeta);

  void updateClusterMeta(final BusinessClusterMetaRecord clusterMeta);

  void delayedDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  void saveDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  void dispatchPlanDelete(final BusinessDispatchPlanRecord dispatchPlan);

  void updateDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  void dispatchPlanComplete(final long dispatchPlanId);

  void dispatchPlanExecution(final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);

  void dispatchPlanExecutionFail(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);

  void dispatchPlanExecutionSuccess(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);
}
