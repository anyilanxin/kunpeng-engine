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
package com.anyilanxin.kunpeng.repository.admin.modules.business.applier;

import com.anyilanxin.kunpeng.repository.admin.AdminRegisterRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.clustermeta.impl.BusinessClusterMetaCreatedApplierImpl;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.clustermeta.impl.BusinessClusterMetaDeletedApplierImpl;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.clustermeta.impl.BusinessClusterMetaUpdatedApplierImpl;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.dispatchplan.impl.*;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.execution.impl.BusinessDispatchPlanExecutionExecutedApplierImpl;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.execution.impl.BusinessDispatchPlanExecutionFailedApplierImpl;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.execution.impl.BusinessDispatchPlanExecutionSucceedApplierImpl;

/**
 * @author zxuanhong
 * @since
 */
public class RepositoryBusinessApplierRegister {

  public static void register(
      final AdminRegisterRepositoryAppliers appliers, final AdminRepository repository) {
    appliers
        .register(new BusinessDispatchPlanCanceledApplierImpl(repository))
        .register(new BusinessDispatchPlanExecutedApplierImpl(repository))
        .register(new BusinessClusterMetaCreatedApplierImpl(repository))
        .register(new BusinessClusterMetaDeletedApplierImpl(repository))
        .register(new BusinessClusterMetaUpdatedApplierImpl(repository))
        .register(new BusinessDispatchPlanCompletedApplierImpl(repository))
        .register(new BusinessDispatchPlanCreatedApplierImpl(repository))
        .register(new BusinessDispatchPlanDelayedApplierImpl(repository))
        .register(new BusinessDispatchPlanExecutionExecutedApplierImpl(repository))
        .register(new BusinessDispatchPlanExecutionFailedApplierImpl(repository))
        .register(new BusinessDispatchPlanExecutionSucceedApplierImpl(repository))
        .register(new BusinessDispatchPlanFailedApplierImpl(repository));
  }
}
