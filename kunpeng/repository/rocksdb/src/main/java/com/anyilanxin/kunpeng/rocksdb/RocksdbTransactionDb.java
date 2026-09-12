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

import static com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil.createFamilyHandle;

import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbOptionsUtil;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import org.rocksdb.*;

/**
 * 支持事务的 RocksDB 数据库实现，可按配置以悲观（TransactionDB）或乐观 （OptimisticTransactionDB）模式打开，并提供列族访问、事务、snapshot
 * 与数据复制合并能力
 *
 * @author zxuanhong
 */
public final class RocksdbTransactionDb<ColumnFamilyType extends ColumnFamilies>
    implements KvStore<ColumnFamilyType> {
  private OptimisticTransactionDB optimisticTransactionDB;
  private TransactionDB transactionDB;
  private RocksDB rocksDB;
  private final String dbPath;
  private final boolean optimisticTransaction;
  private final RocksdbOptions rocksdbOptions;
  private final List<AutoCloseable> closeables;
  private final ColumnFamilyHandle[] columnFamilyHandles;
  private volatile boolean closed = false;

  /** 构造事务数据库实例并准备相关 RocksDB 配置 */
  public RocksdbTransactionDb(
      final String dbPath,
      final ColumnFamilies columnFamilies,
      final boolean optimisticTransaction,
      final MeterRegistry registry,
      final int partitionId,
      final RocksdbConfiguration rocksDbConfiguration,
      final List<AutoCloseable> closeables) {
    this.dbPath = dbPath;
    final int maxColumn =
        Arrays.stream(columnFamilies.allFamily())
            .map(PredefinedColumnFamily::getFamily)
            .mapToInt(v -> v)
            .max()
            .orElse(1);
    columnFamilyHandles = new ColumnFamilyHandle[maxColumn + 1];
    this.optimisticTransaction = optimisticTransaction;
    this.closeables = closeables;
    rocksdbOptions =
        RocksdbOptionsUtil.prepareOptions(
            this.closeables,
            registry,
            partitionId,
            rocksDbConfiguration,
            columnFamilies.allFamily());
  }

  /** 初始化事务数据库 */
  public void open() {
    // 创建数据库目录
    final File dbDir = new File(dbPath);
    if (!dbDir.exists() || !dbDir.isDirectory()) {
      final boolean mkdirs = dbDir.mkdirs();
      if (!mkdirs) {
        throw new RuntimeException("创建数据库目录失败");
      }
    }
    // 配置列族选项
    final List<ColumnFamilyDescriptor> columnFamilyDescriptors =
        RocksdbUtil.columnFamilyDescriptors(rocksdbOptions, dbPath);
    // 打开事务数据库
    final List<ColumnFamilyHandle> handles = new ArrayList<>();
    if (optimisticTransaction) {
      initializeOptimistic(handles, rocksdbOptions, columnFamilyDescriptors);
    } else {
      initializeTransaction(handles, rocksdbOptions, columnFamilyDescriptors);
    }
    RocksdbUtil.handleInitializeResult(columnFamilyHandles, handles);
  }

  private void initializeOptimistic(
      final List<ColumnFamilyHandle> handles,
      final RocksdbOptions rocksdbOptions,
      final List<ColumnFamilyDescriptor> columnFamilyDescriptors) {
    try {
      optimisticTransactionDB =
          OptimisticTransactionDB.open(
              rocksdbOptions.dbOptions(), dbPath, columnFamilyDescriptors, handles);
      rocksDB = optimisticTransactionDB;
      closeables.add(optimisticTransactionDB);
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  private void initializeTransaction(
      final List<ColumnFamilyHandle> handles,
      final RocksdbOptions rocksdbOptions,
      final List<ColumnFamilyDescriptor> columnFamilyDescriptors) {
    try {
      final TransactionDBOptions transactionDBOptions = new TransactionDBOptions();
      transactionDB =
          TransactionDB.open(
              rocksdbOptions.dbOptions(),
              transactionDBOptions,
              dbPath,
              columnFamilyDescriptors,
              handles);
      rocksDB = transactionDB;
      closeables.add(transactionDBOptions);
      closeables.add(transactionDB);
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  /** 获取指定列族对应的 handle，不存在时创建 */
  public ColumnFamilyHandle columnFamilyHandle(final ColumnFamilyType columnFamilies) {
    return createColumnFamilyHandle(columnFamilies);
  }

  /** 创建指定列族的访问实例，用于类型安全地读写该列族的 key-value 数据 */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <
          KeyType extends com.anyilanxin.kunpeng.kvstore.types.KeyType,
          ValueType extends com.anyilanxin.kunpeng.kvstore.types.ValueType>
      ColumnFamily<KeyType, ValueType> createColumnFamily(
          final ColumnFamilyType columnFamilies,
          final TransactionContext transactionContext,
          final KeyType keyInstance,
          final ValueType valueInstance) {
    final ColumnFamilyHandle columnFamilyHandle = createColumnFamilyHandle(columnFamilies);
    return new DefaultColumnFamily(
        rocksdbOptions,
        columnFamilyHandle,
        columnFamilies,
        keyInstance,
        valueInstance,
        transactionContext);
  }

  /** 获取指定列族的 handle，不存在时创建并登记到待关闭资源列表 */
  public ColumnFamilyHandle createColumnFamilyHandle(final ColumnFamilyType columnFamilies) {
    var columnFamilyHandle = columnFamilyHandles[columnFamilies.entityFamily()];
    if (columnFamilyHandle == null) {
      columnFamilyHandle =
          createFamilyHandle(
              columnFamilies,
              rocksDB,
              rocksdbOptions.cfOptions().get(columnFamilies.entityFamily()));
      closeables.add(columnFamilyHandle);
      columnFamilyHandles[columnFamilies.entityFamily()] = columnFamilyHandle;
    }
    return columnFamilyHandle;
  }

  /** 基于新开启的事务创建事务上下文 */
  @Override
  public TransactionContext createTransactionContext() {
    final Transaction transaction = beginTransaction();
    final RocksdbRepositoryTransaction rocksdbTransaction =
        new RocksdbRepositoryTransaction(transaction, this::beginTransaction);
    closeables.add(rocksdbTransaction);
    return new RocksdbTransactionContext(rocksdbTransaction);
  }

  /** 开启一个新事务 */
  public Transaction beginTransaction() {
    ensureOpen();
    if (optimisticTransaction) {
      return optimisticTransactionDB.beginTransaction(rocksdbOptions.writeOptions());
    }
    return transactionDB.beginTransaction(rocksdbOptions.writeOptions());
  }

  /** 基于旧事务开启新事务（复用旧事务资源） */
  public Transaction beginTransaction(final Transaction oldTransaction) {
    ensureOpen();
    if (optimisticTransaction) {
      return optimisticTransactionDB.beginTransaction(
          rocksdbOptions.writeOptions(), oldTransaction);
    }
    return transactionDB.beginTransaction(rocksdbOptions.writeOptions(), oldTransaction);
  }

  /** 通过 checkpoint 在指定目录创建当前数据库的 snapshot */
  @Override
  public void createSnapshot(final File snapshotDir) {
    try (final Checkpoint checkpoint = Checkpoint.create(rocksDB)) {
      try {
        checkpoint.createCheckpoint(snapshotDir.getAbsolutePath());
      } catch (final RocksDBException rocksException) {
        throw new RuntimeException(rocksException);
      }
    }
  }

  /** 将源路径数据库中指定列族的数据复制到目标路径的新数据库 */
  @Override
  public void createCopy(
      final Path fromPath,
      final Path toPath,
      final Set<ColumnFamilyType> familyTypes,
      final ColumnCopyType type) {
    RocksdbUtil.createCopy(fromPath, toPath, familyTypes, type);
  }

  /** 将源路径数据库中指定列族的数据合并到当前数据库 */
  @Override
  public void merge(
      final Path fromPath, final Set<ColumnFamilyType> familyTypes, final ColumnCopyType type) {
    RocksdbUtil.merge(fromPath, this, familyTypes, type);
  }

  /** 获取数据库所有存活文件的校验和信息 */
  @Override
  public Map<String, SnapshotFileInfo> getChecksums() {
    return RocksdbUtil.getChecksums(rocksDB);
  }

  /** 关闭数据库，并按注册的相反顺序释放所有资源 */
  @Override
  public synchronized void close() throws Exception {
    if (closed) {
      return;
    }
    closed = true;
    Collections.reverse(closeables);
    for (final AutoCloseable autoCloseable : closeables) {
      autoCloseable.close();
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("RocksDB database at " + dbPath + " is already closed");
    }
  }

  /** 获取原始 RocksDB 实例 */
  public RocksDB getRocksdb() {
    return rocksDB;
  }
}
