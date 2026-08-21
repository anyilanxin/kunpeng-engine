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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 合并快照推送请求：源分区 leader 向目标分区 leader 主动推送一批分片。
 *
 * <p>线格式（大端）：{@code [快照 id 长度 4B][快照 id UTF-8][是否还有后续批 1B]
 * [末片名长度 4B][末片名 UTF-8][分片数 4B][分片帧长度 4B][分片帧字节]...}；
 * 目标侧首个请求创建接收式 pending，末批（是否还有后续=0）触发校验与提交。
 */
public final class SnapshotPushRequest {

  private final String snapshotId;
  private final boolean hasMore;
  private final String lastChunkName;
  private final List<byte[]> chunkFrames;

  private SnapshotPushRequest(
      final String snapshotId,
      final boolean hasMore,
      final String lastChunkName,
      final List<byte[]> chunkFrames) {
    this.snapshotId = snapshotId;
    this.hasMore = hasMore;
    this.lastChunkName = lastChunkName;
    this.chunkFrames = List.copyOf(chunkFrames);
  }

  /** 构造一批推送请求。 */
  public static SnapshotPushRequest of(
      final String snapshotId,
      final String lastChunkName,
      final List<byte[]> chunkFrames,
      final boolean hasMore) {
    return new SnapshotPushRequest(snapshotId, hasMore, lastChunkName, chunkFrames);
  }

  public String getSnapshotId() {
    return snapshotId;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public String getLastChunkName() {
    return lastChunkName;
  }

  public List<byte[]> getChunkFrames() {
    return chunkFrames;
  }

  /** 按线格式编码。 */
  public byte[] encode() {
    final byte[] idBytes = snapshotId.getBytes(StandardCharsets.UTF_8);
    final byte[] lastBytes =
        lastChunkName == null ? new byte[0] : lastChunkName.getBytes(StandardCharsets.UTF_8);
    final var frames = chunkFrames.toArray(new byte[0][]);
    int framesLength = 0;
    for (final byte[] frame : frames) {
      framesLength += Integer.BYTES + frame.length;
    }
    final ByteBuffer buffer =
        ByteBuffer.allocate(
            Integer.BYTES + idBytes.length
                + Byte.BYTES
                + Integer.BYTES + lastBytes.length
                + Integer.BYTES + framesLength);
    buffer.putInt(idBytes.length);
    buffer.put(idBytes);
    buffer.put((byte) (hasMore ? 1 : 0));
    buffer.putInt(lastBytes.length);
    buffer.put(lastBytes);
    buffer.putInt(frames.length);
    for (final byte[] frame : frames) {
      buffer.putInt(frame.length);
      buffer.put(frame);
    }
    return buffer.array();
  }

  /** 从线格式解码。 */
  public static SnapshotPushRequest decode(final byte[] frame) {
    final ByteBuffer buffer = ByteBuffer.wrap(frame);
    final String snapshotId = readUtf8(buffer);
    final boolean hasMore = buffer.get() != 0;
    final int lastLength = buffer.getInt();
    requireRemaining(buffer, lastLength);
    final byte[] lastBytes = new byte[lastLength];
    buffer.get(lastBytes);
    final String lastChunkName =
        lastLength == 0 ? null : new String(lastBytes, StandardCharsets.UTF_8);
    final int count = buffer.getInt();
    if (count < 0) {
      throw new IllegalArgumentException("Malformed chunk frame count: " + count);
    }
    final byte[][] frames = new byte[count][];
    for (int i = 0; i < count; i++) {
      final int length = buffer.getInt();
      requireRemaining(buffer, length);
      frames[i] = new byte[length];
      buffer.get(frames[i]);
    }
    return new SnapshotPushRequest(snapshotId, hasMore, lastChunkName, List.of(frames));
  }

  private static String readUtf8(final ByteBuffer buffer) {
    final int length = buffer.getInt();
    requireRemaining(buffer, length);
    final byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static void requireRemaining(final ByteBuffer buffer, final int length) {
    if (length < 0 || buffer.remaining() < length) {
      throw new IllegalArgumentException("Malformed push request field length: " + length);
    }
  }
}
