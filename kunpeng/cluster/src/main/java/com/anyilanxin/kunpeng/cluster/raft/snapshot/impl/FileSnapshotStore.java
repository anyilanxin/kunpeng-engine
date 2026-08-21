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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TransientSnapshot;
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
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File-based snapshot store. Snapshots are grouped by type into the subdirectories {@code
 * snapshot/}（常规）、{@code bootstrap/}（引导）、{@code merge/}（合并）under the store root; each
 * snapshot lives in its own directory named {@code <index>-<term>-<nodeId>}, containing a {@code
 * manifest} file plus arbitrary content files. Snapshots are created in temporary directories and
 * moved into place atomically. Directories left directly under the root from an older layout are
 * loaded as legacy regular snapshots.
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
  private final List<PersistedSnapshotListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<String, FilePersistedSnapshot> snapshots = new ConcurrentHashMap<>();
  private final AtomicLong temporaryDirectoryIds = new AtomicLong();

  public FileSnapshotStore(final Path root, final int maxSnapshotCount) {
    this.root = root;
    this.maxSnapshotCount = maxSnapshotCount;
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

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return snapshots.values().stream()
        .filter(snapshot -> !snapshot.isCorrupt())
        .max(Comparator.comparingLong(PersistedSnapshot::getIndex))
        .map(PersistedSnapshot.class::cast);
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot(final SnapshotType type) {
    return snapshots.values().stream()
        .filter(snapshot -> !snapshot.isCorrupt() && snapshot.getType() == type)
        .max(Comparator.comparingLong(PersistedSnapshot::getIndex))
        .map(PersistedSnapshot.class::cast);
  }

  @Override
  public Optional<PersistedSnapshot> getSnapshotAt(final long index) {
    return snapshots.values().stream()
        .filter(snapshot -> snapshot.getIndex() == index)
        .findFirst()
        .map(PersistedSnapshot.class::cast);
  }

  @Override
  public CompletableFuture<Long> getCompactionBound() {
    final long bound =
        snapshots.values().stream()
            .filter(snapshot -> !snapshot.isCorrupt())
            .mapToLong(PersistedSnapshot::getIndex)
            .min()
            .orElse(0L);
    return CompletableFuture.completedFuture(bound);
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
  public CompletableFuture<Void> abortPendingSnapshots() {
    purgeTemporaryDirectories();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Optional<TransientSnapshot> newTransientSnapshot(
      final long index,
      final long term,
      final String nodeId,
      final int replicationThreads,
      final SnapshotType type,
      final int version,
      final Map<String, String> businessInfo) {
    final SnapshotMetadata metadata =
        new SnapshotMetadata(index, term, nodeId, type, version, businessInfo);
    // 同 id 快照允许重拍：创建不拦截，提交时按同位点替换
    final Path temporaryDirectory = newTemporaryDirectory();
    return Optional.of(new FileTransientSnapshot(this, metadata, temporaryDirectory));
  }

  @Override
  public CompletableFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    final SnapshotMetadata metadata;
    try {
      metadata = SnapshotMetadata.fromSnapshotId(snapshotId);
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

    final Path temporaryDirectory = newTemporaryDirectory();
    return CompletableFuture.completedFuture(
        new FileReceivedSnapshot(this, metadata, temporaryDirectory));
  }

  /** Commits a fully written snapshot from its temporary directory into the store. */
  PersistedSnapshot commit(final SnapshotManifest manifest, final Path temporaryDirectory) {
    final SnapshotMetadata metadata = manifest.metadata;
    final String snapshotId = metadata.getSnapshotIdAsString();

    // 幂等重拍：仅当存在 index 严格更大的快照时拒绝；同 id 视为替换提交
    if (snapshots.values().stream().anyMatch(snapshot -> snapshot.getIndex() > metadata.index())) {
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

    final FilePersistedSnapshot persistedSnapshot =
        new FilePersistedSnapshot(destination, manifest);
    snapshots.put(keyOf(manifest.type(), snapshotId), persistedSnapshot);
    enforceRetention();
    LOGGER.debug("Committed snapshot {} at {}", snapshotId, destination);
    notifyListeners(persistedSnapshot);
    return persistedSnapshot;
  }

  private void enforceRetention() {
    final List<FilePersistedSnapshot> retained =
        snapshots.values().stream()
            .sorted(
                Comparator.comparingLong(PersistedSnapshot::getIndex)
                    .reversed()
                    .thenComparing(PersistedSnapshot::getTerm, Comparator.reverseOrder()))
            .toList();
    if (retained.size() <= maxSnapshotCount) {
      return;
    }
    retained.subList(maxSnapshotCount, retained.size())
        .forEach(
            snapshot -> {
              if (snapshot.isReserved()) {
                LOGGER.debug(
                    "Skipping reserved snapshot {} during retention enforcement",
                    snapshot.getId());
                return;
              }
              snapshots.remove(keyOf(snapshot.getType(), snapshot.getId()));
              snapshot.delete();
              deleteMarkerOf(snapshot.getPath());
              LOGGER.debug(
                  "Deleted snapshot {} exceeding the maximum snapshot count", snapshot.getId());
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

  /** 删除整个快照存储根目录并清空内存快照表。 */
  @Override
  public CompletableFuture<Void> delete() {
    snapshots.clear();
    FilePersistedSnapshot.deleteRecursively(root);
    return CompletableFuture.completedFuture(null);
  }

  /** 关闭存储：清空内存状态，文件存储无需额外资源释放。 */
  @Override
  public void close() {
    snapshots.clear();
  }
}
