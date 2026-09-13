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
package com.anyilanxin.kunpeng.repository.admin;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.applier.RepositoryAdminApplierRegister;
import com.anyilanxin.kunpeng.repository.admin.modules.business.applier.RepositoryBusinessApplierRegister;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.applier.RepositoryDelayedApplierRegister;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.RepositorySourceApplierRegister;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class RocksdbAdminRepositoryApplierAdmin implements AdminRegisterRepositoryAppliers {
  private final AdminRecordApplierMap applierMap;
  private final AdminRepository repository;

  RocksdbAdminRepositoryApplierAdmin(final AdminRepository repository) {
    this.repository = repository;
    applierMap = new AdminRecordApplierMap();
    initRegister();
  }

  private void initRegister() {
    RepositoryAdminApplierRegister.register(this, repository);
    RepositoryBusinessApplierRegister.register(this, repository);
    RepositoryDelayedApplierRegister.register(this, repository);
    RepositorySourceApplierRegister.register(this, repository);
  }

  @Override
  public RocksdbAdminRepositoryApplierAdmin register(final AdminApplier applier) {
    final AdminValueType valueType = applier.valueType();
    final AdminValueLifeCycle valueState = applier.lifeCycle();
    if (valueType == null) {
      throw new IllegalStateException("value type is null");
    }
    if (valueState == null) {
      throw new IllegalStateException("value state is null");
    }
    applierMap.put(RecordType.EVENT, applier.valueType(), applier.lifeCycle(), applier);
    return this;
  }

  @Override
  public void applyState(
      final long key,
      final AdminValueType valueType,
      final AdminValueLifeCycle valueState,
      final RecordValue recordValue) {
    final AdminApplier applier = applierMap.get(RecordType.EVENT, valueType, valueState);
    if (applier != null) {
      applier.applyState(key, recordValue);
    }
  }
}
