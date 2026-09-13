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
package com.anyilanxin.kunpeng.kvstore;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 存储无关的 KV 数据库顶层接口，提供列族创建、事务管理、快照与数据复制/合并等能力，具体实现由后端模块提供
 *
 * @author zxuanhong
 */
public interface KvStore<ColumnFamilyType extends ColumnFamilies> extends AutoCloseable {

  /** 用于异步复制/合并操作的虚拟线程执行器 */
  ExecutorService IO_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * 创建指定列族的访问实例，用于在该列族中存取 key-value 对。key 与 value 实例用于保证类型安全。
   *
   * <p>列族实例创建后，该列族中只能存储所定义的 key 与 value 类型。
   *
   * @param columnFamilies 列族
   * @param transactionContext 事务上下文
   * @param keyInstance 定义该列族 key 类型的实例
   * @param valueInstance 定义该列族 value 类型的实例
   * @return 创建的列族访问实例
   */
  <
          KeyType extends com.anyilanxin.kunpeng.kvstore.types.KeyType,
          ValueType extends com.anyilanxin.kunpeng.kvstore.types.ValueType>
      ColumnFamily<KeyType, ValueType> createColumnFamily(
          final ColumnFamilyType columnFamilies,
          TransactionContext transactionContext,
          KeyType keyInstance,
          ValueType valueInstance);

  /** 创建事务上下文。 */
  TransactionContext createTransactionContext();

  /**
   * 将当前数据库的快照创建到给定目录中。
   *
   * @param snapshotDir 快照存储目录
   */
  void createSnapshot(final File snapshotDir);

  /**
   * 将源路径数据库中指定列族的数据复制到目标路径。
   *
   * @param fromPath 源数据库路径
   * @param toPath 目标数据库路径
   * @param familyTypes 需要复制的列族集合
   * @param type 复制方式
   */
  void createCopy(
      Path fromPath, Path toPath, Set<ColumnFamilyType> familyTypes, ColumnCopyType type);

  /** 异步执行 {@link #createCopy}，由内部虚拟线程执行器运行。 */
  default CompletableFuture<Void> createCopyAsync(
      final Path fromPath,
      final Path toPath,
      final Set<ColumnFamilyType> familyTypes,
      final ColumnCopyType type) {
    return CompletableFuture.runAsync(
        () -> createCopy(fromPath, toPath, familyTypes, type), IO_EXECUTOR);
  }

  /**
   * 将源路径数据库中指定列族的数据合并到当前数据库。
   *
   * @param fromPath 源数据库路径
   * @param familyTypes 需要合并的列族集合
   * @param type 合并方式
   */
  void merge(Path fromPath, Set<ColumnFamilyType> familyTypes, ColumnCopyType type);

  /** 异步执行 {@link #merge}，由内部虚拟线程执行器运行。 */
  default CompletableFuture<Void> mergeAsync(
      final Path fromPath, final Set<ColumnFamilyType> familyTypes, final ColumnCopyType type) {
    return CompletableFuture.runAsync(() -> merge(fromPath, familyTypes, type), IO_EXECUTOR);
  }

  /** 获取快照相关文件信息的校验和映射。 */
  Map<String, SnapshotFileInfo> getChecksums();
}
