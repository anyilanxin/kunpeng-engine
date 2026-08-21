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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * A snapshot which has been persisted on local disk. Instances are immutable views; use {@link
 * #delete()} to remove one.
 */
public interface PersistedSnapshot {

  /** Returns the metadata (index, term, nodeId) of this snapshot. */
  SnapshotMetadata getMetadata();

  /** Returns the type of this snapshot, as persisted in its manifest. */
  default SnapshotType getType() {
    return SnapshotType.REGULAR;
  }

  /** Returns the Raft log index of this snapshot. */
  default long getIndex() {
    return getMetadata().index();
  }

  /** Returns the Raft term of this snapshot. */
  default long getTerm() {
    return getMetadata().term();
  }

  /** Returns the canonical id of this snapshot, in the format {@code index-term-nodeId}. */
  default String getId() {
    return getMetadata().getSnapshotIdAsString();
  }

  /**
   * Returns the version of the snapshot format. Used by install requests; bumped when the on-disk
   * or chunk format changes incompatibly.
   */
  int version();

  /** Returns the total size of this snapshot's files on disk, in bytes. */
  long size();

  /** Returns the total size of this snapshot's files on disk, in bytes. */
  default long getTotalSizeInBytes() {
    return size();
  }

  /** Returns the directory of this snapshot. */
  Path getPath();

  /**
   * Returns a new reader over the chunks of this snapshot.
   *
   * @throws UncheckedIOException if the snapshot cannot be opened
   */
  SnapshotChunkReader newChunkReader();

  /** Deletes this snapshot from disk. */
  void delete();

  /** Returns whether this snapshot failed verification and is considered corrupt. */
  boolean isCorrupt();

  /**
   * 预留该快照以防删除：句柄未关闭期间，该快照不参与保留策略删除与 {@code deleteSnapshotsFrom}
   * 删除。默认实现返回立即失效的空句柄。
   */
  default CloseableSilently reserve() {
    return () -> {};
  }
}
