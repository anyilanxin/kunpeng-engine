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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkAppender;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/**
 * 文件版 pending 快照：统一"拍摄"与"接收"两种模式。
 *
 * <p>拍摄模式业务直接写 {@link #getPath()}，persist 时生成 manifest 再三阶段提交；接收模式分片
 * 经 {@link #append(SnapshotChunk)} 逐片写入与校验，persist 时校验已传输的 manifest 与内容。
 */
final class FilePersistableSnapshot implements PersistableSnapshot, SnapshotChunkAppender {

  private final FileSnapshotStore store;
  private final SnapshotMetadata metadata;
  private final Path temporaryDirectory;
  // 接收模式：persist 时校验已传输的 manifest，而非生成
  private final boolean receiveMode;
  // 首个分片声明的总分片数，后续分片必须与其一致
  private final AtomicReference<Integer> expectedTotalCount = new AtomicReference<>();
  // 已接收的不同分片名集合
  private final Set<String> receivedChunkNames = ConcurrentHashMap.newKeySet();
  /** 批量写盘的累计字节阈值，缺省 1MiB；单片超限独占一批（最少 1 片）。 */
  static final long DEFAULT_WRITE_BATCH_BYTES = 1024L * 1024;
  private final long maxWriteBatchBytes;
  // 待落盘的缓冲分片与累计字节数（append 侧填充，达到阈值或 flush 时整批写）
  private final List<SnapshotChunk> writeBuffer = new ArrayList<>();
  private long bufferedBytes;

  FilePersistableSnapshot(
      final FileSnapshotStore store,
      final SnapshotMetadata metadata,
      final Path temporaryDirectory,
      final boolean receiveMode) {
    this(store, metadata, temporaryDirectory, receiveMode, DEFAULT_WRITE_BATCH_BYTES);
  }

  FilePersistableSnapshot(
      final FileSnapshotStore store,
      final SnapshotMetadata metadata,
      final Path temporaryDirectory,
      final boolean receiveMode,
      final long maxWriteBatchBytes) {
    this.store = store;
    this.metadata = metadata;
    this.temporaryDirectory = temporaryDirectory;
    this.receiveMode = receiveMode;
    this.maxWriteBatchBytes = maxWriteBatchBytes;
  }

  @Override
  public SnapshotId snapshotId() {
    return SnapshotId.fromMetadata(metadata);
  }

  @Override
  public Path getPath() {
    return temporaryDirectory;
  }

  @Override
  public ActorFuture<PersistedSnapshot> persist() {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    CompletableFuture.supplyAsync(this::doPersist)
        .whenComplete(
            (result, error) -> {
              if (error != null) {
                future.completeExceptionally(
                    error.getCause() != null ? error.getCause() : error);
              } else {
                future.complete(result);
              }
            });
    return future;
  }

  private PersistedSnapshot doPersist() {
    final SnapshotManifest manifest;
    if (receiveMode) {
      // 先把缓冲分片整批落盘——manifest 文件本身也是分片传输的，可能仍在缓冲中
      flush();
      try {
        manifest = SnapshotManifest.read(temporaryDirectory);
      } catch (final Exception e) {
        abortSync();
        throw new SnapshotException(
            "Failed to read manifest of received snapshot " + metadata, e);
      }
      verifyReceivedContent(manifest);
      try {
        verifyComplete();
      } catch (final RuntimeException e) {
        abortSync();
        throw e;
      }
    } else {
      try {
        Files.createDirectories(temporaryDirectory);
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
      manifest = SnapshotManifest.of(metadata, temporaryDirectory);
      // 空快照拒绝：目录内除 manifest 外没有任何内容文件
      if (manifest.files.isEmpty()) {
        abortSync();
        throw new SnapshotException(
            "Cannot commit snapshot "
                + metadata.getSnapshotIdAsString()
                + "; snapshot content is empty");
      }
      manifest.write(temporaryDirectory);
    }
    final PersistedSnapshot persisted = store.commit(manifest, temporaryDirectory);
    store.removePending(this);
    return persisted;
  }

  /** 接收模式：逐文件校验已传输内容与 manifest 的 size 与 CRC32 是否一致。 */
  private void verifyReceivedContent(final SnapshotManifest manifest) {
    for (final SnapshotManifest.FileEntry entry : manifest.files) {
      final Path file = resolve(temporaryDirectory, entry.name());
      try {
        if (Files.size(file) != entry.size()
            || SnapshotManifest.checksumOf(file) != entry.checksum()) {
          abortSync();
          throw new SnapshotException(
              "Verification failed for file " + entry.name() + " of snapshot " + metadata);
        }
      } catch (final IOException e) {
        abortSync();
        throw new UncheckedIOException(e);
      }
    }
  }

  @Override
  public ActorFuture<Void> abort() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    CompletableFuture.runAsync(this::abortSync)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                future.completeExceptionally(error.getCause() != null ? error.getCause() : error);
              } else {
                future.complete(null);
              }
            });
    return future;
  }

  private void abortSync() {
    store.removePending(this);
    FilePersistedSnapshot.deleteRecursively(temporaryDirectory);
  }

  @Override
  public CompletableFuture<Void> append(final SnapshotChunk chunk) {
    return CompletableFuture.runAsync(() -> appendSync(chunk));
  }

  /** 校验后缓冲分片；累计字节达到批阈值即整批写盘（单片超限独占一批，最少 1 片）。 */
  private synchronized void appendSync(final SnapshotChunk chunk) {
    if (!chunk.getSnapshotId().equals(metadata.getSnapshotIdAsString())) {
      throw new SnapshotException(
          "Expected chunk of snapshot "
              + metadata.getSnapshotIdAsString()
              + ", but got "
              + chunk.getSnapshotId());
    }

    // 总分片数一致性：记录首个分片的 totalCount，后续不一致即失败
    final Integer expected =
        expectedTotalCount.updateAndGet(
            current -> current == null ? chunk.getTotalCount() : current);
    if (expected != chunk.getTotalCount()) {
      throw new SnapshotException(
          "Expected total chunk count "
              + expected
              + ", but got "
              + chunk.getTotalCount()
              + " for chunk "
              + chunk.getChunkName()
              + " of snapshot "
              + chunk.getSnapshotId());
    }

    verifyChecksum(chunk);
    receivedChunkNames.add(chunk.getChunkName());
    writeBuffer.add(chunk);
    bufferedBytes += chunk.getContentLength();
    if (bufferedBytes >= maxWriteBatchBytes) {
      flushBatchLocked();
    }
  }

  @Override
  public synchronized void flush() {
    flushBatchLocked();
  }

  /** 整批写盘：按文件分组、文件内按偏移定位写入，每文件每批一次 force。 */
  private void flushBatchLocked() {
    if (writeBuffer.isEmpty()) {
      return;
    }
    final Map<String, List<SnapshotChunk>> chunksByFile = new LinkedHashMap<>();
    for (final SnapshotChunk chunk : writeBuffer) {
      final var location = ChunkLocation.of(chunk.getChunkName());
      chunksByFile.computeIfAbsent(location.fileName(), name -> new ArrayList<>()).add(chunk);
    }
    for (final var entry : chunksByFile.entrySet()) {
      final Path file = resolve(temporaryDirectory, entry.getKey());
      try {
        Files.createDirectories(file.getParent());
        try (final var channel =
            FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
                StandardOpenOption.CREATE)) {
          for (final SnapshotChunk chunk : entry.getValue()) {
            final var location = ChunkLocation.of(chunk.getChunkName());
            final var buffer = ByteBuffer.wrap(chunk.getContent());
            long written = 0;
            while (buffer.hasRemaining()) {
              channel.position(location.offset() + written);
              final int n = channel.write(buffer);
              if (n < 0) {
                throw new IOException("Failed to write chunk " + chunk.getChunkName());
              }
              written += n;
            }
          }
          // 批量落盘：每文件每批一次 force
          channel.force(true);
        }
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    writeBuffer.clear();
    bufferedBytes = 0;
  }

  @Override
  public void verifyComplete() {
    flush();
    // 分片数完整性：实际接收到的不同分片数必须等于声明的 totalCount
    final Integer expectedCount = expectedTotalCount.get();
    if (expectedCount == null || receivedChunkNames.size() != expectedCount) {
      throw new SnapshotException(
          "Received " + receivedChunkNames.size() + " chunks of snapshot " + metadata
              + ", but expected " + expectedCount);
    }
  }

  /** 校验分片内容 CRC32。 */
  static void verifyChecksum(final SnapshotChunk chunk) {
    final var crc = new CRC32();
    crc.update(chunk.getContent());
    if (crc.getValue() != chunk.getChecksum()) {
      throw new SnapshotException(
          "Checksum mismatch for chunk " + chunk.getChunkName() + " of snapshot "
              + chunk.getSnapshotId());
    }
  }

  /** 分片名解析结果：文件名 + 文件内字节偏移。 */
  record ChunkLocation(String fileName, long offset) {

    static ChunkLocation of(final String chunkName) {
      final int separator = chunkName.lastIndexOf('@');
      if (separator <= 0 || separator == chunkName.length() - 1) {
        throw new SnapshotException("Malformed chunk name " + chunkName);
      }
      try {
        return new ChunkLocation(
            chunkName.substring(0, separator),
            Long.parseLong(chunkName.substring(separator + 1)));
      } catch (final NumberFormatException e) {
        throw new SnapshotException("Malformed offset in chunk name " + chunkName, e);
      }
    }
  }

  /** 按 {@code 文件名@字节偏移} 定位并写入分片内容，写完 force 落盘。 */
  static void writeChunkFile(final Path directory, final SnapshotChunk chunk) {
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
    final Path file = resolve(directory, fileName);
    try {
      Files.createDirectories(file.getParent());
      try (final var channel =
          FileChannel.open(
              file,
              StandardOpenOption.WRITE,
              StandardOpenOption.READ,
              StandardOpenOption.CREATE)) {
        final var buffer = ByteBuffer.wrap(chunk.getContent());
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
  }

  /** 解析文件名并拒绝越出目录的路径。 */
  static Path resolve(final Path directory, final String fileName) {
    final Path resolved = directory.resolve(fileName).normalize();
    if (!resolved.startsWith(directory)) {
      throw new SnapshotException("Illegal file name in snapshot chunk: " + fileName);
    }
    return resolved;
  }

  @Override
  public String toString() {
    return "FilePersistableSnapshot{metadata=" + metadata + ", path=" + temporaryDirectory + '}';
  }
}
