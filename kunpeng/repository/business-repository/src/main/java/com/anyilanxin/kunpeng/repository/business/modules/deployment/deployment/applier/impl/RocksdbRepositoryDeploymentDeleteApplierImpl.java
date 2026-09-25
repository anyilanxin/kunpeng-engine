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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.DeploymentLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.MutableDeploymentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.applier.RepositoryDeploymentApplier;

/**
 * 部署删除事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryDeploymentDeleteApplierImpl
    implements RepositoryDeploymentApplier<DeploymentRecord> {
  private final MutableDeploymentRepository deployment;

  public RocksdbRepositoryDeploymentDeleteApplierImpl(final BusinessRepository repository) {
    deployment = repository.deploymentRepository();
  }

  @Override
  public void applyState(final long key, final DeploymentRecord recordValue) {
    deployment.delete(key);
  }

  @Override
  public DeploymentLifeCycle valueState() {
    return DeploymentLifeCycle.DELETED;
  }
}
