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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.v2;

import com.anyilanxin.kunpeng.cluster.raft.journal.snapshots.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.journal.snapshots.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.journal.snapshots.SnapshotChunk;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link ReceivedSnapshot} 的 v2 实现（包装 {@link IncomingReplica}） */
final class V2ReceivedSnapshot implements ReceivedSnapshot {

  private static final Logger LOG = LoggerFactory.getLogger(V2ReceivedSnapshot.class);

  private final SnapshotVault vault;
  private final IncomingReplica replica;

  V2ReceivedSnapshot(final SnapshotVault vault, final IncomingReplica replica) {
    this.vault = vault;
    this.replica = replica;
  }

  @Override
  public long index() {
    return replica.ref().index();
  }

  @Override
  public CompletableFuture<Void> apply(final SnapshotChunk chunk) {
    final var block = toBlock(chunk);
    return vault.applyBlock(replica, block);
  }

  @Override
  public CompletableFuture<PersistedSnapshot> persist() {
    return vault.commitReplica(replica)
        .thenApply(
            v -> {
              final var latest = vault.getLatestSnapshot();
              if (latest.isPresent() && latest.get().ref().index() == replica.ref().index()) {
                return (PersistedSnapshot) new V2PersistedSnapshot(latest.get());
              }
              LOG.warn("快照提交后未找到对应落档: {}", replica.ref());
              return null;
            });
  }

  @Override
  public void abort() {
    replica.abort();
  }

  @Override
  public String toString() {
    return "V2ReceivedSnapshot{" + replica.ref() + "}";
  }

  /** v1 wire 块转 v2 内部传输块 */
  private static SnapshotBlock toBlock(final SnapshotChunk chunk) {
    final String blockName = chunk.getChunkName();
    final int separator = blockName.lastIndexOf(':');
    final String fileName = separator > 0 ? blockName.substring(0, separator) : blockName;
    final long fileOffset = separator > 0 ? Long.parseLong(blockName.substring(separator + 1)) : 0;
    return SnapshotBlock.of(
        chunk.getSnapshotId(),
        chunk.getTotalCount(),
        fileName,
        fileOffset,
        0,
        chunk.getContent());
  }
}
