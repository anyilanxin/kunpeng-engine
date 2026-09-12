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
package com.anyilanxin.kunpeng.rocksdb;

import static org.rocksdb.Status.Code.*;

import com.anyilanxin.kunpeng.kvstore.RepositoryTransaction;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.TransactionOperation;
import com.anyilanxin.kunpeng.kvstore.exception.KvStoreException;
import java.util.EnumSet;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;

/**
 * {@link TransactionContext} 的 RocksDB 实现，负责事务的复用、提交与回滚，并将可恢复的 RocksDB 异常包装为 {@link
 * KvStoreException}
 */
public final class RocksdbTransactionContext implements TransactionContext {

  final RocksdbRepositoryTransaction transaction;
  static final EnumSet<Status.Code> RECOVERABLE_ERROR_CODES =
      EnumSet.of(Ok, Aborted, Expired, IOError, Busy, TimedOut, TryAgain, MergeInProgress);

  RocksdbTransactionContext(final RocksdbRepositoryTransaction rocksdbTransaction) {
    transaction = rocksdbTransaction;
  }

  /**
   * 在事务中执行给定操作；已处于事务中时直接复用当前事务，否则开启新事务，正常结束时提交、出错时 回滚
   *
   * @param operations 要执行的事务操作
   */
  @Override
  public void runInTransaction(final TransactionOperation operations) {
    try {
      if (transaction.isInCurrentTransaction()) {
        operations.run();
      } else {
        runInNewTransaction(operations);
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final RocksDBException rdbex) {
      final String errorMessage = "Unexpected error occurred during RocksDB transaction.";
      if (isRocksDbExceptionRecoverable(rdbex)) {
        throw new KvStoreException(errorMessage, rdbex);
      } else {
        throw new RuntimeException(errorMessage, rdbex);
      }
    } catch (final Exception ex) {
      throw new RuntimeException(
          "Unexpected error occurred during RocksDB transaction operation.", ex);
    }
  }

  /**
   * 获取当前事务；若当前事务已失效则先续借新事务
   *
   * @return 当前事务
   */
  @Override
  public RepositoryTransaction getCurrentTransaction() {
    if (!transaction.isInCurrentTransaction()) {
      transaction.resetTransaction();
    }
    return transaction;
  }

  private void runInNewTransaction(final TransactionOperation operations) throws Exception {
    try {
      transaction.resetTransaction();
      operations.run();
      transaction.commit();
    } catch (final Exception e) {
      try {
        transaction.rollback();
      } catch (final Exception rollbackEx) {
        e.addSuppressed(rollbackEx);
      }
      throw e;
    }
  }

  private boolean isRocksDbExceptionRecoverable(final RocksDBException rdbex) {
    final Status status = rdbex.getStatus();
    return RECOVERABLE_ERROR_CODES.contains(status.getCode());
  }
}
