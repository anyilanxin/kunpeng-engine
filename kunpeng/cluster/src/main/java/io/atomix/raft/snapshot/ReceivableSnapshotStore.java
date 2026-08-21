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
package io.atomix.raft.snapshot;

import java.util.concurrent.CompletableFuture;

/** A snapshot store which can also receive snapshots replicated from a leader, chunk by chunk. */
public interface ReceivableSnapshotStore extends PersistedSnapshotStore {

  /**
   * Creates a new received snapshot for the given snapshot id ({@code index-term-nodeId}). The
   * returned snapshot writes into a temporary directory until committed.
   *
   * @param snapshotId the id of the snapshot to receive
   * @return a future completed with the received snapshot
   * @throws SnapshotException.SnapshotAlreadyExistsException if an identical snapshot exists
   */
  CompletableFuture<ReceivedSnapshot> newReceivedSnapshot(String snapshotId);
}
