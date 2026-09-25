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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.nio.ByteBuffer;

/**
 * 快照分片：传输的最小内容单元，一个分片属于快照内某个文件的一个字节区间。
 *
 * <p>checksum 仅覆盖本分片内容（CRC32）；{@code chunkName} 以 {@code 文件名@字节偏移} 编码，
 * 与分片尺寸无关以支持断点续传。分片内容是只读字节视图，可能直接内存映射自源文件，消费方不应假设 其底层是堆内数组。
 */
public interface SnapshotChunk {
  /** 分片名：{@code 文件名@字节偏移}。 */
  String getChunkName();

  /** 总文件内容的 CRC32 校验和。 */
  long getSnapshotChecksum();

  /** 分片总大小 */
  long getTotalLength();

  /** 本分片内容的 CRC32 校验和。 */
  long getChecksum();

  /** 分片内容（只读视图，读取位置从 0 开始、剩余长度即分片长度）。 */
  ByteBuffer getContent();

  /** 分片内容长度（字节）。 */
  int getLength();

  /** 偏移量 */
  long getOffset();
}
