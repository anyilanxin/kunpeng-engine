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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 快照分片的线格式实现，用于在安装快照请求中承载单个分片。
 *
 * <p>采用大端字节序的自描述二进制布局：魔数（"KSNP"）、版本字节、快照类型字节，随后依次为
 * 带长度前缀的 UTF-8 快照 ID 与分片名、分片总数、CRC32 校验值、分片在文件中的偏移、文件总
 * 大小，最后是带长度前缀的分片内容。
 */
public final class SnapshotChunkImpl implements SnapshotChunk {

  /** 魔数 "KSNP"，用于识别分片帧。 */
  private static final int MAGIC_NUMBER = 0x4B534E50;

  /** 当前线格式版本号（v2 起在版本字节后携带快照类型字节）。 */
  private static final byte WIRE_VERSION = 2;

  /** 除两个变长字符串和内容外的定长部分字节数。 */
  private static final int FIXED_PART_BYTES =
      4 * Integer.BYTES + 3 * Long.BYTES + 2 * Byte.BYTES;

  private long checksum;
  private long totalFileSize;
  private long fileBlockPosition;
  private byte[] content = new byte[0];
  private int totalCount;
  private String chunkName = "";
  private String snapshotId = "";
  private SnapshotType type = SnapshotType.REGULAR;

  /** 供反序列化复用的无参构造器。 */
  public SnapshotChunkImpl() {}

  /** 基于已有分片对象拷贝构造。 */
  public SnapshotChunkImpl(final SnapshotChunk chunk) {
    this(
        chunk.getSnapshotId(),
        chunk.getTotalCount(),
        chunk.getChunkName(),
        chunk.getChecksum(),
        chunk.getContent(),
        chunk.getFileBlockPosition(),
        chunk.getTotalFileSize(),
        chunk.getType());
  }

  /** 以完整分片属性直接构造。 */
  public SnapshotChunkImpl(
      final String snapshotId,
      final int totalCount,
      final String chunkName,
      final long checksum,
      final byte[] content,
      final long fileBlockPosition,
      final long totalFileSize,
      final SnapshotType type) {
    this.snapshotId = snapshotId;
    this.totalCount = totalCount;
    this.chunkName = chunkName;
    this.checksum = checksum;
    this.content = content;
    this.fileBlockPosition = fileBlockPosition;
    this.totalFileSize = totalFileSize;
    this.type = type;
  }

  @Override
  public SnapshotType getType() {
    return type;
  }

  @Override
  public String getSnapshotId() {
    return snapshotId;
  }

  @Override
  public int getTotalCount() {
    return totalCount;
  }

  @Override
  public String getChunkName() {
    return chunkName;
  }

  @Override
  public long getChecksum() {
    return checksum;
  }

  @Override
  public byte[] getContent() {
    return content;
  }

  @Override
  public long getFileBlockPosition() {
    return fileBlockPosition;
  }

  @Override
  public long getTotalFileSize() {
    return totalFileSize;
  }

  @Override
  public long getContentLength() {
    return content.length;
  }

  /** 将分片内容包装为只读视图。 */
  public DirectBuffer getContentBuffer() {
    return new UnsafeBuffer(content);
  }

  /** 序列化后该分片占用的总字节数。 */
  public int getLength() {
    return FIXED_PART_BYTES
        + utf8Of(snapshotId).length
        + utf8Of(chunkName).length
        + content.length;
  }

  /** 把当前分片序列化到新分配的缓冲区并返回。 */
  public ByteBuffer toByteBuffer() {
    final ByteBuffer buffer = ByteBuffer.allocate(getLength());
    serializeInto(buffer);
    buffer.flip();
    return buffer;
  }

  /** 尝试从缓冲区起始处解析分片；失败时本实例回到空状态并返回 false。 */
  public boolean tryWrap(final DirectBuffer buffer) {
    return tryWrap(buffer, 0, buffer.capacity());
  }

  /** 尝试从缓冲区的指定区间解析分片；失败时本实例回到空状态并返回 false。 */
  public boolean tryWrap(final DirectBuffer buffer, final int offset, final int length) {
    final int[] cursor = {offset};
    try {
      final boolean headerInvalid =
          length < FIXED_PART_BYTES
              || buffer.getInt(cursor[0]) != MAGIC_NUMBER
              || buffer.getByte(cursor[0] + Integer.BYTES) != WIRE_VERSION;
      if (headerInvalid) {
        return resetAndReportFalse();
      }
      cursor[0] += Integer.BYTES;
      final int typeOrdinal = buffer.getByte(cursor[0]);
      cursor[0] += Byte.BYTES;
      final SnapshotType[] knownTypes = SnapshotType.values();
      if (typeOrdinal < 0 || typeOrdinal >= knownTypes.length) {
        return resetAndReportFalse();
      }
      final SnapshotType parsedType = knownTypes[typeOrdinal];

      final String parsedSnapshotId = readUtf8(buffer, cursor);
      final String parsedChunkName = readUtf8(buffer, cursor);
      final int parsedTotalCount = buffer.getInt(cursor[0]);
      cursor[0] += Integer.BYTES;
      final long parsedChecksum = buffer.getLong(cursor[0]);
      cursor[0] += Long.BYTES;
      final long parsedBlockPosition = buffer.getLong(cursor[0]);
      cursor[0] += Long.BYTES;
      final long parsedFileSize = buffer.getLong(cursor[0]);
      cursor[0] += Long.BYTES;
      final int contentBytes = buffer.getInt(cursor[0]);
      cursor[0] += Integer.BYTES;
      if (contentBytes < 0 || cursor[0] + contentBytes > offset + length) {
        return resetAndReportFalse();
      }
      final byte[] parsedContent = new byte[contentBytes];
      buffer.getBytes(cursor[0], parsedContent);

      snapshotId = parsedSnapshotId;
      chunkName = parsedChunkName;
      totalCount = parsedTotalCount;
      checksum = parsedChecksum;
      fileBlockPosition = parsedBlockPosition;
      totalFileSize = parsedFileSize;
      content = parsedContent;
      type = parsedType;
      return true;
    } catch (final Exception parseFailure) {
      return resetAndReportFalse();
    }
  }

  /** 把当前分片序列化写入可变缓冲区的指定偏移处，返回写入字节数。 */
  public int write(final MutableDirectBuffer buffer, final int offset) {
    final ByteBuffer scratch = toByteBuffer();
    buffer.putBytes(offset, scratch.array(), 0, scratch.remaining());
    return scratch.remaining();
  }

  /** 将本实例恢复为默认空状态。 */
  public void reset() {
    totalCount = 0;
    checksum = 0;
    fileBlockPosition = 0;
    totalFileSize = 0;
    snapshotId = "";
    chunkName = "";
    content = new byte[0];
    type = SnapshotType.REGULAR;
  }

  @Override
  public String toString() {
    return "SnapshotChunkImpl{snapshotId="
        + snapshotId
        + ", chunkName='"
        + chunkName
        + "', type="
        + type
        + ", totalCount="
        + totalCount
        + ", checksum="
        + checksum
        + ", fileBlockPosition="
        + fileBlockPosition
        + ", totalFileSize="
        + totalFileSize
        + ", contentLength="
        + content.length
        + "}";
  }

  /** 按线格式顺序把各字段写入缓冲区。 */
  private void serializeInto(final ByteBuffer buffer) {
    final byte[] snapshotIdBytes = utf8Of(snapshotId);
    final byte[] chunkNameBytes = utf8Of(chunkName);
    buffer.putInt(MAGIC_NUMBER).put(WIRE_VERSION).put((byte) type.ordinal());
    buffer.putInt(snapshotIdBytes.length).put(snapshotIdBytes);
    buffer.putInt(chunkNameBytes.length).put(chunkNameBytes);
    buffer.putInt(totalCount)
        .putLong(checksum)
        .putLong(fileBlockPosition)
        .putLong(totalFileSize)
        .putInt(content.length)
        .put(content);
  }

  /** 读取带四字节长度前缀的 UTF-8 字符串，并把游标推进到字符串之后。 */
  private static String readUtf8(final DirectBuffer buffer, final int[] cursor) {
    final int byteCount = buffer.getInt(cursor[0]);
    final byte[] raw = new byte[byteCount];
    buffer.getBytes(cursor[0] + Integer.BYTES, raw);
    cursor[0] += Integer.BYTES + byteCount;
    return new String(raw, StandardCharsets.UTF_8);
  }

  /** 取字符串的 UTF-8 字节形式。 */
  private static byte[] utf8Of(final String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /** 统一失败出口：先清空状态再返回 false。 */
  private boolean resetAndReportFalse() {
    reset();
    return false;
  }
}
