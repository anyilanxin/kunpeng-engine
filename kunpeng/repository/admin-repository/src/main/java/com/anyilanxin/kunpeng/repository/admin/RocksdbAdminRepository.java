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

import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.MutableRepositoryAdmin;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.RepositoryAdmin;
import com.anyilanxin.kunpeng.repository.admin.modules.business.MutableRepositoryBusiness;
import com.anyilanxin.kunpeng.repository.admin.modules.business.RepositoryBusiness;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.MutableRepositoryDelayed;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.RepositoryDelayed;
import com.anyilanxin.kunpeng.repository.admin.modules.key.MutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.key.RepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.position.MutableRepositoryPosition;
import com.anyilanxin.kunpeng.repository.admin.modules.position.RepositoryPosition;
import com.anyilanxin.kunpeng.repository.admin.modules.source.MutableRepositorySource;
import com.anyilanxin.kunpeng.repository.admin.modules.source.RepositorySource;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;

/**
 * @author zxuanhong
 * @since
 */
final class RocksdbAdminRepository implements AdminRepository {
  private final TransactionContext transaction;
  private final MutableRepositoryAdmin repositoryAdmin;
  private final MutableRepositoryBusiness repositoryBusiness;
  private final MutableRepositoryKey repositoryKey;
  private final MutableRepositoryPosition repositoryPosition;
  private final MutableRepositoryDelayed repositoryDelayed;
  private final MutableRepositorySource repositorySource;
  private final AdminRepositoryAppliers appliers;

  public RocksdbAdminRepository(
      final int positionId,
      final Set<Integer> resourceIds,
      final KvStore<AdminRepositoryColumnFamilies> db,
      final MeterRegistry meterRegistry) {
    this(positionId, resourceIds, db, db.createTransactionContext(), meterRegistry);
  }

  public RocksdbAdminRepository(
      final int positionId,
      final Set<Integer> resourceIds,
      final KvStore<AdminRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final MeterRegistry meterRegistry) {
    this.transaction = transaction;
    repositoryAdmin = new RepositoryAdmin(db, transaction);
    repositoryBusiness = new RepositoryBusiness(db, transaction);
    repositoryKey = new RepositoryKey(db, transaction);
    repositoryPosition = new RepositoryPosition(db, transaction);
    repositoryDelayed = new RepositoryDelayed(db, transaction);
    repositorySource = new RepositorySource(db, transaction);
    appliers = new RocksdbAdminRepositoryApplierAdmin(this);
  }

  @Override
  public TransactionContext getContext() {
    return transaction;
  }

  @Override
  public AdminRepositoryAppliers getAppliers() {
    return appliers;
  }

  @Override
  public MutableRepositoryAdmin repositoryAdmin() {
    return repositoryAdmin;
  }

  @Override
  public MutableRepositoryBusiness repositoryBusiness() {
    return repositoryBusiness;
  }

  @Override
  public MutableRepositoryDelayed repositoryDelayed() {
    return repositoryDelayed;
  }

  @Override
  public MutableRepositoryKey repositoryKey() {
    return repositoryKey;
  }

  @Override
  public MutableRepositoryPosition repositoryPosition() {
    return repositoryPosition;
  }

  @Override
  public MutableRepositorySource repositorySource() {
    return repositorySource;
  }
}
