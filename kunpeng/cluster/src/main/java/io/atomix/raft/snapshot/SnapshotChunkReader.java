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
package io.atomix.raft.snapshot;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/**
 * A closeable iterator over the chunks of a snapshot, as created by {@link
 * PersistedSnapshot#newChunkReader()}.
 *
 * <p>The reader peeks at the id of the next chunk via {@link #nextId()} without consuming it;
 * {@link #next()} returns the chunk and advances the reader. If reading fails because the snapshot
 * was deleted or corrupted, an {@link UncheckedIOException} is thrown.
 */
public interface SnapshotChunkReader extends AutoCloseable {

  /** Returns whether another chunk is available. */
  boolean hasNext();

  /** Returns the id of the next chunk without consuming it, or {@code null} if exhausted. */
  ByteBuffer nextId();

  /**
   * Returns the next chunk and advances the reader.
   *
   * @throws java.util.NoSuchElementException if exhausted
   */
  SnapshotChunk next();

  /**
   * Positions the reader at the chunk with the given chunk id, so that the next call to {@link
   * #next()} returns it.
   *
   * @param chunkId the id of the chunk to seek to
   */
  void seek(ByteBuffer chunkId);

  /** Resets the reader to the first chunk. */
  void reset();

  /**
   * Sets the maximum content size of subsequent chunks. Applies to all chunks returned after this
   * call.
   *
   * @param maximumChunkSize the maximum chunk size in bytes
   */
  void setMaximumChunkSize(int maximumChunkSize);

  /** Closes the reader and releases its resources; safe to call multiple times. */
  @Override
  void close();
}
