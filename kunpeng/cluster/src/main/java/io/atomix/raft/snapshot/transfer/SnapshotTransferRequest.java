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

import io.atomix.raft.snapshot.SnapshotType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 跨分区快照拉取请求：声明目标快照类型、期望分片大小与续传位置。
 *
 * <p>线格式（大端）：{@code [类型序号 1B][期望分片大小 4B][是否有续传位 1B][续传分片名长度
 * 4B][续传分片名 UTF-8]}。
 */
public final class SnapshotTransferRequest {

  private final SnapshotType type;
  private final int preferredChunkSize;
  private final String afterChunkName;

  public SnapshotTransferRequest(
      final SnapshotType type, final int preferredChunkSize, final String afterChunkName) {
    this.type = type;
    this.preferredChunkSize = preferredChunkSize;
    this.afterChunkName = afterChunkName;
  }

  public SnapshotType getType() {
    return type;
  }

  public int getPreferredChunkSize() {
    return preferredChunkSize;
  }

  /** 续传位置：返回该分片名之后的下一个分片；为 null 表示从头开始。 */
  public String getAfterChunkName() {
    return afterChunkName;
  }

  /** 按线格式编码。 */
  public byte[] encode() {
    final byte[] afterBytes =
        afterChunkName == null
            ? new byte[0]
            : afterChunkName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(
            Byte.BYTES + Integer.BYTES + Byte.BYTES + Integer.BYTES + afterBytes.length);
    buffer.put((byte) type.ordinal());
    buffer.putInt(preferredChunkSize);
    buffer.put((byte) (afterChunkName == null ? 0 : 1));
    buffer.putInt(afterBytes.length);
    buffer.put(afterBytes);
    return buffer.array();
  }

  /** 从线格式解码，格式非法时抛 {@link IllegalArgumentException}。 */
  public static SnapshotTransferRequest decode(final byte[] frame) {
    final ByteBuffer buffer = ByteBuffer.wrap(frame);
    final var types = SnapshotType.values();
    final int typeOrdinal = buffer.get();
    if (typeOrdinal < 0 || typeOrdinal >= types.length) {
      throw new IllegalArgumentException("Unknown snapshot type ordinal: " + typeOrdinal);
    }
    final SnapshotType type = types[typeOrdinal];
    final int chunkSize = buffer.getInt();
    final boolean hasAfter = buffer.get() != 0;
    final int afterLength = buffer.getInt();
    if (afterLength < 0 || buffer.remaining() < afterLength) {
      throw new IllegalArgumentException("Malformed resume chunk name length: " + afterLength);
    }
    final byte[] afterBytes = new byte[afterLength];
    buffer.get(afterBytes);
    final String afterChunkName = hasAfter ? new String(afterBytes, StandardCharsets.UTF_8) : null;
    return new SnapshotTransferRequest(type, chunkSize, afterChunkName);
  }
}
