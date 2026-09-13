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
package com.anyilanxin.kunpeng.rocksdb.util;

import static com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily.fromColumnFamilyName;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;
import com.anyilanxin.kunpeng.rocksdb.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.rocksdb.*;
import org.slf4j.Logger;

/**
 * RocksDB 工具类，提供列族 handle 创建、文件校验和计算、列族数据复制与合并等通用能力
 *
 * @author zxuanhong
 */
public final class RocksdbUtil {
  private static final Logger LOG = RocksdbLoggers.ROCKSDB_LOGGER;

  private RocksdbUtil() {}

  /**
   * 在数据库中创建指定列族的 handle
   *
   * @param columnFamilyType 列族类型
   * @param rocksDB 目标数据库
   * @param columnFamilyOptions 列族选项
   * @return 创建的列族 handle
   */
  public static <ColumnFamilyType extends ColumnFamilies> ColumnFamilyHandle createFamilyHandle(
      final ColumnFamilyType columnFamilyType,
      final RocksDB rocksDB,
      final ColumnFamilyOptions columnFamilyOptions) {
    final ColumnFamilyDescriptor columnFamilyDescriptor =
        new ColumnFamilyDescriptor(columnFamilyType.entityFamilyName(), columnFamilyOptions);
    try {
      return rocksDB.createColumnFamily(columnFamilyDescriptor);
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 获取数据库所有存活文件的校验和信息（过滤掉无校验和的文件）
   *
   * @param rocksDB 目标数据库
   * @return 文件名到快照文件信息的映射
   */
  public static Map<String, SnapshotFileInfo> getChecksums(final RocksDB rocksDB) {
    return rocksDB.getLiveFilesMetaData().stream()
        .filter(fileMetaData -> fileMetaData.fileChecksum().length != 0)
        .collect(
            Collectors.toMap(RocksdbUtil::getMetadataName, RocksdbUtil::rocksDBChecksumAsLong));
  }

  private static String getMetadataName(final LiveFileMetaData fileMetaData) {
    return fileMetaData.fileName().substring(1);
  }

  private static SnapshotFileInfo rocksDBChecksumAsLong(final LiveFileMetaData fileMetaData) {
    final var checksumBytes = fileMetaData.fileChecksum();
    final long checksum =
        Integer.toUnsignedLong(ByteBuffer.wrap(checksumBytes).order(ByteOrder.BIG_ENDIAN).getInt());
    final long size = fileMetaData.size();
    return new SnapshotFileInfo(checksum, size);
  }

  /**
   * 将源路径数据库中指定列族的数据复制到目标路径的新数据库
   *
   * @param fromPath 源数据库路径
   * @param toPath 目标数据库路径
   * @param familyTypes 要复制的列族
   * @param type 0-基于列族，1-基于虚拟列族
   */
  public static <ColumnFamilyType extends ColumnFamilies> void createCopy(
      final Path fromPath,
      final Path toPath,
      final Set<ColumnFamilyType> familyTypes,
      final ColumnCopyType type) {
    final DefaultRocksdbFactory<ColumnFamilyType> factory = new DefaultRocksdbFactory<>();
    final ColumnFamilyType columnFamilies = familyTypes.iterator().next();
    try (final RocksdbTransactionDb<ColumnFamilyType> newRocksdbDb =
            factory.createDb(
                toPath.toString(), null, 0, new RocksdbConfiguration(), columnFamilies);
         final RocksdbReadOnlyDb<ColumnFamilyType> oldRocksdbDb =
            factory.createReadOnlyDb(fromPath.toString(), columnFamilies)) {
      final RocksdbRepositoryTransaction currentTransaction =
          beginRepositoryTransaction(newRocksdbDb);
      currentTransaction.run(
          () ->
              copyColumnFamilies(
                  oldRocksdbDb,
                  newRocksdbDb,
                  currentTransaction.getTransaction(),
                  familyTypes,
                  type));
      currentTransaction.commit();
    } catch (final Exception e) {
      LOG.error("Failed to create copy from {} to {}", fromPath, toPath, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * 将源路径数据库中指定列族的数据合并到当前数据库
   *
   * @param fromPath 源数据库路径
   * @param rocksdb 目标数据库
   * @param familyTypes 要合并的列族
   * @param type 0-基于列族，1-基于虚拟列族
   */
  public static <ColumnFamilyType extends ColumnFamilies> void merge(
      final Path fromPath,
      final RocksdbTransactionDb<ColumnFamilyType> rocksdb,
      final Set<ColumnFamilyType> familyTypes,
      final ColumnCopyType type) {
    final DefaultRocksdbFactory<ColumnFamilyType> factory = new DefaultRocksdbFactory<>();
    final ColumnFamilyType columnFamilies = familyTypes.iterator().next();
    try (final RocksdbReadOnlyDb<ColumnFamilyType> oldRocksdbDb =
        factory.createReadOnlyDb(fromPath.toString(), columnFamilies)) {
      final RocksdbRepositoryTransaction currentTransaction = beginRepositoryTransaction(rocksdb);
      currentTransaction.run(
          () ->
              copyColumnFamilies(
                  oldRocksdbDb, rocksdb, currentTransaction.getTransaction(), familyTypes, type));
      currentTransaction.commit();
    } catch (final Exception e) {
      LOG.error("Failed to merge from {} ", fromPath, e);
      throw new RuntimeException(e);
    }
  }

  /** 在目标库上开启一个仓储级事务 */
  private static RocksdbRepositoryTransaction beginRepositoryTransaction(
      final RocksdbTransactionDb<?> rocksdb) {
    final RocksdbTransactionContext transactionContext =
        (RocksdbTransactionContext) rocksdb.createTransactionContext();
    return (RocksdbRepositoryTransaction) transactionContext.getCurrentTransaction();
  }

  /** 将源库中指定列族的数据按复制类型写入目标库事务：FAMILY 整列族复制，VIRTUAL 仅复制匹配虚拟列族 前缀的记录 */
  private static <ColumnFamilyType extends ColumnFamilies> void copyColumnFamilies(
      final RocksdbReadOnlyDb<ColumnFamilyType> sourceDb,
      final RocksdbTransactionDb<ColumnFamilyType> targetDb,
      final Transaction transaction,
      final Set<ColumnFamilyType> familyTypes,
      final ColumnCopyType type)
      throws RocksDBException {
    if (type == ColumnCopyType.FAMILY) {
      final Set<Integer> copiedFamilies = new HashSet<>();
      for (final ColumnFamilyType columnFamilyType : familyTypes) {
        if (!copiedFamilies.add(columnFamilyType.entityFamily())) {
          continue;
        }
        requireTransferable(columnFamilyType);
        copyWholeFamily(
            sourceDb, columnFamilyType, targetDb.columnFamilyHandle(columnFamilyType), transaction);
      }
    } else {
      for (final ColumnFamilyType columnFamilyType : familyTypes) {
        requireTransferable(columnFamilyType);
        copyFamilyByVirtualPrefix(
            sourceDb, columnFamilyType, targetDb.columnFamilyHandle(columnFamilyType), transaction);
      }
    }
  }

  /** 不可迁移的列族禁止复制与合并 */
  private static void requireTransferable(final ColumnFamilies columnFamilyType) {
    if (!columnFamilyType.enableTransfer()) {
      throw new IllegalArgumentException("Current Column Family Not Transfer");
    }
  }

  /** 复制列族的全部数据 */
  private static <ColumnFamilyType extends ColumnFamilies> void copyWholeFamily(
      final RocksdbReadOnlyDb<ColumnFamilyType> sourceDb,
      final ColumnFamilyType columnFamilyType,
      final ColumnFamilyHandle targetHandle,
      final Transaction transaction)
      throws RocksDBException {
    try (final RocksIterator oldRocksIterator =
        sourceDb.getRocksdb().newIterator(sourceDb.columnFamilyHandle(columnFamilyType))) {
      for (oldRocksIterator.seekToFirst(); oldRocksIterator.isValid(); oldRocksIterator.next()) {
        transaction.put(targetHandle, oldRocksIterator.key(), oldRocksIterator.value());
      }
    }
  }

  /** 仅复制列族中匹配虚拟列族前缀的数据 */
  private static <ColumnFamilyType extends ColumnFamilies> void copyFamilyByVirtualPrefix(
      final RocksdbReadOnlyDb<ColumnFamilyType> sourceDb,
      final ColumnFamilyType columnFamilyType,
      final ColumnFamilyHandle targetHandle,
      final Transaction transaction)
      throws RocksDBException {
    try (final RocksIterator oldRocksIterator =
        sourceDb.getRocksdb().newIterator(sourceDb.columnFamilyHandle(columnFamilyType))) {
      for (oldRocksIterator.seek(keyToBuffer(columnFamilyType));
          oldRocksIterator.isValid();
          oldRocksIterator.next()) {
        final byte[] key = oldRocksIterator.key();
        if (isCurrentColumn(key, columnFamilyType)) {
          transaction.put(targetHandle, key, oldRocksIterator.value());
        }
      }
    }
  }

  /** 将列族的虚拟列族 id 编码为 4 字节前缀字节数组 */
  static <ColumnFamilyType extends ColumnFamilies> byte[] keyToBuffer(
      final ColumnFamilyType columnFamilyType) {
    final var bytes = ByteBuffer.allocate(Integer.BYTES);
    final var buffer = new UnsafeBuffer(bytes);
    buffer.putInt(0, columnFamilyType.virtualFamily(), ByteOrder.BIG_ENDIAN);
    return bytes.array();
  }

  /** 判断 key 的虚拟列族前缀是否与给定列族一致 */
  private static <ColumnFamilyType extends ColumnFamilies> boolean isCurrentColumn(
      final byte[] bytes, final ColumnFamilyType columnFamilyType) {
    final DirectBuffer keyViewBuffer = new UnsafeBuffer(0, 0);
    keyViewBuffer.wrap(bytes, 0, Integer.BYTES);
    final int column = keyViewBuffer.getInt(0, ByteOrder.BIG_ENDIAN);
    return column == columnFamilyType.virtualFamily();
  }

  /**
   * 构建数据库的列族描述符；数据库中不存在列族时返回默认列族的描述符
   *
   * @param rocksdbOptions RocksDB 配置选项
   * @param dbPath 数据库路径
   * @return 列族描述符列表
   */
  public static List<ColumnFamilyDescriptor> columnFamilyDescriptors(
      final RocksdbOptions rocksdbOptions, final String dbPath) {
    // 配置列族选项
    final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
    // 获取已存在的列族
    List<byte[]> existingColumnFamilies;
    try {
      final Options options =
          new Options(
              rocksdbOptions.dbOptions(),
              rocksdbOptions
                  .cfOptions()
                  .get(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.getFamily()));
      try {
        existingColumnFamilies = RocksDB.listColumnFamilies(options, dbPath);
      } finally {
        options.close();
      }
    } catch (final RocksDBException e) {
      existingColumnFamilies = new ArrayList<>();
    }
    if (existingColumnFamilies.isEmpty()) {
      columnFamilyDescriptors.add(
          new ColumnFamilyDescriptor(
              PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.getColumnFamilyName(),
              rocksdbOptions
                  .cfOptions()
                  .get(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.getFamily())));
    } else {
      for (final byte[] columnFamilyName : existingColumnFamilies) {
        final PredefinedColumnFamily predefinedColumnFamily =
            fromColumnFamilyName(columnFamilyName);
        // 磁盘上可能存在上层未声明使用的遗留列族，打开数据库必须提供全部已有列族的描述符，
        // 缺省沿用 default 列族选项
        final ColumnFamilyOptions options =
            rocksdbOptions
                .cfOptions()
                .getOrDefault(
                    predefinedColumnFamily.getFamily(),
                    rocksdbOptions
                        .cfOptions()
                        .get(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.getFamily()));
        columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName, options));
      }
    }
    return columnFamilyDescriptors;
  }

  /**
   * 将打开数据库返回的列族 handle 按列族归类存入 handles 数组
   *
   * @param columnFamilyHandles 存放结果的 handle 数组
   * @param handles 打开数据库时返回的 handle 列表
   */
  public static void handleInitializeResult(
      final ColumnFamilyHandle[] columnFamilyHandles, final List<ColumnFamilyHandle> handles) {
    try {
      for (final ColumnFamilyHandle columnFamilyHandle : handles) {
        final byte[] name = columnFamilyHandle.getName();
        final PredefinedColumnFamily businessColumnFamily = fromColumnFamilyName(name);
        columnFamilyHandles[businessColumnFamily.getFamily()] = columnFamilyHandle;
      }
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
  }
}
