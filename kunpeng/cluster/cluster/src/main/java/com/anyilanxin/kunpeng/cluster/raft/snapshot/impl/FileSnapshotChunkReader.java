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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SimpleFileVerificationChecksums;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.CRC32;

/**
 * 文件镜像分片读取器：镜像目录内每个文件（含 snapshot.metadata）按最大分片尺寸切块， 每块一个分片；文件清单与整文件 size/CRC32 取自 .sfc 校验集，不再额外
 * stat。 分片 id 为 chunkName（{@code 文件名@字节偏移}）的 UTF-8 字节，与分片尺寸无关以支持续传。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class FileSnapshotChunkReader implements SnapshotChunkReader {

  static final int DEFAULT_CHUNK_SIZE = 1024 * 1024;

  private final Path directory;
  private final Map<String, SnapshotFileInfo> fileInfos;
  private final List<String> fileNames;
  private int maximumChunkSize = DEFAULT_CHUNK_SIZE;
  private List<ChunkDescriptor> chunks;
  private int position;

  public FileSnapshotChunkReader(
      final Path directory, final SimpleFileVerificationChecksums checksums) {
    this.directory = directory;
    fileInfos = checksums.getFileInfos();
    fileNames = fileInfos.keySet().stream().sorted().toList();
    rebuild();
  }

  private void rebuild() {
    final List<ChunkDescriptor> descriptors = new ArrayList<>();
    for (final String fileName : fileNames) {
      final long fileSize = fileInfos.get(fileName).size();
      for (long offset = 0; offset < fileSize; offset += maximumChunkSize) {
        descriptors.add(new ChunkDescriptor(fileName, offset));
      }
      if (fileSize == 0) {
        descriptors.add(new ChunkDescriptor(fileName, 0));
      }
    }
    chunks = List.copyOf(descriptors);
    position = 0;
  }

  @Override
  public boolean hasNext() {
    return position < chunks.size();
  }

  @Override
  public ByteBuffer nextId() {
    if (!hasNext()) {
      return null;
    }
    return ByteBuffer.wrap(chunks.get(position).chunkName().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public SnapshotChunk next() {
    if (!hasNext()) {
      throw new NoSuchElementException("Snapshot chunk reader is exhausted");
    }
    final ChunkDescriptor descriptor = chunks.get(position);
    position++;

    final SnapshotFileInfo fileInfo = fileInfos.get(descriptor.fileName());
    final byte[] content =
        readBlock(directory.resolve(descriptor.fileName()), descriptor.offset(), maximumChunkSize);
    final var crc = new CRC32();
    crc.update(content);

    return new SnapshotChunkImpl(
        descriptor.chunkName(),
        fileInfo.checksum(),
        fileInfo.size(),
        crc.getValue(),
        ByteBuffer.wrap(content),
        descriptor.offset());
  }

  @Override
  public void seek(final ByteBuffer chunkId) {
    final ByteBuffer duplicate = chunkId.slice();
    final byte[] bytes = new byte[duplicate.remaining()];
    duplicate.get(bytes);
    final String chunkName = new String(bytes, StandardCharsets.UTF_8);
    for (int i = 0; i < chunks.size(); i++) {
      if (chunks.get(i).chunkName().equals(chunkName)) {
        position = i;
        return;
      }
    }
    throw new UncheckedIOException(
        new IOException("Expected to seek to chunk " + chunkName + ", but it does not exist"));
  }

  @Override
  public void reset() {
    position = 0;
  }

  @Override
  public void setMaximumChunkSize(final int maximumChunkSize) {
    if (maximumChunkSize <= 0 || maximumChunkSize == this.maximumChunkSize) {
      return;
    }
    this.maximumChunkSize = maximumChunkSize;
    rebuild();
  }

  @Override
  public void close() {
    // 无状态读取器，无需释放资源
  }

  private static byte[] readBlock(final Path file, final long offset, final int limit) {
    try (final var channel = Files.newByteChannel(file)) {
      final int length = (int) Math.min(channel.size() - offset, limit);
      if (length < 0) {
        throw new UncheckedIOException(
            new IOException("Block offset " + offset + " is beyond the end of " + file));
      }
      final var buffer = ByteBuffer.allocate(length);
      int read = 0;
      while (read < length) {
        channel.position(offset + read);
        final int n = channel.read(buffer);
        if (n < 0) {
          throw new UncheckedIOException(new IOException("Unexpected end of file " + file));
        }
        read += n;
      }
      return buffer.array();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 分片描述：chunkName 以 {@code 文件名@字节偏移} 编码，与分片尺寸无关以支持续传。 */
  private record ChunkDescriptor(String fileName, long offset) {

    private String chunkName() {
      return fileName + "@" + offset;
    }
  }
}
