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
package com.anyilanxin.kunpeng.repository.business;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.applier.RepositoryActivityInstanceApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.async.applier.RepositoryAsyncApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.batch.applier.RepositoryBatchApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.delay.applier.RepositoryDelayApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.applier.RepositoryDeploymentApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.applier.RepositoryDistributeParallelApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.applier.RepositoryDistributeSerialApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.applier.RepositoryHistoryCleanupApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.incident.applier.RepositoryIncidentApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.job.applier.RepositoryJobApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.message.applier.RepositoryMessageApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.applier.RepositoryProcessInstanceApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.signal.applier.RepositorySignalApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.timer.applier.RepositoryTimerApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.applier.RepositoryUserTaskApplierRegister;
import com.anyilanxin.kunpeng.repository.business.modules.variable.applier.RepositoryVariableApplierRegister;

/**
 * RocksDB 业务面应用器集合实现：按 Record 类型分发应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class RocksdbBusinessRepositoryApplier implements EnableRegisterRepositoryAppliers {
  private final BusinessRecordApplierMap applierMap;
  private final BusinessRepository repository;

  RocksdbBusinessRepositoryApplier(final BusinessRepository repository) {
    this.repository = repository;
    applierMap = new BusinessRecordApplierMap();
    initRegister();
  }

  private void initRegister() {
    RepositoryBatchApplierRegister.register(this, repository);
    RepositoryJobApplierRegister.register(this, repository);
    RepositoryTimerApplierRegister.register(this, repository);
    RepositorySignalApplierRegister.register(this, repository);
    RepositoryMessageApplierRegister.register(this, repository);
    RepositoryAsyncApplierRegister.register(this, repository);
    RepositoryDelayApplierRegister.register(this, repository);
    RepositoryDeploymentApplierRegister.register(this, repository);
    RepositoryDistributeParallelApplierRegister.register(this, repository);
    RepositoryDistributeSerialApplierRegister.register(this, repository);
    RepositoryHistoryCleanupApplierRegister.register(this, repository);
    RepositoryProcessInstanceApplierRegister.register(this, repository);
    RepositoryActivityInstanceApplierRegister.register(this, repository);
    RepositoryVariableApplierRegister.register(this, repository);
    RepositoryUserTaskApplierRegister.register(this, repository);
    RepositoryIncidentApplierRegister.register(this, repository);
  }

  @Override
  public RocksdbBusinessRepositoryApplier register(final BusinessApplier applier) {
    final ValueType valueType = applier.valueType();
    final ValueLifeCycle valueState = applier.valueState();
    if (valueType == null) {
      throw new IllegalStateException("value type is null");
    }
    if (valueState == null) {
      throw new IllegalStateException("value state is null");
    }
    applierMap.put(RecordType.EVENT, applier.valueType(), applier.valueState(), applier);
    return this;
  }

  @Override
  public void applyState(
      final long key,
      final ValueType valueType,
      final ValueLifeCycle valueState,
      final RecordValue recordValue) {
    final BusinessApplier applier = applierMap.get(RecordType.EVENT, valueType, valueState);
    if (applier != null) {
      applier.applyState(key, recordValue);
    }
  }
}
