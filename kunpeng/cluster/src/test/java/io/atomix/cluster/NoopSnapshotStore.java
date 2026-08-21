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
package io.atomix.cluster;

import io.atomix.raft.snapshot.PersistedSnapshot;
import io.atomix.raft.snapshot.PersistedSnapshotListener;
import io.atomix.raft.snapshot.ReceivableSnapshotStore;
import io.atomix.raft.snapshot.ReceivedSnapshot;
import io.atomix.raft.snapshot.TransientSnapshot;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** A no-op snapshot store which never holds any snapshots. */
public class NoopSnapshotStore implements ReceivableSnapshotStore {

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.empty();
  }

  @Override
  public Optional<PersistedSnapshot> getSnapshotAt(final long index) {
    return Optional.empty();
  }

  @Override
  public CompletableFuture<Long> getCompactionBound() {
    return CompletableFuture.completedFuture(0L);
  }

  @Override
  public int getMaxSnapshotCount() {
    return 0;
  }

  @Override
  public CompletableFuture<Integer> deleteSnapshotsFrom(final long index) {
    return CompletableFuture.completedFuture(0);
  }

  @Override
  public CompletableFuture<Void> abortPendingSnapshots() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Optional<TransientSnapshot> newTransientSnapshot(
      final long index,
      final long term,
      final String nodeId,
      final int replicationThreads,
      final io.atomix.raft.snapshot.SnapshotType type,
      final int version,
      final java.util.Map<String, String> businessInfo) {
    return Optional.of(
        new TransientSnapshot() {
          @Override
          public CompletableFuture<Void> take(final java.util.function.Consumer<java.nio.file.Path> writer) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public CompletableFuture<PersistedSnapshot> commit() {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public void abort() {}
        });
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public CompletableFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("NoopSnapshotStore cannot receive snapshots"));
  }
}
