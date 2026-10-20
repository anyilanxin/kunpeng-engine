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
package com.anyilanxin.kunpeng.repository.business.modules.state;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.state.record.PartitionFrozenState;

/**
 * 冻结标记域仓储实现：数据手术冻结窗口的本地持久化（PARTITION_STATE 列族单行，行存在即冻结、值携带 drain 标志）。 引擎启动时读取恢复冻结态，broker 手术中重启不破防。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class FrozenStateRepository implements RockResourceDataSplit {
  private static final String FROZEN_STATE_KEY = "1";

  private final TransactionContext transaction;
  private final StringType stateKeyDbKey;
  private final PartitionFrozenState frozenState = new PartitionFrozenState();
  private final ColumnFamily<StringType, PartitionFrozenState> stateColumnFamily;

  public FrozenStateRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    this.transaction = transaction;
    stateKeyDbKey = new StringType();
    stateKeyDbKey.wrapString(FROZEN_STATE_KEY);
    stateColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PARTITION_STATE,
            transaction,
            stateKeyDbKey,
            frozenState);
  }

  /** 冻结标记（null=未冻结；值=冻结时的 drain 标志）；读写均包事务（put/delete 必须提交才落盘） */
  public Boolean frozenMarker() {
    final PartitionFrozenState[] holder = new PartitionFrozenState[1];
    transaction.runInTransaction(() -> holder[0] = stateColumnFamily.get(stateKeyDbKey));
    return holder[0] == null ? null : holder[0].isDrain();
  }

  public void markFrozen(final boolean drain) {
    transaction.runInTransaction(
        () -> {
          frozenState.setDrain(drain);
          stateColumnFamily.put(stateKeyDbKey, frozenState);
        });
  }

  public void clearFrozen() {
    transaction.runInTransaction(() -> stateColumnFamily.delete(stateKeyDbKey));
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}
}
