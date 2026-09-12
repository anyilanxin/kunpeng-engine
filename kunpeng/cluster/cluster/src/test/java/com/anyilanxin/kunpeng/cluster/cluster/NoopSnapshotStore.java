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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.ConstructableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;

import java.nio.file.Path;
import java.util.Optional;

/** A no-op snapshot store which never holds any snapshots. */
public class NoopSnapshotStore implements RaftSnapshotStore {

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.empty();
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    return CompletableActorFuture.completed(0L);
  }

  @Override
  public int getMaxSnapshotCount() {
    return 0;
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    return CompletableActorFuture.completed();
  }

  @Override
  public void start() {
  }

  @Override
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(final long index, final long term) {
    return CompletableActorFuture.completedExceptionally(
        new SnapshotException("NoopSnapshotStore cannot take snapshots"));
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    return CompletableActorFuture.completedExceptionally(
            new UnsupportedOperationException("NoopSnapshotStore cannot receive snapshots"));
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    return CompletableActorFuture.completed(true);
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
    return CompletableActorFuture.completed(true);
  }

  @Override
  public long getCurrentSnapshotIndex() {
    return 0;
  }

  @Override
  public ActorFuture<Void> delete() {
    return CompletableActorFuture.completed();
  }

  @Override
  public Path getPath() {
    return null;
  }
}
