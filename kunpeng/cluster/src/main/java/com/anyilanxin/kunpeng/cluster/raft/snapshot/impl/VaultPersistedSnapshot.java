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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/** {@link PersistedSnapshot} 的 v2 实现（包装 {@link ArchivedSnapshot}） */
final class VaultPersistedSnapshot implements PersistedSnapshot {

  private final ArchivedSnapshot archived;

  VaultPersistedSnapshot(final ArchivedSnapshot archived) {
    this.archived = archived;
  }

  @Override
  public long getIndex() {
    return archived.ref().index();
  }

  @Override
  public long getTerm() {
    return archived.ref().term();
  }

  @Override
  public String getId() {
    return archived.ref().toString();
  }

  @Override
  public int version() {
    return 1;
  }

  @Override
  public long getProcessedPosition() {
    return archived.meta().processedPosition();
  }

  @Override
  public long getExportedPosition() {
    return archived.meta().exportedPosition();
  }

  @Override
  public long getChecksum() {
    return archived.manifest().combined();
  }

  @Override
  public Path getPath() {
    return archived.directory();
  }

  @Override
  public SnapshotChunkReader newChunkReader() {
    final int maxBlockBytes = 1024 * 1024;
    return new V2ChunkReaderAdapter(archived.blockReader(maxBlockBytes));
  }

  ArchivedSnapshot archived() {
    return archived;
  }

  /** v2 块读取器到 v1 接口的适配器 */
  private static final class V2ChunkReaderAdapter implements SnapshotChunkReader {

    private final BlockStreamReader reader;
    private SnapshotBlock current;

    V2ChunkReaderAdapter(final BlockStreamReader reader) {
      this.reader = reader;
    }

    @Override
    public boolean hasNext() {
      return reader.hasNext();
    }

    @Override
    public SnapshotChunk next() {
      if (!hasNext()) {
        throw new java.util.NoSuchElementException();
      }
      try {
        current = reader.next();
        return new V2ChunkAdapter(current);
      } catch (final SnapshotStoreException e) {
        throw new UncheckedIOException(new java.io.IOException(e.getMessage(), e));
      }
    }

    @Override
    public ByteBuffer nextId() {
      if (!hasNext()) {
        return null;
      }
      return ByteBuffer.wrap(reader.cursor().getBytes());
    }

    @Override
    public void seek(final ByteBuffer chunkId) {
      final String blockName = new String(chunkId.array(), 0, chunkId.remaining());
      reader.seek(blockName);
    }
  }

  /** v2 块到 v1 快照块的适配器 */
  private record V2ChunkAdapter(
      SnapshotBlock block)
      implements SnapshotChunk {

    @Override
    public String getSnapshotId() {
      return block.snapshotId();
    }

    @Override
    public int getTotalCount() {
      return block.blockCount();
    }

    @Override
    public String getChunkName() {
      return block.blockName();
    }

    @Override
    public long getChecksum() {
      return block.crc();
    }

    @Override
    public long getSnapshotChecksum() {
      return 0;
    }

    @Override
    public byte[] getContent() {
      return block.payload();
    }
  }
}
