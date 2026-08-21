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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-memory test double implementing both {@link PersistedSnapshot} and {@link ReceivedSnapshot}. */
public final class InMemorySnapshot implements PersistedSnapshot, ReceivedSnapshot {

  private final TestSnapshotStore testSnapshotStore;
  private final SnapshotMetadata metadata;
  private final String id;
  private final NavigableMap<String, byte[]> chunks = new TreeMap<>();
  private final AtomicBoolean reserved = new AtomicBoolean(false);

  InMemorySnapshot(final TestSnapshotStore testSnapshotStore, final String snapshotId) {
    this.testSnapshotStore = testSnapshotStore;
    id = snapshotId;
    final var parts = snapshotId.split("-");
    metadata =
        SnapshotMetadata.of(
            Long.parseLong(parts[0]),
            Long.parseLong(parts[1]),
            parts[2],
            SnapshotType.REGULAR);
  }

  InMemorySnapshot(
      final TestSnapshotStore testSnapshotStore,
      final long index,
      final long term,
      final int nodeId) {
    this.testSnapshotStore = testSnapshotStore;
    id = String.format("%d-%d-%d", index, term, nodeId);
    metadata = SnapshotMetadata.of(index, term, String.valueOf(nodeId), SnapshotType.REGULAR);
  }

  public static InMemorySnapshot newPersistedSnapshot(
      final int nodeId,
      final long index,
      final long term,
      final int size,
      final TestSnapshotStore snapshotStore) {
    return newPersistedSnapshot(nodeId, index, term, size, snapshotStore, false);
  }

  public static InMemorySnapshot newPersistedSnapshot(
      final int nodeId,
      final long index,
      final long term,
      final int size,
      final TestSnapshotStore snapshotStore,
      final boolean withMetadata) {
    final var snapshot = new InMemorySnapshot(snapshotStore, index, term, nodeId);
    for (int i = 0; i < size; i++) {
      snapshot.writeChunks("chunk-" + i, ("test-" + i).getBytes(StandardCharsets.UTF_8));
    }
    if (withMetadata) {
      // Mirror a real snapshot, which ships a metadata chunk excluded from the total size in
      // bytes.
      snapshot.writeChunks("metadata", "metadata".getBytes(StandardCharsets.UTF_8));
    }
    snapshot.persist();
    return snapshot;
  }

  void writeChunks(final String id, final byte[] chunk) {
    chunks.put(id, chunk);
  }

  @Override
  public int version() {
    return 1;
  }

  @Override
  public SnapshotMetadata getMetadata() {
    return metadata;
  }

  @Override
  public long size() {
    return chunks.values().stream().mapToLong(chunk -> chunk.length).sum();
  }

  @Override
  public Path getPath() {
    return null;
  }

  @Override
  public SnapshotChunkReader newChunkReader() {
    return new SnapshotChunkReader() {
      private NavigableMap<String, byte[]> iterator = chunks;

      @Override
      public void reset() {
        iterator = chunks;
      }

      @Override
      public void seek(final ByteBuffer id) {
        final var chunkId = StandardCharsets.UTF_8.decode(id).toString();
        iterator = chunks.tailMap(chunkId, true);
      }

      @Override
      public ByteBuffer nextId() {
        if (!hasNext()) {
          return null;
        }
        return ByteBuffer.wrap(iterator.firstEntry().getKey().getBytes(StandardCharsets.UTF_8));
      }

      @Override
      public void setMaximumChunkSize(final int maximumChunkSize) {}

      @Override
      public void close() {
        iterator = null;
      }

      @Override
      public boolean hasNext() {
        return !iterator.isEmpty();
      }

      @Override
      public SnapshotChunk next() {
        final var nextEntry = iterator.firstEntry();
        iterator = chunks.tailMap(nextEntry.getKey(), false);
        return new TestSnapshotChunkImpl(
            id, nextEntry.getKey(), nextEntry.getValue(), chunks.size());
      }
    };
  }

  @Override
  public void delete() {
    if (testSnapshotStore != null) {
      testSnapshotStore.removeSnapshot(this);
    }
  }

  @Override
  public boolean isCorrupt() {
    return false;
  }

  /** 预留该快照以防被存储删除，返回的句柄关闭时解除预留。 */
  public CloseableSilently reserve() {
    reserved.set(true);
    return () -> reserved.set(false);
  }

  boolean isReserved() {
    return reserved.get();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIndex(), getTerm(), id);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final InMemorySnapshot that = (InMemorySnapshot) o;
    return getIndex() == that.getIndex()
        && getTerm() == that.getTerm()
        && id.equals(that.id)
        && chunks.equals(that.chunks);
  }

  @Override
  public String toString() {
    return "InMemorySnapshot{"
        + "index="
        + getIndex()
        + ", term="
        + getTerm()
        + ", id='"
        + id
        + "'}";
  }

  // ReceivedSnapshot

  @Override
  public SnapshotMetadata snapshotId() {
    return metadata;
  }

  @Override
  public CompletableFuture<Void> apply(final SnapshotChunk chunk) {
    chunks.put(chunk.getChunkName(), chunk.getContent());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<PersistedSnapshot> persist() {
    if (testSnapshotStore != null) {
      testSnapshotStore.newSnapshot(this);
    }
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public void abort() {}
}
