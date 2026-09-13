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
package com.anyilanxin.kunpeng.repository.admin.modules.admin.applier.dispatchplan.impl;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.MutableRepositoryAdmin;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.applier.dispatchplan.AdminDispatchPlanApplier;

/**
 * @author zxuanhong
 * @since
 */
public class AdminDispatchPlanCompletedApplierImpl implements AdminDispatchPlanApplier {
  private final MutableRepositoryAdmin repositoryAdmin;

  public AdminDispatchPlanCompletedApplierImpl(final AdminRepository repository) {
    repositoryAdmin = repository.repositoryAdmin();
  }

  @Override
  public void applyState(final long key, final AdminDispatchPlanRecord recordValue) {
    repositoryAdmin.updateDispatchPlan(recordValue);
  }

  @Override
  public AdminDispatchPlanLifeCycle lifeCycle() {
    return AdminDispatchPlanLifeCycle.COMPLETED;
  }
}
