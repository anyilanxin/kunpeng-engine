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
package io.atomix.raft.snapshot.transfer;

import java.nio.ByteBuffer;

/**
 * 跨分区快照拉取应答：携带一个分片帧（{@link io.atomix.raft.snapshot.impl.SnapshotChunkImpl}
 * 的自描述二进制）与"是否还有后续分片"标志。
 *
 * <p>线格式（大端）：{@code [是否还有后续 1B][分片帧长度 4B][分片帧字节]}；结束时长度为 0。
 */
public final class SnapshotTransferResponse {

  private final boolean hasMore;
  private final byte[] chunkFrame;

  private SnapshotTransferResponse(final boolean hasMore, final byte[] chunkFrame) {
    this.hasMore = hasMore;
    this.chunkFrame = chunkFrame;
  }

  /** 携带一个分片的应答。 */
  public static SnapshotTransferResponse of(final byte[] chunkFrame, final boolean hasMore) {
    return new SnapshotTransferResponse(hasMore, chunkFrame);
  }

  /** 传输结束的应答。 */
  public static SnapshotTransferResponse end() {
    return new SnapshotTransferResponse(false, new byte[0]);
  }

  public boolean hasMore() {
    return hasMore;
  }

  /** 分片帧字节；结束应答时长度为 0。 */
  public byte[] getChunkFrame() {
    return chunkFrame;
  }

  /** 按线格式编码。 */
  public byte[] encode() {
    final ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + chunkFrame.length);
    buffer.put((byte) (hasMore ? 1 : 0));
    buffer.putInt(chunkFrame.length);
    buffer.put(chunkFrame);
    return buffer.array();
  }

  /** 从线格式解码。 */
  public static SnapshotTransferResponse decode(final byte[] frame) {
    final ByteBuffer buffer = ByteBuffer.wrap(frame);
    final boolean hasMore = buffer.get() != 0;
    final int length = buffer.getInt();
    if (length < 0 || buffer.remaining() < length) {
      throw new IllegalArgumentException("Malformed chunk frame length: " + length);
    }
    final byte[] chunkFrame = new byte[length];
    buffer.get(chunkFrame);
    return new SnapshotTransferResponse(hasMore, chunkFrame);
  }
}
