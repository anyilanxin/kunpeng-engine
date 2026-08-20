/*
 * Copyright 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TestSnapshotStore implements ReceivableSnapshotStore {

  final AtomicReference<InMemorySnapshot> currentPersistedSnapshot;
  final List<InMemorySnapshot> receivedSnapshots = new CopyOnWriteArrayList<>();
  final List<PersistedSnapshotListener> listeners = new CopyOnWriteArrayList<>();

  public TestSnapshotStore(final AtomicReference<InMemorySnapshot> persistedSnapshotRef) {
    currentPersistedSnapshot = persistedSnapshotRef;
  }

  void newSnapshot(final InMemorySnapshot snapshot) {
    currentPersistedSnapshot.set(snapshot);
    listeners.forEach(listener -> listener.onNewSnapshot(snapshot));
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(currentPersistedSnapshot.get());
  }

  @Override
  public Iterable<PersistedSnapshot> getAvailableSnapshots() {
    return Collections.singletonList(currentPersistedSnapshot.get());
  }

  @Override
  public long getCompactionBound() {
    return getCurrentSnapshotIndex();
  }

  @Override
  public ReceivedSnapshot newReceivedSnapshot(final String snapshotId) {
    final var snapshot = new InMemorySnapshot(this, snapshotId);
    receivedSnapshots.add(snapshot);
    return snapshot;
  }

  @Override
  public CompletableFuture<Void> purgePendingSnapshots() {
    receivedSnapshots.clear();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.remove(listener);
  }

  public long getCurrentSnapshotIndex() {
    if (currentPersistedSnapshot.get() == null) {
      return 0;
    }
    return currentPersistedSnapshot.get().getIndex();
  }

  @Override
  public void close() {}
}
