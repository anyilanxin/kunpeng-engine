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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class InMemorySnapshot implements PersistedSnapshot, ReceivedSnapshot {

  private final TestSnapshotStore testSnapshotStore;
  private final long index;
  private final long term;
  private final String id;
  private final NavigableMap<String, byte[]> chunks = new TreeMap<>();
  private final Checksum checksumCalculator = new CRC32C();

  private long checksum;

  InMemorySnapshot(final TestSnapshotStore testSnapshotStore, final String snapshotId) {
    this.testSnapshotStore = testSnapshotStore;
    id = snapshotId;
    final var parts = snapshotId.split("-");
    index = Long.parseLong(parts[0]);
    term = Long.parseLong(parts[1]);
  }

  InMemorySnapshot(final TestSnapshotStore testSnapshotStore, final long index, final long term) {
    this.testSnapshotStore = testSnapshotStore;
    this.index = index;
    this.term = term;
    id = String.format("%d-%d", index, term);
  }

  public static InMemorySnapshot newPersistedSnapshot(
      final long index, final long term, final int size, final TestSnapshotStore snapshotStore) {
    final var snapshot = new InMemorySnapshot(snapshotStore, index, term);
    for (int i = 0; i < size; i++) {
      snapshot.writeChunks("chunk-" + i, "test".getBytes(StandardCharsets.UTF_8));
    }
    snapshot.persist();
    return snapshot;
  }

  void writeChunks(final String id, final byte[] chunk) {
    chunks.put(id, chunk);
    checksumCalculator.update(chunk);
  }

  @Override
  public int version() {
    return 1;
  }

  @Override
  public long getIndex() {
    return index;
  }

  @Override
  public long getTerm() {
    return term;
  }

  @Override
  public SnapshotChunkReader newChunkReader() {
    return new SnapshotChunkReader() {
      private NavigableMap<String, byte[]> iterator = chunks;

      @Override
      public void seek(final ByteBuffer chunkId) {
        final String key = new String(chunkId.array(), 0, chunkId.remaining());
        iterator = chunks.tailMap(key, true);
      }

      @Override
      public ByteBuffer nextId() {
        if (!hasNext()) {
          return null;
        }
        return ByteBuffer.wrap(iterator.firstEntry().getKey().getBytes(StandardCharsets.UTF_8));
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
  public String getId() {
    return id;
  }

  @Override
  public long getChecksum() {
    return checksum;
  }

  @Override
  public Path getPath() {
    return null;
  }

  // ===== ReceivedSnapshot =====

  @Override
  public long index() {
    return index;
  }

  @Override
  public CompletableFuture<Void> apply(final SnapshotChunk chunk) {
    chunks.put(chunk.getChunkName(), chunk.getContent());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void abort() {}

  @Override
  public CompletableFuture<PersistedSnapshot> persist() {
    testSnapshotStore.newSnapshot(this);
    checksum = checksumCalculator.getValue();
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, term, id);
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
    return index == that.index
        && term == that.term
        && id.equals(that.id)
        && chunks.equals(that.chunks);
  }
}
