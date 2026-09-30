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
package com.anyilanxin.kunpeng.broker.business.raft;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.exception.KvStoreException;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import com.anyilanxin.kunpeng.rocksdb.RocksdbFactory;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.slf4j.Logger;

/**
 * 业务分区的 Raft 快照提供者，负责业务分区（business-repository RocksDB）快照内容的构建、写出与恢复。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessRaftSnapshotProvider
    implements RaftSnapshotProvider<KvStore<BusinessRepositoryColumnFamilies>>,
        TransferSnapshotProvider {
  private final ConcurrencyControl concurrencyControl;
  private KvStore<BusinessRepositoryColumnFamilies> rocksdbDb;
  private final RocksdbFactory<BusinessRepositoryColumnFamilies> rocksdbFactory;
  private final RocksdbConfiguration rocksdbConfiguration;
  private static final Logger LOG = BrokerLoggers.CLUSTER_BUSINESS;
  private Path partitionDirectory;
  private Path runtimeDirectory;
  private SnapshotStore snapshotStore;
  private final MeterRegistry registry;
  private RaftPartition partition;

  /** 在途 recover（分区 transition 链打开业务库的 future）：合并镜像早于库打开到达时等待其完成。 */
  private volatile ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> pendingRecover;

  public BusinessRaftSnapshotProvider(
      final ConcurrencyControl concurrencyControl,
      final RocksdbConfiguration rocksdbConfiguration,
      final MeterRegistry registry) {
    this.concurrencyControl = concurrencyControl;
    rocksdbFactory = new DefaultRocksdbFactory<>();
    this.rocksdbConfiguration = rocksdbConfiguration;
    this.registry = registry;
  }

  @Override
  public Map<String, Object> takeSnapshot(final Path snapshotDirectory) {
    try {
      FileUtil.deleteTreeIfExists(snapshotDirectory);
    } catch (final Exception e) {
      LOG.debug("Failed to delete snapshot directory when closing", e);
    }
    System.out.println("---takeSnapshot-----" + partition.partitionMetadata().id());
    rocksdbDb.createSnapshot(snapshotDirectory.toFile());
    return Map.of("timestamp", System.currentTimeMillis());
  }

  @Override
  public void setPartitionDirectory(final Path partitionDirectory) {
    this.partitionDirectory = partitionDirectory;
  }

  @Override
  public void setRaftPartition(final RaftPartition partition) {
    this.partition = partition;
  }

  @Override
  public Path getPartitionDirectory() {
    return partitionDirectory;
  }

  @Override
  public void setRuntimeDirectory(final Path runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  @Override
  public Path getRuntimeDirectory() {
    return runtimeDirectory;
  }

  @Override
  public void setSnapshotStore(final SnapshotStore snapshotStore) {
    this.snapshotStore = snapshotStore;
  }

  @Override
  public SnapshotStore getSnapshotStore() {
    return snapshotStore;
  }

  @Override
  public ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> recover() {
    final ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> future =
        concurrencyControl.createFuture();
    pendingRecover = future;
    concurrencyControl.run(() -> recoverInternal(future));
    return future;
  }

  private void recoverInternal(
      final ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> future) {
    try {
      FileUtil.deleteTreeIfExists(runtimeDirectory);
    } catch (final IOException e) {
      future.completeExceptionally(
          new RuntimeException(
              "Failed to delete runtime folder. Cannot recover from snapshot.", e));
    }
    snapshotStore.getLatestSnapshot().ifPresent(snapshot -> recoverFromSnapshot(future, snapshot));
    openDb(future);
  }

  private void recoverFromSnapshot(
      final ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> future,
      final PersistedSnapshot snapshot) {
    LOG.debug("Recovering state from available snapshot: {}", snapshot.getMetadata().snapshotId());
    try (final var db =
        rocksdbFactory.createReadOnlyDb(
            snapshot.getPath().toString(), BusinessRepositoryColumnFamilies.DEFAULT)) {
      db.createSnapshot(runtimeDirectory.toFile());
    } catch (final Exception e) {
      future.completeExceptionally(
          new KvStoreException(
              String.format(
                  "Failed to recover from snapshot %s", snapshot.getMetadata().snapshotId()),
              e));
    }
  }

  private void openDb(final ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> future) {
    try {
      if (rocksdbDb == null) {
        rocksdbDb =
            rocksdbFactory.createDb(
                runtimeDirectory.toFile().getPath(),
                registry,
                1,
                rocksdbConfiguration,
                BusinessRepositoryColumnFamilies.DEFAULT);
        LOG.debug("Opened database from '{}'.", runtimeDirectory);
        future.complete(rocksdbDb);
      }
    } catch (final Exception error) {
      future.completeExceptionally(new RuntimeException("Failed to open database", error));
    }
  }

  private void closeDbInternal(final ActorFuture<Void> future) {
    try {
      if (rocksdbDb != null) {
        final var dbToClose = rocksdbDb;
        rocksdbDb = null;
        dbToClose.close();
        LOG.debug("Closed database from '{}'.", runtimeDirectory);
      }
      tryDeletingRuntimeDirectory();
      future.complete(null);
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
  }

  private void tryDeletingRuntimeDirectory() {
    try {
      FileUtil.deleteTreeIfExists(runtimeDirectory);
    } catch (final Exception e) {
      LOG.debug("Failed to delete runtime directory when closing", e);
    }
  }

  @Override
  public void close() {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(() -> closeDbInternal(future));
  }

  @Override
  public ActorFuture<Void> mergeSnapshot(final PersistedSnapshot received) {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(() -> mergeInternal(received, future));
    return future;
  }

  /**
   * 合并执行前置编排：推送到达可能早于分区 transition 链的 recover（mergePushServer 随 raft 角色变化即注册，业务库由 transition
   * 链异步打开）——库未开且 recover 在途时等待其完成后续跑合并； 从未发起 recover 则前置不满足，直接失败。
   */
  private void mergeInternal(final PersistedSnapshot received, final ActorFuture<Void> future) {
    if (rocksdbDb == null) {
      final ActorFuture<KvStore<BusinessRepositoryColumnFamilies>> pending = pendingRecover;
      if (pending == null) {
        future.completeExceptionally(
            new IllegalStateException(
                "Merge snapshot arrived before partition transition recovered the db"));
        return;
      }
      pending.onComplete(
          (db, error) -> {
            if (error != null) {
              future.completeExceptionally(error);
            } else {
              doMerge(received, future);
            }
          });
      return;
    }
    doMerge(received, future);
  }

  private void doMerge(final PersistedSnapshot received, final ActorFuture<Void> future) {
    System.out.println("----mergeSnapshot-------" + partition.partitionMetadata().id());
    final Path checksumPath = received.getPath();
    rocksdbDb.merge(
        checksumPath,
        Set.of(BusinessRepositoryColumnFamilies.PROCESS_POSITION),
        ColumnCopyType.FAMILY);
    partition
        .triggerFollowerSnapshotInstall()
        .whenComplete(
            new BiConsumer<Long, Throwable>() {
              @Override
              public void accept(Long aLong, Throwable throwable) {
                if (throwable != null) {
                  future.completeExceptionally(throwable);
                } else {
                  future.complete(null);
                }
              }
            });
  }

  @Override
  public Map<String, Object> takeBootstrapSnapshot(
      final Path snapshotDirectory, final Map<String, String> parameters) {
    try {
      FileUtil.deleteTreeIfExists(snapshotDirectory);
    } catch (final Exception e) {
      LOG.debug("Failed to delete snapshot directory when closing", e);
    }
    snapshotStore
        .getLatestSnapshot()
        .ifPresent(
            snapshot -> {
              rocksdbDb.createCopy(
                  snapshot.getPath(),
                  snapshotDirectory,
                  Set.of(BusinessRepositoryColumnFamilies.DEPLOYMENT),
                  ColumnCopyType.FAMILY);
            });

    return Map.of("timestamp", System.currentTimeMillis());
  }

  @Override
  public ActorFuture<Void> mergeSnapshotResult(
      final boolean result,
      final String message,
      final PersistedSnapshot persistedSnapshot,
      final Map<String, String> parameters) {
    // 业务合并结果处理：成功（水位已推进，镜像由传输层删除）/失败（镜像保留在 merge 存储，可据此回滚）
    return concurrencyControl.createCompletedFuture();
  }

  @Override
  public Map<String, Object> takeMergeSnapshot(
      final Path snapshotDirectory, final Map<String, String> parameters) {
    snapshotStore
        .getLatestSnapshot()
        .ifPresent(
            snapshot -> {
              rocksdbDb.createCopy(
                  snapshot.getPath(),
                  snapshotDirectory,
                  Set.of(BusinessRepositoryColumnFamilies.PROCESS_POSITION),
                  ColumnCopyType.FAMILY);
            });
    return Map.of();
  }
}
