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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.MutableDistributeParallelRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.applier.RepositoryDistributeParallelApplier;

/**
 * 并行分发完成事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbDistributeParallelCompletedApplierImpl
    implements RepositoryDistributeParallelApplier<DistributeParallelRecord> {
  private final MutableDistributeParallelRepository distribute;

  public RocksdbDistributeParallelCompletedApplierImpl(final BusinessRepository repository) {
    distribute = repository.distributeParallelRepository();
  }

  @Override
  public void applyState(final long key, final DistributeParallelRecord recordValue) {
    distribute.removeDistribute(key);
  }

  @Override
  public DistributeParallelLifeCycle valueState() {
    return DistributeParallelLifeCycle.DISTRIBUTE_COMPLETED;
  }
}
