/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.BootstrapSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.MergeSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.scheduler.Either;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件快照存储：按类型分目录存储三类快照，目录名 {@code <index>-<term>-<hex(nodeId)>}。
 *
 * <p>常规快照存于 {@code snapshot/}（按 {@code maxSnapshotCount} 保留），引导/合并快照分别存于
 * {@code bootstrap/}、{@code merge/}（各只保留最新一个）。快照在临时目录中构建后原子 move 到
 * 目标位置；根目录下的旧布局目录按常规快照加载。
 */
public final class FileSnapshotStore implements ReceivableSnapshotStore {

  static final String TEMP_DIRECTORY_PREFIX = "tmp-";
  /** 快照完整性标记文件后缀（与快照目录同名、SFV 风格逐文件 CRC32）。 */
  static final String VERIFICATION_SUFFIX = ".sfc";
  /** 常规快照子目录名。 */
  static final String REGULAR_DIRECTORY = "snapshot";
  /** 引导快照子目录名。 */
  static final String BOOTSTRAP_DIRECTORY = "bootstrap";
  /** 合并快照子目录名。 */
  static final String MERGE_DIRECTORY = "merge";
  private static final Logger LOGGER = LoggerFactory.getLogger(FileSnapshotStore.class);

  private final Path root;
  private final int maxSnapshotCount;
  // 本节点 id：拍摄快照的标识三元组之一
  private final String nodeId;
  private final List<PersistedSnapshotListener> listeners = new CopyOnWriteArrayList<>();
  // 进行中的 pending 快照（拍摄/接收），供 abortPendingSnapshots 统一清理
  private final List<FilePersistableSnapshot> pendingSnapshots = new CopyOnWriteArrayList<>();
  private final Map<String, FilePersistedSnapshot> snapshots = new ConcurrentHashMap<>();
  private final AtomicLong temporaryDirectoryIds = new AtomicLong();

  public FileSnapshotStore(final Path root, final int maxSnapshotCount, final String nodeId) {
    this.root = root;
    this.maxSnapshotCount = maxSnapshotCount;
    this.nodeId = nodeId;
    try {
      Files.createDirectories(root);
      for (final SnapshotType type : SnapshotType.values()) {
        Files.createDirectories(typedRoot(type));
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to create snapshot directory " + root, e);
    }
    purgeTemporaryDirectories();
    loadSnapshots();
  }

  /** 类型对应的存储子目录。 */
  private Path typedRoot(final SnapshotType type) {
    return root.resolve(directoryNameOf(type));
  }

  private static String directoryNameOf(final SnapshotType type) {
    return switch (type) {
      case REGULAR -> REGULAR_DIRECTORY;
      case BOOTSTRAP -> BOOTSTRAP_DIRECTORY;
      case MERGE -> MERGE_DIRECTORY;
    };
  }

  private static String keyOf(final SnapshotType type, final String snapshotId) {
    return directoryNameOf(type) + "/" + snapshotId;
  }

  private void purgeTemporaryDirectories() {
    try (final var directories = Files.list(root)) {
      directories
          .filter(Files::isDirectory)
          .filter(dir -> dir.getFileName().toString().startsWith(TEMP_DIRECTORY_PREFIX))
          .forEach(FilePersistedSnapshot::deleteRecursively);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void loadSnapshots() {
    // 新布局：三个类型子目录；旧布局：根目录下直接的快照目录按常规快照加载
    for (final SnapshotType type : SnapshotType.values()) {
      loadFrom(typedRoot(type));
    }
    loadLegacyRootSnapshots();
  }

  private void loadFrom(final Path directory) {
    try (final var entries = Files.list(directory)) {
      entries.filter(Files::isDirectory).forEach(dir -> register(dir, true));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void loadLegacyRootSnapshots() {
    try (final var directories = Files.list(root)) {
      directories
          .filter(Files::isDirectory)
          .filter(
              dir -> {
                final String name = dir.getFileName().toString();
                return !name.startsWith(TEMP_DIRECTORY_PREFIX)
                    && !name.equals(REGULAR_DIRECTORY)
                    && !name.equals(BOOTSTRAP_DIRECTORY)
                    && !name.equals(MERGE_DIRECTORY);
              })
          .forEach(dir -> register(dir, false));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void register(final Path directory, final boolean requireVerificationMarker) {
    // 新布局目录要求 .sfc 完整性标记：原子 move 之后才生成，缺失即视为未完整提交的残留
    if (requireVerificationMarker && !Files.exists(markerOf(directory))) {
      LOGGER.warn(
          "Snapshot directory {} has no verification marker, deleting it as incomplete", directory);
      FilePersistedSnapshot.deleteRecursively(directory);
      return;
    }
    // 加载时做内容完整性校验：manifest 缺失/不可解析或逐文件 size/CRC32 不一致均视为损坏
    final FilePersistedSnapshot snapshot;
    try {
      snapshot = FilePersistedSnapshot.load(directory);
    } catch (final Exception e) {
      LOGGER.warn("Snapshot directory {} cannot be loaded, deleting it", directory, e);
      FilePersistedSnapshot.deleteRecursively(directory);
      return;
    }
    if (snapshot.isCorrupt() || !snapshot.verifyContent()) {
      LOGGER.warn("Snapshot directory {} failed content verification, deleting it", directory);
      FilePersistedSnapshot.deleteRecursively(directory);
      return;
    }
    snapshots.put(keyOf(snapshot.getType(), snapshot.getId()), snapshot);
  }

  /** 与快照目录同名、后缀 .sfc 的完整性标记文件（位于同一父目录）。 */
  private static Path markerOf(final Path snapshotDirectory) {
    return snapshotDirectory
        .getParent()
        .resolve(snapshotDirectory.getFileName().toString() + VERIFICATION_SUFFIX);
  }

  /** 最新常规快照（仅 REGULAR 类型）。 */
  @Override
  public Optional<RaftSnapshot> getLatestSnapshot() {
    return latestOf(SnapshotType.REGULAR).map(RaftSnapshot.class::cast);
  }

  @Override
  public Optional<RaftSnapshot> getSnapshotAt(final long index) {
    return snapshots.values().stream()
        .filter(snapshot -> snapshot.getType() == SnapshotType.REGULAR)
        .filter(snapshot -> snapshot.getIndex() == index)
        .findFirst()
        .map(RaftSnapshot.class::cast);
  }

  /** 压缩下界：仅统计常规快照（引导/合并不参与日志压缩）。 */
  @Override
  public CompletableFuture<Long> getCompactionBound() {
    final long bound =
        snapshots.values().stream()
            .filter(snapshot -> !snapshot.isCorrupt())
            .filter(snapshot -> snapshot.getType() == SnapshotType.REGULAR)
            .mapToLong(PersistedSnapshot::getIndex)
            .min()
            .orElse(0L);
    return CompletableFuture.completedFuture(bound);
  }

  @Override
  public Optional<BootstrapSnapshot> getBootstrapSnapshot() {
    return latestOf(SnapshotType.BOOTSTRAP).map(BootstrapSnapshot.class::cast);
  }

  @Override
  public Optional<MergeSnapshot> getMergeSnapshot() {
    return latestOf(SnapshotType.MERGE).map(MergeSnapshot.class::cast);
  }

  /** 给定类型的最新未损坏快照。 */
  private Optional<FilePersistedSnapshot> latestOf(final SnapshotType type) {
    return snapshots.values().stream()
        .filter(snapshot -> !snapshot.isCorrupt() && snapshot.getType() == type)
        .max(Comparator.comparingLong(PersistedSnapshot::getIndex));
  }

  @Override
  public int getMaxSnapshotCount() {
    return maxSnapshotCount;
  }

  @Override
  public CompletableFuture<Integer> deleteSnapshotsFrom(final long index) {
    final List<FilePersistedSnapshot> toDelete =
        snapshots.values().stream()
            .filter(snapshot -> snapshot.getIndex() >= index)
            .filter(
                snapshot -> {
                  if (snapshot.isReserved()) {
                    LOGGER.debug("Skipping reserved snapshot {}", snapshot.getId());
                    return false;
                  }
                  return true;
                })
            .toList();
    toDelete.forEach(
        snapshot -> {
          snapshots.remove(keyOf(snapshot.getType(), snapshot.getId()));
          snapshot.delete();
          deleteMarkerOf(snapshot.getPath());
        });
    return CompletableFuture.completedFuture(toDelete.size());
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    final var future = new CompletableActorFuture<Void>();
    CompletableFuture.runAsync(
            () -> {
              pendingSnapshots.forEach(pending -> pending.abort().toCompletableFuture().join());
              pendingSnapshots.clear();
              purgeTemporaryDirectories();
            })
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                future.completeExceptionally(error.getCause() != null ? error.getCause() : error);
              } else {
                future.complete(null);
              }
            });
    return future;
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newTransientSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newPending(index, term, SnapshotType.REGULAR, businessInfo);
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newBootstrapSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newPending(index, term, SnapshotType.BOOTSTRAP, businessInfo);
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newMergeSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newPending(index, term, SnapshotType.MERGE, businessInfo);
  }

  /** 创建指定类型的 pending 快照：同 id 允许重拍，提交时按同位点替换。 */
  private Either<SnapshotException, PersistableSnapshot> newPending(
      final long index, final long term, final SnapshotType type,
      final Map<String, Object> businessInfo) {
    final SnapshotMetadata metadata =
        SnapshotMetadata.ofBusinessInfo(index, term, nodeId, type, businessInfo);
    final var pending =
        new FilePersistableSnapshot(this, metadata, newTemporaryDirectory(), false);
    pendingSnapshots.add(pending);
    return Either.right(pending);
  }

  @Override
  public CompletableFuture<PersistableSnapshot> newReceivedSnapshot(final String snapshotId) {
    final SnapshotMetadata metadata;
    try {
      final SnapshotId id = SnapshotId.fromString(snapshotId);
      metadata = SnapshotMetadata.of(id.index(), id.term(), id.nodeId(), SnapshotType.REGULAR);
    } catch (final IllegalArgumentException e) {
      return CompletableFuture.failedFuture(e);
    }

    // Reject if an identical or newer snapshot already exists locally.
    if (snapshots.values().stream()
        .anyMatch(snapshot -> snapshot.getIndex() >= metadata.index())) {
      return CompletableFuture.failedFuture(
          new SnapshotAlreadyExistsException(
              "Cannot receive snapshot " + snapshotId + "; an identical or newer snapshot exists"));
    }

    final var pending =
        new FilePersistableSnapshot(this, metadata, newTemporaryDirectory(), true);
    pendingSnapshots.add(pending);
    return CompletableFuture.completedFuture(pending);
  }

  /** 从最新常规快照复制产生引导快照：回调负责把源目录内容复制到 pending 目录。 */
  @Override
  public ActorFuture<PersistedSnapshot> copyForBootstrap(
      final BiConsumer<Path, Path> copySnapshot) {
    final var future = new CompletableActorFuture<PersistedSnapshot>();
    CompletableFuture.supplyAsync(
            () -> {
              final var source = getLatestSnapshot();
              if (source.isEmpty()) {
                throw new SnapshotException(
                    "No regular snapshot to copy for bootstrap on " + nodeId);
              }
              final SnapshotMetadata sourceMetadata = source.get().getMetadata();
              final SnapshotMetadata bootstrapMetadata =
                  SnapshotMetadata.ofBusinessInfo(
                      sourceMetadata.index(),
                      sourceMetadata.term(),
                      sourceMetadata.nodeId(),
                      SnapshotType.BOOTSTRAP,
                      Map.of());
              final Path pendingDirectory = newTemporaryDirectory();
              final var pending =
                  new FilePersistableSnapshot(this, bootstrapMetadata, pendingDirectory, false);
              pendingSnapshots.add(pending);
              try {
                Files.createDirectories(pendingDirectory);
                copySnapshot.accept(source.get().getPath(), pendingDirectory);
              } catch (final Exception e) {
                pending.abort().toCompletableFuture().join();
                throw new SnapshotException(
                    "Failed to copy snapshot " + sourceMetadata.getSnapshotIdAsString()
                        + " for bootstrap", e);
              }
              return pending.persist().toCompletableFuture().join();
            })
        .whenComplete(
            (result, error) -> {
              if (error != null) {
                future.completeExceptionally(
                    error.getCause() != null ? error.getCause() : error);
              } else {
                future.complete(result);
              }
            });
    return future;
  }

  /** 提交完成/放弃时把 pending 移出跟踪列表。 */
  void removePending(final FilePersistableSnapshot pending) {
    pendingSnapshots.remove(pending);
  }

  /** Commits a fully written snapshot from its temporary directory into the store. */
  PersistedSnapshot commit(final SnapshotManifest manifest, final Path temporaryDirectory) {
    final SnapshotMetadata metadata = manifest.metadata;
    final String snapshotId = metadata.getSnapshotIdAsString();

    // 幂等重拍：仅当存在同类型 index 严格更大的快照时拒绝；同 id 视为替换提交
    if (snapshots.values().stream()
        .anyMatch(
            snapshot ->
                snapshot.getType() == manifest.type() && snapshot.getIndex() > metadata.index())) {
      FilePersistedSnapshot.deleteRecursively(temporaryDirectory);
      throw new SnapshotAlreadyExistsException(
          "Cannot commit snapshot " + snapshotId + "; a newer snapshot exists");
    }

    final Path destination = typedRoot(manifest.type()).resolve(snapshotId);
    if (Files.exists(destination)) {
      FilePersistedSnapshot.deleteRecursively(destination);
      deleteMarkerOf(destination);
    }

    try {
      Files.move(temporaryDirectory, destination, StandardCopyOption.ATOMIC_MOVE);
    } catch (final AtomicMoveNotSupportedException e) {
      throw new SnapshotException("Atomic move is not supported by the file system", e);
    } catch (final IOException e) {
      FilePersistedSnapshot.deleteRecursively(temporaryDirectory);
      throw new UncheckedIOException("Failed to move snapshot " + snapshotId + " into place", e);
    }
    // 提交后对目标类型子目录做目录 fsync，保证 rename 元数据落盘
    try (final var dirChannel = FileChannel.open(typedRoot(manifest.type()))) {
      dirChannel.force(true);
    } catch (final IOException e) {
      LOGGER.debug("Failed to fsync snapshot directory for {}", snapshotId, e);
    }
    // 原子 move 完成后才生成 .sfc 完整性标记：启动时缺失标记的目录视为未完整提交并清除
    writeVerificationFile(destination, manifest);

    final FilePersistedSnapshot persistedSnapshot = FilePersistedSnapshot.of(destination, manifest);
    snapshots.put(keyOf(manifest.type(), snapshotId), persistedSnapshot);
    enforceRetention(manifest.type());
    LOGGER.debug("Committed snapshot {} at {}", snapshotId, destination);
    notifyListeners(persistedSnapshot);
    return persistedSnapshot;
  }

  /**
   * 保留策略：常规按 maxSnapshotCount 保留；引导/合并只保留最新一个（新快照提交即删旧）。
   */
  private void enforceRetention(final SnapshotType committedType) {
    final List<FilePersistedSnapshot> retained =
        snapshots.values().stream()
            .filter(snapshot -> snapshot.getType() == committedType)
            .filter(
                snapshot -> {
                  if (snapshot.isReserved()) {
                    LOGGER.debug(
                        "Skipping reserved snapshot {} during retention enforcement",
                        snapshot.getId());
                    return false;
                  }
                  return true;
                })
            .sorted(
                Comparator.comparingLong(PersistedSnapshot::getIndex)
                    .reversed()
                    .thenComparing(PersistedSnapshot::getTerm, Comparator.reverseOrder()))
            .toList();

    final int keepCount =
        committedType == SnapshotType.REGULAR ? Math.max(1, maxSnapshotCount) : 1;
    if (retained.size() <= keepCount) {
      return;
    }
    retained.subList(keepCount, retained.size())
        .forEach(
            snapshot -> {
              snapshots.remove(keyOf(snapshot.getType(), snapshot.getId()));
              snapshot.delete();
              deleteMarkerOf(snapshot.getPath());
              LOGGER.debug(
                  "Deleted snapshot {} exceeding the retention policy of type {}",
                  snapshot.getId(),
                  snapshot.getType());
            });
  }

  private void notifyListeners(final PersistedSnapshot snapshot) {
    listeners.forEach(listener -> listener.onNewPersistedSnapshot(snapshot));
  }

  /** 生成 SFV 风格的 .sfc 标记（逐文件 CRC32），在原子 move 完成后写入并强制落盘。 */
  private void writeVerificationFile(
      final Path snapshotDirectory, final SnapshotManifest manifest) {
    final StringBuilder sfv = new StringBuilder();
    sfv.append("; kunpeng snapshot verification\n");
    sfv.append("; snapshot: ").append(manifest.metadata.getSnapshotIdAsString()).append('\n');
    sfv.append("; type: ").append(manifest.type().name()).append('\n');
    try (final var files = Files.list(snapshotDirectory)) {
      files.map(Path::getFileName)
          .map(Path::toString)
          .sorted()
          .forEach(
              name ->
                  sfv
                      .append(name)
                      .append(' ')
                      .append(hexCrcOf(snapshotDirectory.resolve(name)))
                      .append('\n'));
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to list snapshot files for verification marker", e);
    }
    final Path marker = markerOf(snapshotDirectory);
    try {
      Files.write(marker, sfv.toString().getBytes(StandardCharsets.UTF_8));
      try (final var channel = FileChannel.open(marker, StandardOpenOption.WRITE)) {
        channel.force(true);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write verification marker " + marker, e);
    }
  }

  /** 计算文件内容的 CRC32，返回 8 位小写十六进制。 */
  private static String hexCrcOf(final Path file) {
    final var crc = new CRC32();
    try (final var input = Files.newInputStream(file)) {
      final var buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        crc.update(buffer, 0, read);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return String.format("%08x", crc.getValue());
  }

  /** 删除快照目录对应的 .sfc 标记。 */
  private static void deleteMarkerOf(final Path snapshotDirectory) {
    try {
      Files.deleteIfExists(markerOf(snapshotDirectory));
    } catch (final IOException e) {
      LOGGER.debug("Failed to delete verification marker of {}", snapshotDirectory, e);
    }
  }

  private Path newTemporaryDirectory() {
    return root.resolve(
        TEMP_DIRECTORY_PREFIX
            + System.nanoTime()
            + "-"
            + temporaryDirectoryIds.incrementAndGet());
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.remove(listener);
  }

  /** 关闭存储：清空内存状态，文件存储无需额外资源释放。 */
  @Override
  public ActorFuture<Void> close() {
    snapshots.clear();
    pendingSnapshots.clear();
    return CompletableActorFuture.completed();
  }
}
