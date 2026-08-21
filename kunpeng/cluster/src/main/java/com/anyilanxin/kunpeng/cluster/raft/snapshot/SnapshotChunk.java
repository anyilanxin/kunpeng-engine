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

/**
 * A single piece of a snapshot, transferred in an install request. A snapshot is split into blocks
 * of bounded size, each belonging to one of the snapshot's files. The checksum covers only this
 * chunk's content.
 */
public interface SnapshotChunk {

  /** Returns the type of the snapshot this chunk belongs to; defaults to a regular snapshot. */
  default SnapshotType getType() {
    return SnapshotType.REGULAR;
  }

  /** Returns the id of the snapshot this chunk belongs to ({@code index-term-nodeId}). */
  String getSnapshotId();

  /** Returns the total number of chunks of the snapshot, given the current chunk size. */
  int getTotalCount();

  /**
   * Returns the name of this chunk, which identifies the file and block within it, e.g. {@code
   * <fileName>:<blockNumber>}.
   */
  String getChunkName();

  /** Returns the CRC32 checksum of this chunk's content. */
  long getChecksum();

  /** Returns the content of this chunk. */
  byte[] getContent();

  /** Returns the position of this chunk's block within its file. */
  long getFileBlockPosition();

  /** Returns the total size of the file this chunk belongs to. */
  long getTotalFileSize();

  /** Returns the length of this chunk's content. */
  long getContentLength();
}
