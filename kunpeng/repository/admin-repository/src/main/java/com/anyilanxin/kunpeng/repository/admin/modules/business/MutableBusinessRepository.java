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
 * 业务域可写仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MutableBusinessRepository extends ImmutableBusinessRepository {

  void saveClusterMeta(final BusinessClusterMetaRecord clusterMeta);

  void updateClusterMeta(final BusinessClusterMetaRecord clusterMeta);

  void delayedDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  void saveDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  void updateDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan);

  /**
   * 计划完结落库（COMPLETED/FAILED 共用）：当前计划按记录携带的终态持久化（处理器已置 SUCCEED/FAILED）， GC 指针指向的
   * 前任完结计划及其明细，最后把"最新完结计划"指针前移到当前计划。
   *
   * <p>终态必须落库：单槽闸门按计划记录的 {@code DispatchPlanState} 判定占坑，跳过落库会让完结计划永远停在 EXECUTED 成为僵尸，闸门永久误拒后续调度。
   *
   * @param dispatchPlan 完结计划记录（携带终态与计划 id）
   */
  void dispatchPlanComplete(final BusinessDispatchPlanRecord dispatchPlan);

  void dispatchPlanExecution(final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);

  void dispatchPlanExecutionFail(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);

  void dispatchPlanExecutionSuccess(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);

  /** 长时明细进度续期：仅更新明细记录的进度字段，不置终态、不动 ORDER 索引 */
  void dispatchPlanExecutionProgress(
      final long key, final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);
}
