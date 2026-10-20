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
package com.anyilanxin.kunpeng.repository.admin.modules.business.applier.execution.impl;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.MutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.execution.BusinessDispatchPlanExecutionApplier;

/**
 * 业务面调度执行明细进度续期事件应用器：长时明细（冻结排干/数据传输）的 IN_PROGRESS ack 进度附着到明细记录， 不置终态、不推进状态机。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessDispatchPlanExecutionProgressingApplierImpl
    implements BusinessDispatchPlanExecutionApplier {
  private final MutableBusinessRepository repositoryBusiness;

  public BusinessDispatchPlanExecutionProgressingApplierImpl(final AdminRepository repository) {
    repositoryBusiness = repository.businessRepository();
  }

  @Override
  public void applyState(final long key, final BusinessDispatchPlanExecutionRecord recordValue) {
    repositoryBusiness.dispatchPlanExecutionProgress(key, recordValue);
  }

  @Override
  public BusinessDispatchPlanExecutionLifeCycle lifeCycle() {
    return BusinessDispatchPlanExecutionLifeCycle.PROGRESSING;
  }
}
