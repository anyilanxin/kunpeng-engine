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

import java.util.concurrent.CompletableFuture;

/**
 * A snapshot currently being received from the leader, chunk by chunk. Chunks are applied via
 * {@link #apply(SnapshotChunk)}; once complete, {@link #persist()} verifies and commits it, or
 * {@link #abort()} discards it.
 */
public interface ReceivedSnapshot {

  /** Returns the metadata of the snapshot being received. */
  SnapshotMetadata snapshotId();

  /** Returns the Raft log index of the snapshot being received. */
  default long index() {
    return snapshotId().index();
  }

  /**
   * Writes the given chunk's content into the snapshot's temporary directory.
   *
   * @param chunk the received chunk
   * @return a future completed when the chunk is written
   */
  CompletableFuture<Void> apply(SnapshotChunk chunk);

  /**
   * Verifies the received content against the snapshot's manifest and commits it atomically.
   *
   * @return a future completed with the persisted snapshot
   */
  CompletableFuture<PersistedSnapshot> persist();

  /** Discards the snapshot and deletes any temporary files. */
  void abort();
}
