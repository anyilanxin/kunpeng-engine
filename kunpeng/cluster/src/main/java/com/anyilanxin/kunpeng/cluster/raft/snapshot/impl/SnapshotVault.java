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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.ArchivedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChecksumProvider;
import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 快照库（唯一 Store 门面）：三副本区（主/bootstrap/merge）各自维护 active 落档；
 * 变异全部串行于内部单线程执行器；同步读任意线程。
 *
 * <p>构造期扫描规则：暂存前缀目录删除；无 manifest.bin 的目录视为部分写（含旧格式快照）删除；
 * 清单不符丢弃。
 */
public final class SnapshotVault implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SnapshotVault.class);

  private final Path snapshotsRoot;
  private final Path bootstrapRoot;
  private final Path mergeRoot;
  private final SnapshotChecksumProvider checksumProvider;

  private final AtomicReference<ArchivedSnapshot> active = new AtomicReference<>();
  private final AtomicReference<ArchivedSnapshot> activeBootstrap = new AtomicReference<>();
  private final AtomicReference<ArchivedSnapshot> activeMerge = new AtomicReference<>();
  // 串行线程独占
  private final Map<SnapshotRef, ArchivedSnapshot> known = new HashMap<>();
  private final Set<Object> inProgress = new HashSet<>();
  private final List<ArchivedSnapshotListener> listeners = new CopyOnWriteArrayList<>();

  private final ExecutorService serializer =
      Executors.newSingleThreadExecutor(
          runnable -> {
            final Thread thread = new Thread(runnable, "snapshot-vault");
            thread.setDaemon(true);
            return thread;
          });

  private final Counter committed;
  private final Counter purged;
  private final Counter corrupted;
  private final AtomicLong knownGauge = new AtomicLong();

  public SnapshotVault(
      final Path partitionRoot,
      final SnapshotChecksumProvider checksumProvider,
      final MeterRegistry registry) {
    snapshotsRoot = partitionRoot.resolve(SnapshotLayout.SNAPSHOTS_DIR);
    bootstrapRoot = partitionRoot.resolve(SnapshotLayout.BOOTSTRAP_DIR);
    mergeRoot = partitionRoot.resolve(SnapshotLayout.MERGE_DIR);
    this.checksumProvider = checksumProvider;
    if (registry != null) {
      Gauge.builder("snapshotVault.known", knownGauge, AtomicLong::doubleValue)
          .description("已落档快照数")
          .register(registry);
      committed =
          Counter.builder("snapshotVault.committed")
              .tags(Tags.of(Tag.of("result", "committed")))
              .register(registry);
      purged =
          Counter.builder("snapshotVault.committed")
              .tags(Tags.of(Tag.of("result", "purged")))
              .register(registry);
      corrupted = Counter.builder("snapshotVault.corrupted").register(registry);
    } else {
      committed = null;
      purged = null;
      corrupted = null;
    }
    try {
      VaultFiles.ensureDirectory(snapshotsRoot);
      VaultFiles.ensureDirectory(bootstrapRoot);
      VaultFiles.ensureDirectory(mergeRoot);
      // 构造完成即代表可读
      scanRegion(snapshotsRoot, Region.MAIN);
      scanRegion(bootstrapRoot, Region.BOOTSTRAP);
      scanRegion(mergeRoot, Region.MERGE);
    } catch (final IOException e) {
      throw new SnapshotStoreException.WriteFailure("快照目录初始化失败", e);
    }
  }

  // ===== 同步读（任意线程） =====

  public Optional<ArchivedSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(active.get());
  }

  public Optional<ArchivedSnapshot> getBootstrapSnapshot() {
    return Optional.ofNullable(activeBootstrap.get());
  }

  public Optional<ArchivedSnapshot> getMergeSnapshot() {
    return Optional.ofNullable(activeMerge.get());
  }

  public long getCompactionBound() {
    return known.keySet().stream().mapToLong(SnapshotRef::index).min().orElse(0);
  }

  public Collection<ArchivedSnapshot> getAvailableSnapshots() {
    return List.copyOf(known.values());
  }

  public long getCurrentSnapshotIndex() {
    final ArchivedSnapshot latest = active.get();
    return latest == null ? 0 : latest.ref().index();
  }

  public Path snapshotsRoot() {
    return snapshotsRoot;
  }

  public void addSnapshotListener(final ArchivedSnapshotListener listener) {
    listeners.add(listener);
  }

  public void removeSnapshotListener(final ArchivedSnapshotListener listener) {
    listeners.remove(listener);
  }

  // ===== 串行操作 =====

  /** 建立暂存快照（之后调用 {@link #capture} 完成拍摄） */
  public CompletableFuture<StagedSnapshot> stage(
      final long processedPosition,
      final long exportedPosition,
      final long index,
      final long term,
      final String brokerId,
      final boolean force) {
    return submit(
        () -> {
          if (processedPosition < 0 || exportedPosition < 0) {
            throw new SnapshotStoreException.WriteFailure(
                "快照位置不可为负: processed="
                    + processedPosition
                    + " exported="
                    + exportedPosition,
                null);
          }
          final var current = active.get();
          final var ref =
              new SnapshotRef(index, term, processedPosition, exportedPosition, brokerId);
          if (!force && current != null && current.ref().compareTo(ref) >= 0) {
            throw new SnapshotStoreException.AlreadyExists("已有更新的快照: " + current.ref());
          }
          final Path staging = snapshotsRoot.resolve(SnapshotLayout.STAGING_PREFIX + UUID.randomUUID());
          // 不预建目录: RocksDB Checkpoint 要求目标不存在, 由 taker 自建
          final var staged = new StagedSnapshot(this, ref, staging, force);
          inProgress.add(staged);
          return staged;
        });
  }

  /** 执行拍摄并提交（taker 写内容 → 清单/元数据/原子改名/commit） */
  public CompletableFuture<Void> capture(final StagedSnapshot staged, final Consumer<Path> taker) {
    return submitVoid(
        () -> {
          try {
            staged.take(taker);
            staged.persist();
          } catch (final SnapshotStoreException.AlreadyExists duplicate) {
            // 并发拍摄同内容快照(校验和相同→同名): 幂等成功, 读取方取既有落档即可
            LOG.info("同内容快照已由并发拍摄落档, 幂等跳过: {}", duplicate.getMessage());
            staged.abort();
          } catch (final Exception e) {
            staged.abort();
            throw e;
          } finally {
            inProgress.remove(staged);
          }
        });
  }

  /** 建立接收会话：目标目录若存在但无清单则删除重来；有清单则 AlreadyExists */
  public CompletableFuture<IncomingReplica> receive(final String snapshotIdName) {
    return submit(
        () -> {
          final SnapshotRef ref;
          try {
            ref = SnapshotRef.parse(snapshotIdName);
          } catch (final Exception e) {
            throw new IllegalArgumentException(snapshotIdName, e);
          }
          final Path target = snapshotsRoot.resolve(snapshotIdName);
          if (Files.exists(target.resolve(SnapshotLayout.MANIFEST_FILE))) {
            throw new SnapshotStoreException.AlreadyExists("快照已存在: " + snapshotIdName);
          }
          try {
            VaultFiles.deleteRecursively(target);
            VaultFiles.ensureDirectory(target);
          } catch (final IOException e) {
            throw new SnapshotStoreException.WriteFailure("接收目录准备失败: " + target, e);
          }
          final var replica = new IncomingReplica(this, ref, -1, target);
          inProgress.add(replica);
          return replica;
        });
  }

  /** 应用传输块到接收副本（串行线程上执行保证会话状态一致） */
  public CompletableFuture<Void> applyBlock(final IncomingReplica replica, final SnapshotBlock block) {
    return submitVoid(
        () -> {
          try {
            replica.apply(block);
          } catch (final IOException e) {
            throw new SnapshotStoreException.WriteFailure("块写入失败: " + block.blockName(), e);
          }
        });
  }

  /** 提交接收副本 */
  public CompletableFuture<Void> commitReplica(final IncomingReplica replica) {
    return submitVoid(
        () -> {
          try {
            replica.persist();
          } catch (final Exception e) {
            replica.abort();
            throw e;
          } finally {
            inProgress.remove(replica);
          }
        });
  }

  /** 中止全部未完成快照（角色切换清理） */
  public CompletableFuture<Void> abortPendingSnapshots() {
    return submitVoid(
        () -> {
          for (final Object pending : List.copyOf(inProgress)) {
            if (pending instanceof final StagedSnapshot staged) {
              staged.abort();
            } else if (pending instanceof final IncomingReplica replica) {
              replica.abort();
            }
          }
          inProgress.clear();
        });
  }

  /** 复制指定快照到 bootstrap 副本区（不及最新则先由调用方触发拍摄） */
  public CompletableFuture<Void> copyForBootstrap(final String snapshotIdName, final long position) {
    return copyToRegion(snapshotIdName, Region.BOOTSTRAP);
  }

  public CompletableFuture<Void> copyForMerge(final String snapshotIdName, final long position) {
    return copyToRegion(snapshotIdName, Region.MERGE);
  }

  public CompletableFuture<Void> deleteBootstrapSnapshots() {
    return deleteRegion(bootstrapRoot, activeBootstrap);
  }

  public CompletableFuture<Void> deleteMergeSnapshots() {
    return deleteRegion(mergeRoot, activeMerge);
  }

  /** 关闭串行线程（不再接受新任务） */
  @Override
  public void close() {
    serializer.shutdown();
  }

  private CompletableFuture<Void> copyToRegion(final String snapshotIdName, final Region region) {
    return submitVoid(
        () -> {
          final Path source = snapshotsRoot.resolve(snapshotIdName);
          if (!Files.exists(source.resolve(SnapshotLayout.MANIFEST_FILE))) {
            throw new SnapshotStoreException.NotFound("快照不存在: " + snapshotIdName);
          }
          final Path root = region == Region.BOOTSTRAP ? bootstrapRoot : mergeRoot;
          VaultFiles.deleteRecursively(root);
          VaultFiles.ensureDirectory(root);
          final Path target = root.resolve(snapshotIdName);
          // 复制而非移动：保留主快照区档位，副本区独立持有拷贝
          VaultFiles.copySnapshot(source, target);
          final var archived = ArchivedSnapshot.load(target);
          (region == Region.BOOTSTRAP ? activeBootstrap : activeMerge).set(archived);
        });
  }

  private CompletableFuture<Void> deleteRegion(
      final Path root, final AtomicReference<ArchivedSnapshot> activeRef) {
    return submitVoid(
        () -> {
          VaultFiles.deleteRecursively(root);
          VaultFiles.ensureDirectory(root);
          activeRef.set(null);
        });
  }

  // ===== 包内（串行线程或被串行作业调用） =====

  /**
   * 构建目录清单：优先外部提供，否则逐文件 CRC32C
   */
  ChecksumManifest buildManifest(final Path directory) {
    final ChecksumManifest manifest = ChecksumManifest.empty();
    Map<String, String> provided = null;
    if (checksumProvider != null) {
      provided = checksumProvider.getSnapshotChecksums(directory);
    }
    try {
      for (final String name : VaultFiles.listFilesSorted(directory)) {
        if (SnapshotLayout.MANIFEST_FILE.equals(name)) {
          continue;
        }
        final String checksum =
            provided != null && provided.containsKey(name)
                ? provided.get(name)
                    : Long.toHexString(VaultFiles.fileCrc(directory.resolve(name)));
        manifest.add(name, checksum);
      }
    } catch (final IOException e) {
      throw new SnapshotStoreException.WriteFailure("清单计算失败: " + directory, e);
    }
    return manifest;
  }

  /** 落档提交（串行线程）：过期则丢弃；否则 CAS active、登记、淘汰旧档、通知 */
  void commit(
      final SnapshotRef ref,
      final Path directory,
      final ChecksumManifest manifest,
      final SnapshotMeta meta,
      final boolean forced) {
    final ArchivedSnapshot current = active.get();
    if (current != null && current.ref().compareTo(ref) > 0 && !forced) {
      LOG.info("提交的快照过期, 丢弃: {} (当前 {})", ref, current.ref());
      try {
        VaultFiles.deleteRecursively(directory);
      } catch (final IOException e) {
        LOG.warn("过期快照删除失败: {}", directory, e);
      }
      return;
    }
    final var archived = new ArchivedSnapshot(ref, directory, manifest, meta);
    active.set(archived);
    known.put(ref, archived);
    knownGauge.set(known.size());
    if (committed != null) {
      committed.increment();
    }
    purgeSuperseded();
    listeners.forEach(listener -> listener.onArchived(archived));
  }

  private void purgeSuperseded() {
    final ArchivedSnapshot latest = active.get();
    if (latest == null) {
      return;
    }
    for (final var entry : List.copyOf(known.entrySet())) {
      final var candidate = entry.getValue();
      if (candidate == latest || candidate.leased()) {
        continue;
      }
      known.remove(entry.getKey());
      knownGauge.set(known.size());
      try {
        VaultFiles.deleteRecursively(candidate.directory());
        if (purged != null) {
          purged.increment();
        }
        listeners.forEach(listener -> listener.onPurged(candidate));
      } catch (final IOException e) {
        LOG.warn("旧快照淘汰失败: {}", candidate.directory(), e);
        known.put(entry.getKey(), candidate);
      }
    }
  }

  private void scanRegion(final Path root, final Region region) {
    if (!Files.isDirectory(root)) {
      return;
    }
    final List<Path> directories;
    try {
      directories = VaultFiles.sortedSubDirectories(root);
    } catch (final IOException e) {
      LOG.warn("快照区扫描失败: {}", root, e);
      return;
    }
    final List<ArchivedSnapshot> loaded = new ArrayList<>();
    for (final Path directory : directories) {
      final String name = directory.getFileName().toString();
      if (name.startsWith(SnapshotLayout.STAGING_PREFIX)) {
        deleteQuietly(directory, "暂存残留");
        continue;
      }
      if (!Files.exists(directory.resolve(SnapshotLayout.MANIFEST_FILE))) {
        deleteQuietly(directory, "部分写或旧格式快照(升级后首启将重拍)");
        continue;
      }
      try {
        final var archived = ArchivedSnapshot.load(directory);
        final var recomputed = buildManifest(directory);
        if (recomputed.combined() != archived.manifest().combined()) {
          if (corrupted != null) {
            corrupted.increment();
          }
          deleteQuietly(directory, "清单校验不符");
          continue;
        }
        loaded.add(archived);
      } catch (final Exception e) {
        LOG.warn("快照目录装载失败: {}", directory, e);
        deleteQuietly(directory, "装载异常");
      }
    }
    if (loaded.isEmpty()) {
      return;
    }
    loaded.sort((a, b) -> a.ref().compareTo(b.ref()));
    final ArchivedSnapshot latest = loaded.get(loaded.size() - 1);
    final AtomicReference<ArchivedSnapshot> activeRef =
        region == Region.MAIN
            ? active
            : region == Region.BOOTSTRAP ? activeBootstrap : activeMerge;
    activeRef.set(latest);
    if (region == Region.MAIN) {
      for (final var archived : loaded) {
        known.put(archived.ref(), archived);
      }
      knownGauge.set(known.size());
      LOG.info("快照区装载 {} 个, 最新 {}", loaded.size(), latest.ref());
    }
  }

  private void deleteQuietly(final Path directory, final String reason) {
    try {
      VaultFiles.deleteRecursively(directory);
      LOG.info("删除快照目录 {} ({})", directory.getFileName(), reason);
    } catch (final IOException e) {
      LOG.warn("快照目录删除失败: {}", directory, e);
    }
  }

  private <T> CompletableFuture<T> submit(final java.util.function.Supplier<T> action) {
    final var future = new CompletableFuture<T>();
    serializer.execute(
        () -> {
          try {
            future.complete(action.get());
          } catch (final Throwable error) {
            future.completeExceptionally(error);
          }
        });
    return future;
  }

  private CompletableFuture<Void> submitVoid(final VaultTask action) {
    final var future = new CompletableFuture<Void>();
    serializer.execute(
        () -> {
          try {
            action.run();
            future.complete(null);
          } catch (final Throwable error) {
            future.completeExceptionally(error);
          }
        });
    return future;
  }

  /** 可抛受检异常的串行任务 */
  @FunctionalInterface
  private interface VaultTask {

    void run() throws Exception;
  }

  private enum Region {
    MAIN,
    BOOTSTRAP,
    MERGE
  }
}
