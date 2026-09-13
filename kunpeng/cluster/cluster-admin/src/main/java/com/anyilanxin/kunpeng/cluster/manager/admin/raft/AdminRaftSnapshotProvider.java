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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft;

import com.anyilanxin.kunpeng.cluster.manager.ClusterAdminLoggers;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.exception.KvStoreException;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import com.anyilanxin.kunpeng.rocksdb.RocksdbFactory;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;

/**
 * 管理分区的 Raft 快照提供者，负责生成管理分组的快照内容，当前快照内容为空。
 *
 * @author zxuanhong
 * @since
 */
public class AdminRaftSnapshotProvider
    implements RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> {
  private final ConcurrencyControl concurrencyControl;
  private KvStore<AdminRepositoryColumnFamilies> rocksdbDb;
  private final RocksdbFactory<AdminRepositoryColumnFamilies> rocksdbFactory;
  private final RocksdbConfiguration rocksdbConfiguration;
  private static final Logger LOG = ClusterAdminLoggers.CLUSTER_ADMIN;
  private Path partitionDirectory;
  private Path runtimeDirectory;
  private SnapshotStore snapshotStore;
  private final MeterRegistry registry;

  public AdminRaftSnapshotProvider(
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
    rocksdbDb.createSnapshot(snapshotDirectory.toFile());
    return Map.of("teset", System.currentTimeMillis());
  }

  @Override
  public void setPartitionDirectory(final Path partitionDirectory) {
    this.partitionDirectory = partitionDirectory;
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
  public ActorFuture<KvStore<AdminRepositoryColumnFamilies>> recover() {
    final ActorFuture<KvStore<AdminRepositoryColumnFamilies>> future =
        concurrencyControl.createFuture();
    concurrencyControl.run(() -> recoverInternal(future));
    return future;
  }

  private void recoverInternal(final ActorFuture<KvStore<AdminRepositoryColumnFamilies>> future) {
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
      final ActorFuture<KvStore<AdminRepositoryColumnFamilies>> future,
      final PersistedSnapshot snapshot) {
    LOG.debug("Recovering state from available snapshot: {}", snapshot.getMetadata().snapshotId());
    try (final var db =
        rocksdbFactory.createReadOnlyDb(
            snapshot.getPath().toString(), AdminRepositoryColumnFamilies.DEFAULT)) {
      db.createSnapshot(runtimeDirectory.toFile());
    } catch (final Exception e) {
      future.completeExceptionally(
          new KvStoreException(
              String.format(
                  "Failed to recover from snapshot %s", snapshot.getMetadata().snapshotId()),
              e));
    }
  }

  private void openDb(final ActorFuture<KvStore<AdminRepositoryColumnFamilies>> future) {
    try {
      if (rocksdbDb == null) {
        rocksdbDb =
            rocksdbFactory.createDb(
                runtimeDirectory.toFile().getPath(),
                registry,
                1,
                rocksdbConfiguration,
                AdminRepositoryColumnFamilies.DEFAULT);
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
  public ActorFuture<Void> mergeSnapshot(final Path snapshotDirectory) {
    return null;
  }
}
