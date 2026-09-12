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

import com.anyilanxin.kunpeng.kvstore.RepositoryTransaction;
import com.anyilanxin.kunpeng.kvstore.TransactionOperation;
import com.anyilanxin.kunpeng.kvstore.exception.KvStoreException;
import java.lang.reflect.Field;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksObject;
import org.rocksdb.Status;
import org.rocksdb.Transaction;

/** RocksDB 事务的仓储级封装，持有当前事务及其 native handle，支持事务的续借、执行、提交与回滚 */
public final class RocksdbRepositoryTransaction implements RepositoryTransaction, AutoCloseable {

  private static final Field NATIVE_HANDLE_FIELD;

  static {
    try {
      NATIVE_HANDLE_FIELD = RocksObject.class.getDeclaredField("nativeHandle_");
      NATIVE_HANDLE_FIELD.setAccessible(true);
    } catch (final NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean inCurrentTransaction;
  private Transaction transaction;
  private long nativeHandle;
  private final TransactionRenovator transactionRenovator;

  /** 使用给定事务与事务续借器构造实例 */
  public RocksdbRepositoryTransaction(
      final Transaction transaction, final TransactionRenovator transactionRenovator) {
    this.transaction = transaction;
    nativeHandle = getNativeHandle(transaction);
    this.transactionRenovator = transactionRenovator;
  }

  /** 通过 renovator 续借事务并刷新 native handle，同时标记为当前事务 */
  void resetTransaction() {
    transaction = transactionRenovator.renewTransaction(transaction);
    nativeHandle = getNativeHandle(transaction);
    inCurrentTransaction = true;
  }

  private static long getNativeHandle(final Transaction transaction) {
    try {
      return NATIVE_HANDLE_FIELD.getLong(transaction);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  long getNativeHandle() {
    return nativeHandle;
  }

  boolean isInCurrentTransaction() {
    return inCurrentTransaction;
  }

  /**
   * 在当前事务中执行给定操作，可恢复的 RocksDB 异常会被包装为 {@link KvStoreException} 抛出
   *
   * @param operations 要执行的事务操作
   * @throws Exception 操作执行过程中发生的异常
   */
  @Override
  public void run(final TransactionOperation operations) throws Exception {
    try {
      operations.run();
    } catch (final RocksDBException rdbex) {
      final String errorMessage = "Unexpected error occurred during RocksDB transaction commit.";
      if (isRocksDbExceptionRecoverable(rdbex)) {
        throw new KvStoreException(errorMessage, rdbex);
      }
      throw rdbex;
    }
  }

  /**
   * 提交当前事务，无论成败都会重置当前事务标记
   *
   * @throws RocksDBException RocksDB 提交失败时抛出
   */
  @Override
  public void commit() throws RocksDBException {
    try {
      transaction.commit();
    } catch (final RocksDBException rdbex) {
      final String errorMessage = "Unexpected error occurred during RocksDB transaction commit.";
      if (isRocksDbExceptionRecoverable(rdbex)) {
        throw new KvStoreException(errorMessage, rdbex);
      }
      throw rdbex;
    } finally {
      inCurrentTransaction = false;
    }
  }

  /** 获取当前原始 RocksDB 事务对象 */
  public Transaction getTransaction() {
    return transaction;
  }

  /**
   * 回滚当前事务，无论成败都会重置当前事务标记
   *
   * @throws RocksDBException RocksDB 回滚失败时抛出
   */
  @Override
  public void rollback() throws RocksDBException {
    try {
      transaction.rollback();
    } catch (final RocksDBException rdbex) {
      final String errorMessage = "Unexpected error occurred during RocksDB transaction rollback.";
      if (isRocksDbExceptionRecoverable(rdbex)) {
        throw new KvStoreException(errorMessage, rdbex);
      }
      throw rdbex;
    } finally {
      inCurrentTransaction = false;
    }
  }

  /** 关闭底层事务 */
  @Override
  public void close() {
    transaction.close();
  }

  private boolean isRocksDbExceptionRecoverable(final RocksDBException rdbex) {
    final Status status = rdbex.getStatus();
    return RocksdbTransactionContext.RECOVERABLE_ERROR_CODES.contains(status.getCode());
  }
}
