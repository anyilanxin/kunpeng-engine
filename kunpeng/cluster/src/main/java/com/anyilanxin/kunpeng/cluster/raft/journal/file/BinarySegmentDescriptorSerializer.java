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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.ChecksumGenerator;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 段描述符的定长二进制编解码器，采用小端字节序。
 *
 * <p>段文件头部布局为：一个字节的描述符版本号，随后是元数据帧（"DESC" 魔数 + 描述符帧的
 * CRC32 校验和），最后是描述符帧本身（"SEGD" 魔数、格式版本、段 ID、起始索引、最大段大小、
 * 最后一条日志索引与最后写入位置）。
 */
final class BinarySegmentDescriptorSerializer implements SegmentDescriptorSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(BinarySegmentDescriptorSerializer.class);

  /** 整个头部统一使用小端编码。 */
  private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

  /** 元数据帧魔数 "DESC"。 */
  private static final int METADATA_MAGIC = 0x44_45_53_43;

  /** 描述符帧魔数 "SEGD"。 */
  private static final int DESCRIPTOR_MAGIC = 0x53_45_47_44;

  static final int DESCRIPTOR_VERSION = 3;

  // 描述符帧内各字段相对于描述符帧起点的偏移
  private static final int FIELD_MAGIC = 0;
  private static final int FIELD_FORMAT_VERSION = FIELD_MAGIC + Integer.BYTES;
  private static final int FIELD_SEGMENT_ID = FIELD_FORMAT_VERSION + Byte.BYTES;
  private static final int FIELD_FIRST_INDEX = FIELD_SEGMENT_ID + Long.BYTES;
  private static final int FIELD_MAX_SEGMENT_SIZE = FIELD_FIRST_INDEX + Long.BYTES;
  private static final int FIELD_LAST_INDEX = FIELD_MAX_SEGMENT_SIZE + Integer.BYTES;
  private static final int FIELD_LAST_POSITION = FIELD_LAST_INDEX + Long.BYTES;

  /** 描述符帧的字节长度。 */
  private static final int DESCRIPTOR_FRAME_LENGTH = FIELD_LAST_POSITION + Integer.BYTES;

  /** 元数据帧的字节长度。 */
  private static final int METADATA_FRAME_LENGTH = Integer.BYTES + Long.BYTES;

  /** 头部总编码长度：版本字节 + 元数据帧 + 描述符帧。 */
  static final int ENCODING_LENGTH =
      Byte.BYTES + METADATA_FRAME_LENGTH + DESCRIPTOR_FRAME_LENGTH;

  // 元数据帧与描述符帧在段文件中的绝对偏移
  private static final int METADATA_FRAME_OFFSET = Byte.BYTES;
  private static final int DESCRIPTOR_FRAME_OFFSET =
      METADATA_FRAME_OFFSET + METADATA_FRAME_LENGTH;

  private final ChecksumGenerator checksumGenerator = new ChecksumGenerator();
  private final UnsafeBuffer scratchView = new UnsafeBuffer();

  /** 头部编码格式的当前大版本。 */
  @Override
  public byte majorVersion() {
    return CUR_VERSION;
  }

  /** 描述符帧的当前小版本。 */
  @Override
  public byte minorVersion() {
    return DESCRIPTOR_VERSION;
  }

  /** 头部固定编码长度。 */
  @Override
  public int encodingLength() {
    return ENCODING_LENGTH;
  }

  /** 把描述符按定长布局写入段文件头部，并同步写入覆盖描述符帧的校验和。 */
  @Override
  public void writeTo(final SegmentDescriptor descriptor, final ByteBuffer buffer) {
    scratchView.wrap(buffer);
    scratchView.putByte(0, CUR_VERSION);

    final int base = DESCRIPTOR_FRAME_OFFSET;
    scratchView.putInt(base + FIELD_MAGIC, DESCRIPTOR_MAGIC, BYTE_ORDER);
    scratchView.putByte(base + FIELD_FORMAT_VERSION, (byte) DESCRIPTOR_VERSION);
    scratchView.putLong(base + FIELD_SEGMENT_ID, descriptor.id(), BYTE_ORDER);
    scratchView.putLong(base + FIELD_FIRST_INDEX, descriptor.index(), BYTE_ORDER);
    scratchView.putInt(base + FIELD_MAX_SEGMENT_SIZE, descriptor.maxSegmentSize(), BYTE_ORDER);
    scratchView.putLong(base + FIELD_LAST_INDEX, descriptor.lastIndex(), BYTE_ORDER);
    scratchView.putInt(base + FIELD_LAST_POSITION, descriptor.lastPosition(), BYTE_ORDER);

    final long descriptorChecksum =
        checksumGenerator.compute(scratchView, DESCRIPTOR_FRAME_OFFSET, DESCRIPTOR_FRAME_LENGTH);
    scratchView.putInt(METADATA_FRAME_OFFSET, METADATA_MAGIC, BYTE_ORDER);
    scratchView.putLong(
        METADATA_FRAME_OFFSET + Integer.BYTES, descriptorChecksum, BYTE_ORDER);

    LOG.trace(
        "已写出段 {} 的描述符，起始索引 {}，校验和 {}",
        descriptor.id(),
        descriptor.index(),
        descriptorChecksum);
  }

  /** 从段文件头部解析描述符；文件过短无法容纳定长头部时按损坏处理。 */
  @Override
  public SegmentDescriptor readFrom(final ByteBuffer buffer) {
    try {
      return decodeDescriptor(buffer);
    } catch (final IndexOutOfBoundsException outOfBounds) {
      throw new CorruptedJournalException("段描述符读取失败：文件长度不足", outOfBounds);
    }
  }

  /** 执行实际的校验与解码：先验版本与两个魔数，再核对校验和，最后读取各字段。 */
  private SegmentDescriptor decodeDescriptor(final ByteBuffer buffer) {
    final DirectBuffer view = new UnsafeBuffer(buffer);

    final byte headerVersion = view.getByte(0);
    if (headerVersion != CUR_VERSION) {
      throw new UnknownVersionException(
          String.format("段描述符版本不符：期望 %d，实际 %d", CUR_VERSION, headerVersion));
    }

    verifyMagic(view, METADATA_FRAME_OFFSET, METADATA_MAGIC, "元数据帧");
    verifyMagic(view, DESCRIPTOR_FRAME_OFFSET, DESCRIPTOR_MAGIC, "描述符帧");

    final long storedChecksum =
        view.getLong(METADATA_FRAME_OFFSET + Integer.BYTES, BYTE_ORDER);
    final long actualChecksum =
        checksumGenerator.compute(view, DESCRIPTOR_FRAME_OFFSET, DESCRIPTOR_FRAME_LENGTH);
    if (storedChecksum != actualChecksum) {
      throw new CorruptedJournalException("段描述符校验和不一致，可能存在数据损坏");
    }

    final int base = DESCRIPTOR_FRAME_OFFSET;
    final byte formatVersion = view.getByte(base + FIELD_FORMAT_VERSION);
    final long segmentId = view.getLong(base + FIELD_SEGMENT_ID, BYTE_ORDER);
    final long firstIndex = view.getLong(base + FIELD_FIRST_INDEX, BYTE_ORDER);
    final int maxSegmentSize = view.getInt(base + FIELD_MAX_SEGMENT_SIZE, BYTE_ORDER);
    final long lastIndex = view.getLong(base + FIELD_LAST_INDEX, BYTE_ORDER);
    final int lastPosition = view.getInt(base + FIELD_LAST_POSITION, BYTE_ORDER);

    return new SegmentDescriptor(
        headerVersion,
        formatVersion,
        segmentId,
        firstIndex,
        maxSegmentSize,
        Math.max(0, lastIndex),
        Math.max(0, lastPosition),
        (short) ENCODING_LENGTH);
  }

  /** 校验指定帧起始处的四字节魔数是否匹配，不匹配则抛出损坏异常。 */
  private static void verifyMagic(
      final DirectBuffer view, final int frameOffset, final int expectedMagic, final String frame) {
    if (view.getInt(frameOffset, BYTE_ORDER) != expectedMagic) {
      throw new CorruptedJournalException(frame + "魔数不符，段文件可能已损坏");
    }
  }
}
