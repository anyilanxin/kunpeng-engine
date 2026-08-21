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
 * 快照分片：传输的最小内容单元，一个分片属于快照内某个文件的一个字节区间。
 *
 * <p>checksum 仅覆盖本分片内容（CRC32）；{@code chunkName} 以 {@code 文件名@字节偏移} 编码，
 * 与分片尺寸无关以支持断点续传。
 */
public interface SnapshotChunk {

  /** 该分片所属快照的类型，缺省为常规快照。 */
  default SnapshotType getType() {
    return SnapshotType.REGULAR;
  }

  /** 该分片所属快照的 id（{@code index-term-hex(nodeId)}）。 */
  String getSnapshotId();

  /** 该快照的总分片数（按当前分片尺寸）。 */
  int getTotalCount();

  /** 分片名：{@code 文件名@字节偏移}。 */
  String getChunkName();

  /** 本分片内容的 CRC32 校验和。 */
  long getChecksum();

  /** 本分片内容。 */
  byte[] getContent();

  /** 分片内容的只读 ByteBuffer 视图。 */
  default ByteBuffer getContentBuffer() {
    return ByteBuffer.wrap(getContent()).asReadOnlyBuffer();
  }

  /** 分片在所属文件内的字节偏移。 */
  long getFileBlockPosition();

  /** 分片所属文件的总大小。 */
  long getTotalFileSize();

  /** 分片内容长度（字节）。 */
  long getContentLength();
}
