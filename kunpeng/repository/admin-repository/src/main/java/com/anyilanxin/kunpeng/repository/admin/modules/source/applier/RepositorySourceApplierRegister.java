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
package com.anyilanxin.kunpeng.repository.admin.modules.source.applier;

import com.anyilanxin.kunpeng.repository.admin.AdminRegisterRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.nodesource.impl.NodeSourceAppliedApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.nodesourcemeta.impl.NodeSourceMetaCreatedApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.nodesourcemeta.impl.NodeSourceMetaUpdatedApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsource.impl.PartitionSourceAppliedApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsource.impl.PartitionSourceTransferredApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsource.impl.PartitionSourceTransferredRemoveApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsourcemeta.impl.PartitionSourceMetaCreatedApplier;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsourcemeta.impl.PartitionSourceMetaUpdatedApplier;

/**
 * @author zxuanhong
 * @since
 */
public class RepositorySourceApplierRegister {

  public static void register(
      final AdminRegisterRepositoryAppliers appliers, final AdminRepository repository) {
    appliers
        .register(new NodeSourceAppliedApplier(repository))
        .register(new NodeSourceMetaCreatedApplier(repository))
        .register(new NodeSourceMetaUpdatedApplier(repository))
        .register(new PartitionSourceAppliedApplier(repository))
        .register(new PartitionSourceTransferredApplier(repository))
        .register(new PartitionSourceTransferredRemoveApplier(repository))
        .register(new PartitionSourceMetaCreatedApplier(repository))
        .register(new PartitionSourceMetaUpdatedApplier(repository));
  }
}
