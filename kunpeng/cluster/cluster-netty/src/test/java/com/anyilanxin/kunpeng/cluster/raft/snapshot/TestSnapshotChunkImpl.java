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

import java.nio.ByteBuffer;

/**
 * 测试用的快照分片桩实现，除名称与内容字段外其余属性均取默认值。
 */
class TestSnapshotChunkImpl implements SnapshotChunk {

  private final String chunkName;
  private final byte[] chunkContent;

  TestSnapshotChunkImpl(final String chunkName, final byte[] content) {
    this.chunkName = chunkName;
    chunkContent = content;
  }

  @Override
  public String getChunkName() {
    return chunkName;
  }

  @Override
  public long getSnapshotChecksum() {
    return 0;
  }

  @Override
  public long getTotalLength() {
    return chunkContent.length;
  }

  @Override
  public long getChecksum() {
    return 0;
  }

  @Override
  public ByteBuffer getContent() {
    return ByteBuffer.wrap(chunkContent);
  }

  @Override
  public int getLength() {
    return chunkContent.length;
  }

  @Override
  public long getOffset() {
    return 0;
  }
}
