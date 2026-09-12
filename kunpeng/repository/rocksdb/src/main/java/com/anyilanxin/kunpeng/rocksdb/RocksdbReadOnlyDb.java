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

import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbOptionsUtil;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import org.rocksdb.*;

/**
 * 只读 RocksDB 数据库实现，仅用于创建 snapshot 与校验和，不支持事务与写入操作
 *
 * @author zxuanhong
 */
public final class RocksdbReadOnlyDb<ColumnFamilyType extends ColumnFamilies>
    implements KvStore<ColumnFamilyType> {
  private RocksDB rocksDB;
  private final String dbPath;
  private final RocksdbOptions rocksdbOptions;
  private final List<AutoCloseable> closeables;
  private final ColumnFamilyHandle[] columnFamilyHandles;
  private volatile boolean closed = false;

  /** 构造只读数据库实例并准备默认的 RocksDB 配置 */
  public RocksdbReadOnlyDb(
      final String dbPath,
      final List<AutoCloseable> closeables,
      final ColumnFamilies columnFamilies) {
    this.dbPath = dbPath;
    final int maxColumn =
        Arrays.stream(columnFamilies.allFamily())
            .map(PredefinedColumnFamily::getFamily)
            .mapToInt(v -> v)
            .max()
            .orElse(1);
    columnFamilyHandles = new ColumnFamilyHandle[maxColumn + 1];
    this.closeables = closeables;
    rocksdbOptions =
        RocksdbOptionsUtil.prepareOptions(
            this.closeables, null, 0, new RocksdbConfiguration(), columnFamilies.allFamily());
  }

  /** 以只读模式打开数据库 */
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
    // 以只读方式打开数据库
    final List<ColumnFamilyHandle> handles = new ArrayList<>();
    try {
      rocksDB =
          RocksDB.openReadOnly(
              rocksdbOptions.dbOptions(), dbPath, columnFamilyDescriptors, handles);
      closeables.add(rocksDB);
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
    RocksdbUtil.handleInitializeResult(columnFamilyHandles, handles);
  }

  /** 获取数据库所有存活文件的校验和信息 */
  @Override
  public Map<String, SnapshotFileInfo> getChecksums() {
    return RocksdbUtil.getChecksums(rocksDB);
  }

  /** 只读数据库不支持该操作，直接抛出异常 */
  @Override
  public <
          KeyType extends com.anyilanxin.kunpeng.kvstore.types.KeyType,
          ValueType extends com.anyilanxin.kunpeng.kvstore.types.ValueType>
      ColumnFamily<KeyType, ValueType> createColumnFamily(
          final ColumnFamilyType columnFamilies,
          final TransactionContext transactionContext,
          final KeyType keyInstance,
          final ValueType valueInstance) {
    throw new RuntimeException("Non supported");
  }

  /** 只读数据库不支持该操作，直接抛出异常 */
  @Override
  public TransactionContext createTransactionContext() {
    throw new RuntimeException("Non supported");
  }

  /** 获取指定列族对应的 handle */
  public ColumnFamilyHandle columnFamilyHandle(final ColumnFamilyType columnFamilies) {
    return columnFamilyHandles[columnFamilies.entityFamily()];
  }

  /** 只读数据库不支持该操作，直接抛出异常 */
  public Transaction beginTransaction() {
    throw new RuntimeException("Non supported");
  }

  /** 只读数据库不支持该操作，直接抛出异常 */
  public Transaction beginTransaction(final Transaction transaction) {
    throw new RuntimeException("Non supported");
  }

  /** 通过 checkpoint 在指定目录创建当前数据库的 snapshot */
  @Override
  public void createSnapshot(final File snapshotDir) {
    try (final var checkpoint = Checkpoint.create(rocksDB)) {
      checkpoint.createCheckpoint(snapshotDir.getAbsolutePath());
    } catch (final RocksDBException e) {
      throw new RuntimeException(
          "Failed to take a RocksDB snapshot at '%s'".formatted(snapshotDir), e);
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

  /** 只读数据库不支持该操作，直接抛出异常 */
  @Override
  public void merge(
      final Path fromPath, final Set<ColumnFamilyType> familyTypes, final ColumnCopyType type) {
    throw new RuntimeException("Non supported");
  }

  /** 关闭数据库，并按注册的相反顺序释放所有资源 */
  @Override
  public void close() throws Exception {
    if (closed) {
      return;
    }
    closed = true;
    Collections.reverse(closeables);
    for (final AutoCloseable autoCloseable : closeables) {
      autoCloseable.close();
    }
  }

  /** 获取原始 RocksDB 实例 */
  public RocksDB getRocksdb() {
    return rocksDB;
  }
}
