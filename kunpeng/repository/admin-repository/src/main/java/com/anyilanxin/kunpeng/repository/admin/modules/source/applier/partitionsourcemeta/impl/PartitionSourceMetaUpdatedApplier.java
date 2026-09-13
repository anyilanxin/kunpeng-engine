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
package com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsourcemeta.impl;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.MutableRepositorySource;
import com.anyilanxin.kunpeng.repository.admin.modules.source.applier.partitionsourcemeta.PartitionSourceMetaApplier;

/**
 * @author zxuanhong
 * @since
 */
public class PartitionSourceMetaUpdatedApplier implements PartitionSourceMetaApplier {
  private final MutableRepositorySource repositorySource;

  public PartitionSourceMetaUpdatedApplier(final AdminRepository repository) {
    repositorySource = repository.repositorySource();
  }

  @Override
  public PartitionSourceMetaLifeCycle lifeCycle() {
    return PartitionSourceMetaLifeCycle.UPDATED;
  }

  @Override
  public void applyState(final long key, final PartitionSourceMetaRecord recordValue) {
    repositorySource.update(recordValue);
  }
}
