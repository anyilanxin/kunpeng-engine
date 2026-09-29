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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 镜像持久对象
 *
 * @author zxuanhong
 */
public interface PersistedSnapshot {
  int version();

  SnapshotId snapshotId();

  long getIndex();

  long getTerm();

  SnapshotChunkReader newChunkReader(UUID readerId);

  Path getPath();

  Path getChecksumPath();

  SimpleFileVerificationChecksums getChecksums();

  SnapshotMetadata getMetadata();

  long getTotalSizeInBytes();

  /**
   * 快照文件列表
   *
   * @return {@code Map<String, Path>}
   */
  default Map<String, Path> files() {
    final var map = new HashMap<String, Path>();
    try (final var stream = Files.list(getPath())) {
      stream.forEach(
          file -> {
            final var fileName = file.getFileName().toString();
            map.put(fileName, file);
          });

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    map.put(getChecksumPath().getFileName().toString(), getChecksumPath());
    return map;
  }
}
