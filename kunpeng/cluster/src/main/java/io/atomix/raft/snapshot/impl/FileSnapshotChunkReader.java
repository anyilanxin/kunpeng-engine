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
package io.atomix.raft.snapshot.impl;

import io.atomix.raft.snapshot.SnapshotChunk;
import io.atomix.raft.snapshot.SnapshotChunkReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.CRC32;

/**
 * Reads a persisted snapshot as a sequence of chunks. Every file of the snapshot (manifest
 * included) is split into blocks of at most the maximum chunk size; each block becomes one chunk.
 */
final class FileSnapshotChunkReader implements SnapshotChunkReader {

  static final int DEFAULT_CHUNK_SIZE = 1024 * 1024;

  private final FilePersistedSnapshot snapshot;
  private final Path directory;
  private final List<String> fileNames;
  private int maximumChunkSize = DEFAULT_CHUNK_SIZE;
  private List<ChunkDescriptor> chunks;
  private int position;

  FileSnapshotChunkReader(final FilePersistedSnapshot snapshot) {
    this.snapshot = snapshot;
    this.directory = snapshot.getPath();
    // The manifest is transferred along with the content files so the receiver can verify it.
    final List<String> names = new ArrayList<>(snapshot.manifest().fileNames());
    names.add(SnapshotManifest.MANIFEST_FILE_NAME);
    names.sort(String::compareTo);
    this.fileNames = List.copyOf(names);
    rebuild();
  }

  private void rebuild() {
    final List<ChunkDescriptor> descriptors = new ArrayList<>();
    try {
      for (final String fileName : fileNames) {
        final long fileSize = Files.size(directory.resolve(fileName));
        for (long offset = 0; offset < fileSize; offset += maximumChunkSize) {
          descriptors.add(new ChunkDescriptor(fileName, offset));
        }
        if (fileSize == 0) {
          descriptors.add(new ChunkDescriptor(fileName, 0));
        }
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
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
    return chunkId(chunks.get(position));
  }

  @Override
  public SnapshotChunk next() {
    if (!hasNext()) {
      throw new NoSuchElementException("Snapshot chunk reader is exhausted");
    }
    final ChunkDescriptor descriptor = chunks.get(position);
    position++;

    final Path file = directory.resolve(descriptor.fileName());
    final byte[] content = readBlock(file, descriptor.offset(), maximumChunkSize);
    final var crc = new CRC32();
    crc.update(content);

    return new SnapshotChunkImpl(
        snapshot.getId(),
        chunks.size(),
        descriptor.chunkName(),
        crc.getValue(),
        content,
        descriptor.offset(),
        sizeOf(file),
        snapshot.getType());
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
    // stateless reader; nothing to release
  }

  private static ByteBuffer chunkId(final ChunkDescriptor descriptor) {
    return ByteBuffer.wrap(descriptor.chunkName().getBytes(StandardCharsets.UTF_8));
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

  private static long sizeOf(final Path file) {
    try {
      return Files.size(file);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 分片描述：chunkName 以"文件名@字节偏移"编码，与分片尺寸无关以支持续传。 */
  private record ChunkDescriptor(String fileName, long offset) {

    private String chunkName() {
      return fileName + "@" + offset;
    }
  }
}
