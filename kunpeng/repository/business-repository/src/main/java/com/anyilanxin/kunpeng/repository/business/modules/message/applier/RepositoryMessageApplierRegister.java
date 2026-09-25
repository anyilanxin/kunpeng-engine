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
package com.anyilanxin.kunpeng.repository.business.modules.message.applier;

import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.EnableRegisterRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.business.modules.message.applier.impl.*;

/**
 * 消息域应用器注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RepositoryMessageApplierRegister {

  public static void register(
      final EnableRegisterRepositoryAppliers appliers, final BusinessRepository repository) {
    appliers
        .register(new RocksdbRepositoryMessageSubscriptionCancelledApplierImpl(repository))
        .register(
            new RocksdbRepositoryMessageSubscriptionDistributeCancelledApplierImpl(repository))
        .register(new RocksdbRepositoryMessageSubscriptionCorrelatedApplierImpl(repository))
        .register(new RocksdbRepositoryMessageSubscriptionCreatedApplierImpl(repository))
        .register(new RocksdbRepositoryMessageSubscriptionDistributeCreatedApplierImpl(repository))
        .register(new RocksdbRepositoryMessageDistributeCorrelateFailApplierImpl(repository))
        .register(new RocksdbRepositoryMessageDistributeCorrelateCreatedApplierImpl(repository))
        .register(new RocksdbRepositoryMessageDistributeCorrelateCorrelatedApplierImpl(repository))
        .register(new RocksdbRepositoryMessageDistributeConfirmedApplierImpl(repository));
  }
}
