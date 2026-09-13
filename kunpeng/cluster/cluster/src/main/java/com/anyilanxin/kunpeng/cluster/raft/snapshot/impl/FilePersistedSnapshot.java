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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件持久镜像：镜像目录（内容文件 + snapshot.metadata）+ 同级 .sfc 校验文件。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class FilePersistedSnapshot implements PersistedSnapshot {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilePersistedSnapshot.class);
  private static final int SNAPSHOT_VERSION = 1;

  private final Path path;
  private final SimpleFileVerificationInfo checksums;
  private final SnapshotId snapshotId;
  private final SnapshotMetadata metadata;
  private final long totalSizeInBytes;

  public FilePersistedSnapshot(
      final Path path,
      final SimpleFileVerificationInfo checksums,
      final SnapshotId snapshotId,
      final SnapshotMetadata metadata) {
    this.path = path;
    this.checksums = checksums;
    this.snapshotId = snapshotId;
    this.metadata = metadata;
    totalSizeInBytes =
        checksums.getFileInfos().values().stream().mapToLong(SnapshotFileInfo::size).sum();
  }

  @Override
  public int version() {
    return SNAPSHOT_VERSION;
  }

  @Override
  public SnapshotId snapshotId() {
    return snapshotId;
  }

  @Override
  public long getIndex() {
    return snapshotId.index();
  }

  @Override
  public long getTerm() {
    return snapshotId.term();
  }

  @Override
  public SnapshotChunkReader newChunkReader(final UUID readerId) {
    return new FileSnapshotChunkReader(path, checksums);
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public Path getChecksumPath() {
    return checksums.getSfvPath();
  }

  @Override
  public SimpleFileVerificationChecksums getChecksums() {
    return checksums;
  }

  @Override
  public SnapshotMetadata getMetadata() {
    return metadata;
  }

  @Override
  public long getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  /** 递归删除目录（不存在则跳过）。 */
  public static void deleteRecursively(final Path path) {
    if (!Files.exists(path)) {
      return;
    }
    try (final var stream = Files.walk(path)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
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
    return "FilePersistedSnapshot{path=" + path + ", snapshotId=" + snapshotId + '}';
  }
}
