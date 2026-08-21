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

/** 测试用的快照分片桩实现，除身份与内容字段外其余属性均取默认值。 */
class TestSnapshotChunkImpl implements SnapshotChunk {

  private final String snapshotId;
  private final String chunkName;
  private final byte[] chunkContent;
  private final int chunksTotal;

  TestSnapshotChunkImpl(
      final String snapshotId, final String chunkName, final byte[] content, final int totalCount) {
    this.snapshotId = snapshotId;
    this.chunkName = chunkName;
    this.chunkContent = content;
    this.chunksTotal = totalCount;
  }

  @Override
  public SnapshotType getType() {
    return SnapshotType.REGULAR;
  }

  @Override
  public String getSnapshotId() {
    return snapshotId;
  }

  @Override
  public String getChunkName() {
    return chunkName;
  }

  @Override
  public byte[] getContent() {
    return chunkContent;
  }

  @Override
  public int getTotalCount() {
    return chunksTotal;
  }

  @Override
  public long getContentLength() {
    return chunkContent.length;
  }

  /** 测试桩不参与校验和计算，恒返回 0。 */
  @Override
  public long getChecksum() {
    return 0;
  }

  /** 测试桩不关心分片落盘位置，恒返回 0。 */
  @Override
  public long getFileBlockPosition() {
    return 0;
  }

  /** 测试桩不感知文件大小，恒返回 0。 */
  @Override
  public long getTotalFileSize() {
    return 0;
  }
}
