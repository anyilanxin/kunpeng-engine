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
package com.anyilanxin.kunpeng.cluster.raft.journal.fs;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Pre-allocates segment files by writing zeroes through the given {@link FileChannel}.
 *
 * <p>This is a pure-Java replacement for {@code posix_fallocate}-based preallocation. It is
 * portable to every platform, at the cost of performing actual I/O for every allocated byte.
 */
public final class Preallocator {

  /** Size, in bytes, of the chunks written while filling the file. */
  private static final int CHUNK_SIZE = 4 * 1024 * 1024;

  /**
   * Extends the file behind {@code channel} to {@code size} bytes by writing zero-filled buffers.
   *
   * @param channel open channel to the file to allocate
   * @param fileDescriptor unused; kept for symmetry with the allocator SPI
   * @param size desired final size of the file, in bytes
   * @throws IOException if writing fails
   */
  public void allocate(
      final FileChannel channel, final FileDescriptor fileDescriptor, final long size)
      throws IOException {
    long position = channel.position();
    final ByteBuffer chunk = ByteBuffer.allocate((int) Math.min(CHUNK_SIZE, size));
    while (position < size) {
      final int writeLength = (int) Math.min(chunk.capacity(), size - position);
      chunk.clear().limit(writeLength);
      final int written = channel.write(chunk, position);
      if (written <= 0) {
        throw new IOException(
            "Failed to pre-allocate file: no progress at position " + position + " of " + size);
      }
      position += written;
    }
    channel.force(true);
  }
}
