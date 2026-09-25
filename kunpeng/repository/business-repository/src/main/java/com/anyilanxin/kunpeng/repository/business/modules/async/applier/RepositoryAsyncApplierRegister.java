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
package com.anyilanxin.kunpeng.repository.business.modules.async.applier;

import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.EnableRegisterRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.business.modules.async.applier.impl.RocksdbRepositoryAsyncAddApplierImpl;
import com.anyilanxin.kunpeng.repository.business.modules.async.applier.impl.RocksdbRepositoryAsyncCompletedApplierImpl;

/**
 * 异步域应用器注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RepositoryAsyncApplierRegister {

  public static void register(
      final EnableRegisterRepositoryAppliers appliers, final BusinessRepository repository) {
    appliers
        .register(new RocksdbRepositoryAsyncAddApplierImpl(repository))
        .register(new RocksdbRepositoryAsyncCompletedApplierImpl(repository));
  }
}
