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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件持久快照公共基类：目录 = manifest + 内容文件；三个子类固定快照的公共类型。
 */
abstract class FilePersistedSnapshot implements PersistedSnapshot {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilePersistedSnapshot.class);
  private static final int SNAPSHOT_VERSION = 1;

  private final Path path;
  private final SnapshotManifest manifest;
  // 预留计数：大于 0 时该快照不可被保留策略或按索引删除
  private final AtomicLong reservations = new AtomicLong();
  private volatile boolean corrupt;

  FilePersistedSnapshot(final Path path, final SnapshotManifest manifest) {
    this.path = path;
    this.manifest = manifest;
  }

  /** 按 manifest 类型分派到对应类型子类。 */
  static FilePersistedSnapshot of(final Path path, final SnapshotManifest manifest) {
    return switch (manifest.type()) {
      case REGULAR -> new FileRaftSnapshot(path, manifest);
      case BOOTSTRAP -> new FileBootstrapSnapshot(path, manifest);
      case MERGE -> new FileMergeSnapshot(path, manifest);
    };
  }

  /** Loads the snapshot stored in the given directory, marking it corrupt if unreadable. */
  static FilePersistedSnapshot load(final Path path) {
    try {
      return of(path, SnapshotManifest.read(path));
    } catch (final Exception e) {
      LOGGER.warn("Snapshot directory {} has no valid manifest, marking corrupt", path, e);
      final var corruptSnapshot = of(path, new SnapshotManifest(corruptMetadata(path), List.of()));
      corruptSnapshot.corrupt = true;
      return corruptSnapshot;
    }
  }

  private static SnapshotMetadata corruptMetadata(final Path path) {
    final String name = path.getFileName().toString();
    try {
      return SnapshotMetadata.fromSnapshotId(name);
    } catch (final IllegalArgumentException e) {
      return SnapshotMetadata.of(-1, -1, name, SnapshotType.REGULAR);
    }
  }

  @Override
  public SnapshotMetadata getMetadata() {
    return manifest.metadata;
  }

  @Override
  public SnapshotType getType() {
    return manifest.type();
  }

  @Override
  public int version() {
    return SNAPSHOT_VERSION;
  }

  @Override
  public long size() {
    return manifest.totalSize();
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public SnapshotChunkReader newChunkReader() {
    return new FileSnapshotChunkReader(this);
  }

  @Override
  public void delete() {
    deleteRecursively(path);
  }

  @Override
  public boolean isCorrupt() {
    return corrupt;
  }

  /** 校验目录内容与 manifest 的 size 与 CRC32 记录是否一致。 */
  boolean verifyContent() {
    return manifest.verifyContent(path);
  }

  @Override
  public CloseableSilently reserve() {
    reservations.incrementAndGet();
    return reservations::decrementAndGet;
  }

  /** 当前是否被预留（预留计数大于 0）。 */
  boolean isReserved() {
    return reservations.get() > 0;
  }

  SnapshotManifest manifest() {
    return manifest;
  }

  static void deleteRecursively(final Path path) {
    if (!Files.exists(path)) {
      return;
    }
    try (final var stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (final IOException e) {
          LOGGER.warn("Failed to delete {}", p, e);
        }
      });
    } catch (final IOException e) {
      LOGGER.warn("Failed to delete directory {}", path, e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{path=" + path + ", metadata=" + manifest.metadata + '}';
  }
}
