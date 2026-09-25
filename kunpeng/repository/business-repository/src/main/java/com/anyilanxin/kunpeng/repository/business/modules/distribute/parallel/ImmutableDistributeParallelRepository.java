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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;

/**
 * 并行分发域只读仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableDistributeParallelRepository extends ResourceDataSplit {

  DistributeParallelRecord getDistribute(long key);

  boolean haveDistributeAck(final long key);

  void foreachRetriableDistribution(WaitDistributionVisitor visitor);

  void foreachRetriableDistributionAfter(WaitDistributionVisitor visitor);

  @FunctionalInterface
  interface WaitDistributionVisitor {
    boolean visit(final long distributionKey, final DistributeParallelRecord distributeRecord);
  }
}
