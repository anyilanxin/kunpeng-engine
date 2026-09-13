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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.receive;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.FilePersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件版接收 pending 镜像：分片逐批写入临时目录，每个文件字节收齐时回读校验整文件 CRC32， 校验信息随写累积；{@link #persist()} 时读取随分片传入的 {@code
 * snapshot.metadata}， 原子 move 到正式目录后由公共存储生成 .sfc 完成提交。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
final class DefaultReceivedSnapshot implements ReceivedSnapshot {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReceivedSnapshot.class);
  private static final int VERIFY_BUFFER_SIZE = 1024 * 1024;

  private final SnapshotId snapshotId;
  private final Path directory;
  private final DefaultFileSnapshotStore store;
  private final ConcurrencyControl actor;
  private final CRC32 crc = new CRC32();
  // 已完成整文件校验的文件 → 校验信息（persist 时作为 .sfc 的校验集）
  private final Map<String, SnapshotFileInfo> fileInfos = new LinkedHashMap<>();

  // 当前正在写入的文件状态
  private FileChannel channel;
  private String currentFileName;
  private long expectedFileSize;
  private long expectedFileChecksum;
  private long receivedBytes;
  // abort/persist 后为 true
  private boolean closed;
  private PersistedSnapshot snapshot;

  DefaultReceivedSnapshot(
      final SnapshotId snapshotId,
      final Path directory,
      final DefaultFileSnapshotStore store,
      final ConcurrencyControl actor) {
    this.snapshotId = snapshotId;
    this.directory = directory;
    this.store = store;
    this.actor = actor;
  }

  @Override
  public ActorFuture<Void> write(final SnapshotChunkBatch batch) {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          try {
            for (final SnapshotChunk chunk : batch.chunks()) {
              writeChunk(chunk);
            }
            future.complete(null);
          } catch (final Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  /** 写入一个分片：校验内容 CRC32 后按 {@code 文件名@字节偏移} 定位写入； 文件字节收齐时回读校验整文件 CRC32。 */
  private void writeChunk(final SnapshotChunk chunk) {
    ensureOpen();
    verifyChunkChecksum(chunk);

    final String fileName = fileNameOf(chunk.getChunkName());
    if (!fileName.equals(currentFileName)) {
      closeCurrentFile();
      openFile(fileName, chunk);
    }

    final ByteBuffer buffer = chunk.getContent();
    final int length = buffer.remaining();
    if (receivedBytes + length > expectedFileSize) {
      throw new SnapshotException("Received more bytes than declared for file " + currentFileName);
    }

    final long offset = chunk.getOffset();
    try {
      int written = 0;
      while (buffer.hasRemaining()) {
        written += channel.write(buffer, offset + written);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write chunk " + chunk.getChunkName(), e);
    }

    receivedBytes += length;
    if (receivedBytes == expectedFileSize) {
      verifyWholeFile();
      fileInfos.put(currentFileName, new SnapshotFileInfo(expectedFileChecksum, expectedFileSize));
    }
  }

  /** 校验分片内容 CRC32 与分片携带的校验和一致；在副本视图上计算，不改动原缓冲区位置。 */
  private void verifyChunkChecksum(final SnapshotChunk chunk) {
    crc.reset();
    crc.update(chunk.getContent().asReadOnlyBuffer());
    if (crc.getValue() != chunk.getChecksum()) {
      throw new SnapshotException("Checksum mismatch for chunk " + chunk.getChunkName());
    }
  }

  /** 从 {@code 文件名@字节偏移} 形式的分片名中解析出文件名。 */
  private static String fileNameOf(final String chunkName) {
    final int separator = chunkName.lastIndexOf('@');
    if (separator <= 0 || separator == chunkName.length() - 1) {
      throw new SnapshotException("Malformed chunk name " + chunkName);
    }
    return chunkName.substring(0, separator);
  }

  /** 打开目标文件并记录文件级期望，随后把文件预撑到最终长度；文件名不允许跳出写入目录。 */
  private void openFile(final String fileName, final SnapshotChunk firstChunk) {
    final Path file = directory.resolve(fileName).normalize();
    if (!file.startsWith(directory)) {
      throw new SnapshotException("Chunk file escapes target directory: " + fileName);
    }
    try {
      Files.createDirectories(file.getParent());
      channel =
          FileChannel.open(
              file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      currentFileName = fileName;
      expectedFileSize = firstChunk.getTotalLength();
      expectedFileChecksum = firstChunk.getSnapshotChecksum();
      receivedBytes = 0;
      preAllocate();
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to open file " + file, e);
    }
  }

  /** 按已知的文件总大小把文件预撑到最终长度（在末尾写一个字节，稀疏分配）。 */
  private void preAllocate() throws IOException {
    if (expectedFileSize > 0) {
      channel.write(ByteBuffer.wrap(new byte[1]), expectedFileSize - 1);
    }
  }

  /** 文件字节收齐后回读全部内容，校验整文件 CRC32 与分片携带的 snapshotChecksum 一致。 */
  private void verifyWholeFile() {
    try {
      channel.force(false);
      crc.reset();
      final var buffer =
          ByteBuffer.allocate((int) Math.clamp(expectedFileSize, 1, VERIFY_BUFFER_SIZE));
      for (long offset = 0; offset < expectedFileSize; offset += VERIFY_BUFFER_SIZE) {
        buffer.clear();
        buffer.limit((int) Math.min(VERIFY_BUFFER_SIZE, expectedFileSize - offset));
        int read = 0;
        while (buffer.hasRemaining()) {
          final int n = channel.read(buffer, offset + read);
          if (n < 0) {
            throw new IOException("Unexpected end of file " + currentFileName);
          }
          read += n;
        }
        buffer.flip();
        crc.update(buffer);
      }
      if (crc.getValue() != expectedFileChecksum) {
        throw new SnapshotException("Checksum mismatch for file " + currentFileName);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to verify file " + currentFileName, e);
    }
  }

  /**
   * 关闭当前文件通道并重置文件级状态。
   *
   * @throws SnapshotException 文件未收齐（存在分片丢失或传输中断）时抛出
   */
  private void closeCurrentFile() {
    if (channel == null) {
      return;
    }
    if (receivedBytes != expectedFileSize) {
      throw new SnapshotException(
          "File "
              + currentFileName
              + " is incomplete: received "
              + receivedBytes
              + " of "
              + expectedFileSize
              + " bytes");
    }
    closeChannelQuietly();
  }

  /** 关闭当前文件通道（abort 路径用，不做完整性检查）。 */
  private void closeChannelQuietly() {
    if (channel != null) {
      try {
        channel.force(true);
        channel.close();
      } catch (final IOException e) {
        LOGGER.debug("Failed to close file channel of {}", currentFileName, e);
      }
      channel = null;
      currentFileName = null;
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new SnapshotException("Received snapshot " + snapshotId + " is already closed");
    }
  }

  @Override
  public ActorFuture<PersistedSnapshot> persist() {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    actor.run(() -> persistInternal(future));
    return future;
  }

  private void persistInternal(final CompletableActorFuture<PersistedSnapshot> future) {
    if (snapshot != null) {
      future.complete(snapshot);
      return;
    }
    try {
      closed = true;
      // 收尾：最后一个文件未收齐会在这里抛出
      closeCurrentFile();
      // 元数据文件随分片一并传输，缺失/不可解析即接收内容不完整
      final var metadata = SnapshotMetadata.readFrom(directory, snapshotId);
      final Path destination = store.getPath().resolve(snapshotId.asString());
      if (Files.exists(destination)) {
        // 启动不清理磁盘残留（如提交中断缺 .sfc），这里覆盖以允许同 id 重新提交；走到此处时该 id 未登记，必为残留
        LOGGER.warn("Overwriting unregistered snapshot residue at {}", destination);
        FilePersistedSnapshot.deleteRecursively(destination);
      }
      Files.move(directory, destination, StandardCopyOption.ATOMIC_MOVE);
      snapshot = store.persistNewSnapshot(destination, snapshotId, fileInfos, metadata);
      future.complete(snapshot);
    } catch (final AtomicMoveNotSupportedException e) {
      abortInternal();
      future.completeExceptionally(
          new SnapshotException("Atomic move is not supported by the file system", e));
    } catch (final Exception e) {
      abortInternal();
      future.completeExceptionally(e);
    } finally {
      store.removePending(this);
    }
  }

  @Override
  public ActorFuture<Void> abort() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          abortInternal();
          future.complete(null);
        });
    return future;
  }

  private void abortInternal() {
    try {
      closed = true;
      snapshot = null;
      LOGGER.debug("Aborting received snapshot {}", this);
      closeChannelQuietly();
      FilePersistedSnapshot.deleteRecursively(directory);
    } finally {
      store.removePending(this);
    }
  }

  @Override
  public SnapshotId snapshotId() {
    return snapshotId;
  }

  @Override
  public Path getPath() {
    return directory;
  }

  @Override
  public String toString() {
    return "DefaultReceivedSnapshot{directory=" + directory + ", snapshotId=" + snapshotId + '}';
  }
}
