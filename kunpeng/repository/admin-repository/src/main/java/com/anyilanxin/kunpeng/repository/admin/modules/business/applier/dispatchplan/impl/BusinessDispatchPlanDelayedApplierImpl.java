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
package com.anyilanxin.kunpeng.repository.admin.modules.business.applier.dispatchplan.impl;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.MutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.dispatchplan.BusinessDispatchPlanApplier;

/**
 * 业务面调度计划延迟事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessDispatchPlanDelayedApplierImpl implements BusinessDispatchPlanApplier {
  private final MutableBusinessRepository repositoryBusiness;

  public BusinessDispatchPlanDelayedApplierImpl(final AdminRepository repository) {
    repositoryBusiness = repository.businessRepository();
  }

  @Override
  public void applyState(final long key, final BusinessDispatchPlanRecord recordValue) {
    repositoryBusiness.delayedDispatchPlan(recordValue);
  }

  @Override
  public BusinessDispatchPlanLifeCycle lifeCycle() {
    return BusinessDispatchPlanLifeCycle.PLAN_DELAYED;
  }
}
