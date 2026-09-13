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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import java.util.Set;

/**
 * 单一调度类型的执行计划生成策略，每种 {@link BusinessDispatchType} 对应一个实现。
 *
 * @author zxuanhong
 * @since
 */
public interface DispatchPlanGenerator {

  /** 本生成器支持的调度类型 */
  BusinessDispatchType dispatchType();

  /**
   * 依据计划记录中的调度类型参数与历史拓扑制定执行计划。
   *
   * @param plan 携带调度类型、历史拓扑（oldMeta/oldPartitionsCount/oldReplicationFactor）与期望值 的计划记录
   * @param memberIds 参与分配的成员池，由调用方保证满足副本分配需要
   */
  BusinessDispatchPlanRecord createPlan(BusinessDispatchPlanRecord plan, Set<MemberId> memberIds);
}
