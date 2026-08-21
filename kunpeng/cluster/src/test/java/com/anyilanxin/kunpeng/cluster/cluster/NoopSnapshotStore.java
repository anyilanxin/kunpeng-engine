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
package com.anyilanxin.kunpeng.cluster.cluster;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.BootstrapSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.MergeSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.scheduler.Either;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** A no-op snapshot store which never holds any snapshots. */
public class NoopSnapshotStore implements ReceivableSnapshotStore {

  @Override
  public Optional<RaftSnapshot> getLatestSnapshot() {
    return Optional.empty();
  }

  @Override
  public Optional<RaftSnapshot> getSnapshotAt(final long index) {
    return Optional.empty();
  }

  @Override
  public CompletableFuture<Long> getCompactionBound() {
    return CompletableFuture.completedFuture(0L);
  }

  @Override
  public Optional<BootstrapSnapshot> getBootstrapSnapshot() {
    return Optional.empty();
  }

  @Override
  public Optional<MergeSnapshot> getMergeSnapshot() {
    return Optional.empty();
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
  public ActorFuture<Void> abortPendingSnapshots() {
    return CompletableActorFuture.completed();
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newTransientSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return Either.left(
        new SnapshotException("NoopSnapshotStore cannot take snapshots"));
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newBootstrapSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newTransientSnapshot(index, term, businessInfo);
  }

  @Override
  public Either<SnapshotException, PersistableSnapshot> newMergeSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newTransientSnapshot(index, term, businessInfo);
  }

  @Override
  public ActorFuture<PersistedSnapshot> copyForBootstrap(
      final BiConsumer<java.nio.file.Path, java.nio.file.Path> copySnapshot) {
    return CompletableActorFuture.completedExceptionally(
        new SnapshotException("NoopSnapshotStore cannot copy snapshots"));
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public CompletableFuture<PersistableSnapshot> newReceivedSnapshot(final String snapshotId) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("NoopSnapshotStore cannot receive snapshots"));
  }
}
