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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/** A snapshot received from the leader, reconstructed chunk by chunk into a temporary directory. */
final class FileReceivedSnapshot implements ReceivedSnapshot {

  private final FileSnapshotStore store;
  private final SnapshotMetadata metadata;
  private final Path temporaryDirectory;
  // 首个分片声明的总分片数，后续分片必须与其一致
  private final AtomicReference<Integer> expectedTotalCount = new AtomicReference<>();
  // 已接收的不同分片名集合
  private final Set<String> receivedChunkNames = ConcurrentHashMap.newKeySet();

  FileReceivedSnapshot(
      final FileSnapshotStore store, final SnapshotMetadata metadata, final Path temporaryDirectory) {
    this.store = store;
    this.metadata = metadata;
    this.temporaryDirectory = temporaryDirectory;
  }

  @Override
  public SnapshotMetadata snapshotId() {
    return metadata;
  }

  @Override
  public CompletableFuture<Void> apply(final SnapshotChunk chunk) {
    return CompletableFuture.runAsync(() -> writeChunk(chunk));
  }

  private void writeChunk(final SnapshotChunk chunk) {
    if (!chunk.getSnapshotId().equals(metadata.getSnapshotIdAsString())) {
      throw new SnapshotException(
          "Expected chunk of snapshot " + metadata.getSnapshotIdAsString() + ", but got "
              + chunk.getSnapshotId());
    }

    // 总分片数一致性：记录首个分片的 totalCount，后续不一致即失败
    final Integer expected = expectedTotalCount.updateAndGet(
        current -> current == null ? chunk.getTotalCount() : current);
    if (expected != chunk.getTotalCount()) {
      abort();
      throw new SnapshotException(
          "Expected total chunk count " + expected + ", but got " + chunk.getTotalCount()
              + " for chunk " + chunk.getChunkName() + " of snapshot " + chunk.getSnapshotId());
    }

    final var crc = new CRC32();
    crc.update(chunk.getContent());
    if (crc.getValue() != chunk.getChecksum()) {
      throw new SnapshotException(
          "Checksum mismatch for chunk " + chunk.getChunkName() + " of snapshot " + chunk.getSnapshotId());
    }

    // chunkName 编码为 "文件名@字节偏移"，按其定位文件与写入偏移
    final String chunkName = chunk.getChunkName();
    final int separator = chunkName.lastIndexOf('@');
    if (separator <= 0 || separator == chunkName.length() - 1) {
      throw new SnapshotException("Malformed chunk name " + chunkName);
    }
    final String fileName = chunkName.substring(0, separator);
    final long offset;
    try {
      offset = Long.parseLong(chunkName.substring(separator + 1));
    } catch (final NumberFormatException e) {
      throw new SnapshotException("Malformed offset in chunk name " + chunkName, e);
    }
    final Path file = resolve(fileName);
    try {
      Files.createDirectories(file.getParent());
      try (final var channel =
          FileChannel.open(
              file,
              StandardOpenOption.WRITE,
              StandardOpenOption.READ,
              StandardOpenOption.CREATE)) {
        final var buffer = java.nio.ByteBuffer.wrap(chunk.getContent());
        long written = 0;
        while (buffer.hasRemaining()) {
          channel.position(offset + written);
          final int n = channel.write(buffer);
          if (n < 0) {
            throw new IOException("Failed to write chunk " + chunk.getChunkName());
          }
          written += n;
        }
        // 接收侧分片落盘：写完立即 force
        channel.force(true);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    receivedChunkNames.add(chunkName);
  }

  private Path resolve(final String fileName) {
    final Path resolved = temporaryDirectory.resolve(fileName).normalize();
    if (!resolved.startsWith(temporaryDirectory)) {
      throw new SnapshotException("Illegal file name in snapshot chunk: " + fileName);
    }
    return resolved;
  }

  @Override
  public CompletableFuture<PersistedSnapshot> persist() {
    return CompletableFuture.supplyAsync(
        () -> {
          final SnapshotManifest manifest;
          try {
            manifest = SnapshotManifest.read(temporaryDirectory);
          } catch (final Exception e) {
            abort();
            throw new SnapshotException(
                "Failed to read manifest of received snapshot " + metadata, e);
          }

          for (final SnapshotManifest.FileEntry entry : manifest.files) {
            final Path file = resolve(entry.name());
            try {
              if (Files.size(file) != entry.size()
                  || SnapshotManifest.checksumOf(file) != entry.checksum()) {
                abort();
                throw new SnapshotException(
                    "Verification failed for file " + entry.name() + " of snapshot " + metadata);
              }
            } catch (final IOException e) {
              abort();
              throw new UncheckedIOException(e);
            }
          }

          // 分片数完整性：实际接收到的不同分片数必须等于声明的 totalCount
          final Integer expectedCount = expectedTotalCount.get();
          if (expectedCount == null || receivedChunkNames.size() != expectedCount) {
            abort();
            throw new SnapshotException(
                "Received " + receivedChunkNames.size() + " chunks of snapshot " + metadata
                    + ", but expected " + expectedCount);
          }

          return store.commit(manifest, temporaryDirectory);
        });
  }

  @Override
  public void abort() {
    FilePersistedSnapshot.deleteRecursively(temporaryDirectory);
  }

  @Override
  public String toString() {
    return "FileReceivedSnapshot{metadata=" + metadata + ", path=" + temporaryDirectory + '}';
  }
}
