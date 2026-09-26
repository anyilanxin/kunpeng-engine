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

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * RocksDB 管理面仓储工厂。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RocksdbAdminRepositoryFactory implements AdminRepositoryFactory {
  private final KvStore<AdminRepositoryColumnFamilies> db;
  private final RaftPartitionSource partitionSource;
  private final MeterRegistry meterRegistry;

  public RocksdbAdminRepositoryFactory(
      final KvStore<AdminRepositoryColumnFamilies> db,
      final RaftPartitionSource partitionSource,
      final MeterRegistry meterRegistry) {
    this.db = db;
    this.partitionSource = partitionSource;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public AdminRepository create() {
    return new RocksdbAdminRepository(partitionSource, db, meterRegistry);
  }
}
