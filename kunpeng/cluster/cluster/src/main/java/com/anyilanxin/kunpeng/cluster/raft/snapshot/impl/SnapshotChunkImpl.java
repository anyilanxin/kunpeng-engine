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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import java.nio.ByteBuffer;

/** {@link SnapshotChunk} 的默认实现：不可变分片。 */
public final class SnapshotChunkImpl implements SnapshotChunk {

  private final String chunkName;
  private final long snapshotChecksum;
  private final long totalLength;
  private final long checksum;
  private final ByteBuffer content;
  private final long offset;

  public SnapshotChunkImpl(
      final String chunkName,
      final long snapshotChecksum,
      final long totalLength,
      final long checksum,
      final ByteBuffer content,
      final long offset) {
    this.chunkName = chunkName;
    this.snapshotChecksum = snapshotChecksum;
    this.totalLength = totalLength;
    this.checksum = checksum;
    this.content = content;
    this.offset = offset;
  }

  @Override
  public String getChunkName() {
    return chunkName;
  }

  @Override
  public long getSnapshotChecksum() {
    return snapshotChecksum;
  }

  @Override
  public long getTotalLength() {
    return totalLength;
  }

  @Override
  public long getChecksum() {
    return checksum;
  }

  /** 返回内容的只读视图，避免消费方改动内部缓冲区的读写位置。 */
  @Override
  public ByteBuffer getContent() {
    return content.asReadOnlyBuffer();
  }

  @Override
  public int getLength() {
    return content.remaining();
  }

  @Override
  public long getOffset() {
    return offset;
  }
}
