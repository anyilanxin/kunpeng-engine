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
package io.atomix.raft.journal.file;

import io.atomix.raft.journal.fs.Preallocator;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.FileChannel;

/** Strategy for pre-allocating disk space for new segment files. */
@FunctionalInterface
public interface SegmentAllocator {

  /**
   * Pre-allocates {@code segmentSize} bytes of disk space for the file behind {@code channel}.
   *
   * @param channel an open channel to the file to pre-allocate
   * @param fileDescriptor the file descriptor of the opened file
   * @param segmentSize the desired size of the segment on disk, in bytes
   * @throws IOException if pre-allocation fails; no guarantees are made about the on-disk state
   */
  void allocate(FileChannel channel, FileDescriptor fileDescriptor, final long segmentSize)
      throws IOException;

  /**
   * Returns the default allocator, which pre-allocates segment files by zero-filling them.
   *
   * @return the default allocator
   */
  static SegmentAllocator defaultAllocator() {
    return fill();
  }

   /** Returns an allocator which does nothing, i.e. does not allocate disk space. */
  static SegmentAllocator noop() {
    return (channel, fileDescriptor, segmentSize) -> {};
  }

  /**
   * Returns an allocator which grows the file by writing zero-filled chunks through the channel.
   */
  static SegmentAllocator fill() {
    return new Preallocator()::allocate;
  }
}
