/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.cluster.cluster;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class NoopSnapshotStore implements ReceivableSnapshotStore {

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.empty();
  }

  @Override
  public Iterable<PersistedSnapshot> getAvailableSnapshots() {
    return Collections.emptyList();
  }

  @Override
  public long getCompactionBound() {
    return 0;
  }

  @Override
  public ReceivedSnapshot newReceivedSnapshot(final String snapshotId) {
    return null;
  }

  @Override
  public CompletableFuture<Void> purgePendingSnapshots() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {}

  @Override
  public void close() {}
}
