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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.ConstructableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存版快照测试替身：同时实现持久快照、拍摄 pending 与接收 pending 三种契约。
 */
public final class InMemorySnapshot
        implements PersistedSnapshot, ConstructableSnapshot, ReceivedSnapshot {

  private static final SimpleFileVerificationChecksums EMPTY_CHECKSUMS =
    new SimpleFileVerificationChecksums() {
            @Override
            public Map<String, SnapshotFileInfo> getFileInfos() {
              return Map.of();
            }

            @Override
            public long getCombinedChecksum() {
              return 0;
            }
          };

  private final TestSnapshotStore testSnapshotStore;
  private final SnapshotId snapshotId;
  private final NavigableMap<String, byte[]> chunks = new TreeMap<>();
  private final AtomicBoolean reserved = new AtomicBoolean(false);

  InMemorySnapshot(final TestSnapshotStore testSnapshotStore, final String snapshotId) {
    this.testSnapshotStore = testSnapshotStore;
    this.snapshotId = SnapshotId.fromString(snapshotId);
  }

  InMemorySnapshot(
      final TestSnapshotStore testSnapshotStore,
      final long index,
      final long term,
      final int nodeId) {
    this.testSnapshotStore = testSnapshotStore;
    snapshotId = new SnapshotId(String.valueOf(nodeId), index, term);
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
      snapshot.writeChunks("metadata", "metadata".getBytes(StandardCharsets.UTF_8));
    }
    snapshot.persist();
    return snapshot;
  }

  /**
   * 每个分片写入 {@code chunkSize} 字节内容，用于构造跨多个传输批的镜像。
   */
  public static InMemorySnapshot newPersistedSnapshot(
          final int nodeId,
          final long index,
          final long term,
          final int size,
          final TestSnapshotStore snapshotStore,
          final int chunkSize) {
    final var snapshot = new InMemorySnapshot(snapshotStore, index, term, nodeId);
    final byte[] content = new byte[chunkSize];
    for (int i = 0; i < size; i++) {
      snapshot.writeChunks("chunk-" + i, content);
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
  public SnapshotId snapshotId() {
    return snapshotId;
  }

  @Override
  public long getIndex() {
    return snapshotId.index();
  }

  @Override
  public long getTerm() {
    return snapshotId.term();
  }

  @Override
  public SnapshotChunkReader newChunkReader(final UUID readerId) {
    return new SnapshotChunkReader() {
      private NavigableMap<String, byte[]> iterator = chunks;

      @Override
      public void reset() {
        iterator = chunks;
      }

      @Override
      public void seek(final ByteBuffer id) {
        // 用 slice 读取，避免推进调用方（可能被 leader/follower 共享）buffer 的 position
        final ByteBuffer duplicate = id.slice();
        final byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        final var chunkId = new String(bytes, StandardCharsets.UTF_8);
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
        return new TestSnapshotChunkImpl(nextEntry.getKey(), nextEntry.getValue());
      }
    };
  }

  @Override
  public Path getPath() {
    return null;
  }

  @Override
  public Path getChecksumPath() {
    return null;
  }

  @Override
  public SimpleFileVerificationChecksums getChecksums() {
    return EMPTY_CHECKSUMS;
  }

  @Override
  public SnapshotMetadata getMetadata() {
    return SnapshotMetadata.of(snapshotId, Map.of());
  }

  @Override
  public long getTotalSizeInBytes() {
    return chunks.values().stream().mapToLong(chunk -> chunk.length).sum();
  }

  @Override
  public ActorFuture<PersistedSnapshot> persist() {
    if (testSnapshotStore != null) {
      testSnapshotStore.newSnapshot(this);
    }
    return CompletableActorFuture.completed(this);
  }

  @Override
  public ActorFuture<Void> abort() {
    return CompletableActorFuture.completed();
  }

  @Override
  public ActorFuture<Void> write(final SnapshotChunkBatch batch) {
    for (final SnapshotChunk chunk : batch.chunks()) {
      final ByteBuffer content = chunk.getContent();
      final byte[] bytes = new byte[content.remaining()];
      content.slice().get(bytes);
      chunks.put(chunk.getChunkName(), bytes);
    }
    return CompletableActorFuture.completed();
  }

  /** 预留该快照以防被存储删除，返回的句柄关闭时解除预留。 */
  public AutoCloseable reserve() {
    reserved.set(true);
    return () -> reserved.set(false);
  }

  boolean isReserved() {
    return reserved.get();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIndex(), getTerm(), snapshotId);
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
    if (!snapshotId.equals(that.snapshotId) || !chunks.keySet().equals(that.chunks.keySet())) {
      return false;
    }
    // TreeMap.equals 对 byte[] 值按引用比较，必须逐片按内容比较
    for (final var entry : chunks.entrySet()) {
      if (!Arrays.equals(entry.getValue(), that.chunks.get(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "InMemorySnapshot{snapshotId=" + snapshotId + '}';
  }
}
