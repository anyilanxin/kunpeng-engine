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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@link ReceivableSnapshotStore} 的 vault 实现（包装 {@link SnapshotVault}）
 */
public final class VaultSnapshotStore implements ReceivableSnapshotStore {

  private static final Logger LOG = LoggerFactory.getLogger(VaultSnapshotStore.class);

  private final SnapshotVault vault;

  public VaultSnapshotStore(final SnapshotVault vault) {
    this.vault = vault;
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return vault.getLatestSnapshot().map(VaultPersistedSnapshot::new);
  }

  @Override
  public Iterable<PersistedSnapshot> getAvailableSnapshots() {
    return (Iterable<PersistedSnapshot>) (Iterable<?>) vault.getAvailableSnapshots().stream()
        .map(VaultPersistedSnapshot::new)
        .toList();
  }

  @Override
  public long getCompactionBound() {
    return vault.getCompactionBound();
  }

  @Override
  public ReceivedSnapshot newReceivedSnapshot(final String snapshotId) {
    final IncomingReplica replica = vault.receive(snapshotId).join();
    return new VaultReceivedSnapshot(vault, replica);
  }

  @Override
  public CompletableFuture<Void> purgePendingSnapshots() {
    return vault.abortPendingSnapshots();
  }

  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {
    vault.addSnapshotListener(listener);
  }

  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {
    vault.removeSnapshotListener(listener);
  }

  @Override
  public void close() {
    vault.close();
  }

  SnapshotVault vault() {
    return vault;
  }
}
