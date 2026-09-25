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

import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;

/**
 * RocksDB 业务面仓储工厂。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RocksdbBusinessRepositoryFactory implements BusinessRepositoryFactory {
  private final KvStore<BusinessRepositoryColumnFamilies> db;
  private final PartitionSourceMetadata partitionSourceMetadata;
  private final BeanFactory beanFactory;
  private final MeterRegistry meterRegistry;

  public RocksdbBusinessRepositoryFactory(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final PartitionSourceMetadata partitionSourceMetadata,
      final BeanFactory beanFactory,
      final MeterRegistry meterRegistry) {
    this.db = db;
    this.partitionSourceMetadata = partitionSourceMetadata;
    this.beanFactory = beanFactory;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public BusinessRepository create() {
    return new RocksdbBusinessRepository(
        partitionSourceMetadata.partitionId(),
        Set.of(partitionSourceMetadata.sourceId()),
        db,
        beanFactory,
        meterRegistry);
  }
}
