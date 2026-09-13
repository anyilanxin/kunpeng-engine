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
package com.anyilanxin.kunpeng.eventlog.serialize;

import static com.anyilanxin.kunpeng.eventlog.serialize.VarInt.int32Length;
import static com.anyilanxin.kunpeng.eventlog.serialize.VarInt.int64Length;
import static com.anyilanxin.kunpeng.eventlog.serialize.VarInt.uint32Length;
import static com.anyilanxin.kunpeng.eventlog.serialize.VarInt.uint64Length;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import java.util.List;
import org.agrona.MutableDirectBuffer;

/**
 * 批帧编码（v1，小端 LEB128 varint，无对齐）。
 *
 * <p>一次追加调用 = 一个存储块 = 一个批帧。position 逐条不上 wire——由批级 {@code firstPosition + 下标} 推算；时间戳为批级（同批同时间戳）。
 *
 * <pre>
 * magic "EL"(0x45 0x4C) + version(1B) + flags(1B, bit0 预留 CRC) + batchLength(uvarint32)
 * ---- 以下计入 batchLength ----
 * firstPosition(uvarint64) + entryCount(uvarint32) + sourcePosition(svarint64) + timestamp(uvarint64)
 * entry[entryCount]:
 *   key(svarint64, -1=null) + entryFlags(1B bit0=skipProcessing) + sourceIndex(svarint32, -1=无)
 *   + metadataLen(uvarint32) + metadata + valueLen(uvarint32) + value
 * </pre>
 */
public final class BatchFrame {

  // wire 常量单一事实源（帧布局图见设计文档 §3.2/3.3 与本类 javadoc）
  public static final int MAGIC_1 = 0x45; // .E.
  public static final int MAGIC_2 = 0x4C; // .L.
  public static final int VERSION = 0x01;

  // 批级 flags: bit0 预留 CRC, 当前恒 0
  public static final int FLAG_RESERVED_CRC = 0x01;

  // 条目级 flags: bit0 = skipProcessing
  public static final int ENTRY_FLAG_SKIP_PROCESSING = 0x01;

  // 帧固定头（magic + version + flags）
  public static final int FIXED_HEADER_BYTES = 4;

  private BatchFrame() {}

  /** 批帧总字节数（与 {@link #serialize} 写出严格一致） */
  public static int calculateLength(
      final long firstPosition,
      final long sourcePosition,
      final long timestamp,
      final List<AppendEntry> entries) {
    int body =
        uint64Length(firstPosition)
            + uint32Length(entries.size())
            + int64Length(sourcePosition)
            + uint64Length(timestamp);
    for (final AppendEntry entry : entries) {
      body +=
          int64Length(entry.key())
              + 1
              + int32Length(entry.sourceIndex())
              + uint32Length(entry.metadata().getLength())
              + entry.metadata().getLength()
              + uint32Length(entry.value().getLength())
              + entry.value().getLength();
    }
    return FIXED_HEADER_BYTES + uint32Length(body) + body;
  }

  /**
   * 编码整个批帧到目标 buffer。
   *
   * @return 写出后的下一偏移
   */
  public static int serialize(
      final MutableDirectBuffer buffer,
      int offset,
      final long firstPosition,
      final long sourcePosition,
      final long timestamp,
      final List<AppendEntry> entries) {
    final int bodyLength = bodyLength(firstPosition, sourcePosition, timestamp, entries);

    buffer.putByte(offset++, (byte) MAGIC_1);
    buffer.putByte(offset++, (byte) MAGIC_2);
    buffer.putByte(offset++, (byte) VERSION);
    buffer.putByte(offset++, (byte) 0); // flags 预留
    offset = VarInt.writeUInt32(buffer, offset, bodyLength);

    offset = VarInt.writeUInt64(buffer, offset, firstPosition);
    offset = VarInt.writeUInt32(buffer, offset, entries.size());
    offset = VarInt.writeInt64(buffer, offset, sourcePosition);
    offset = VarInt.writeUInt64(buffer, offset, timestamp);

    for (final AppendEntry entry : entries) {
      offset = VarInt.writeInt64(buffer, offset, entry.key());
      buffer.putByte(offset++, (byte) (entry.isSkipProcessing() ? ENTRY_FLAG_SKIP_PROCESSING : 0));
      offset = VarInt.writeInt32(buffer, offset, entry.sourceIndex());
      offset = writeOpaque(buffer, offset, entry.metadata());
      offset = writeOpaque(buffer, offset, entry.value());
    }
    return offset;
  }

  /** 长度前缀 + 内容直写；null 视为空载荷（零分配热路径） */
  private static int writeOpaque(
      final MutableDirectBuffer buffer,
      int offset,
      final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter payload) {
    if (payload == null) {
      buffer.putByte(offset++, (byte) 0);
      return offset;
    }
    offset = VarInt.writeUInt32(buffer, offset, payload.getLength());
    payload.write(buffer, offset);
    return offset + payload.getLength();
  }

  private static int bodyLength(
      final long firstPosition,
      final long sourcePosition,
      final long timestamp,
      final List<AppendEntry> entries) {
    int body =
        uint64Length(firstPosition)
            + uint32Length(entries.size())
            + int64Length(sourcePosition)
            + uint64Length(timestamp);
    for (final AppendEntry entry : entries) {
      body +=
          int64Length(entry.key())
              + 1
              + int32Length(entry.sourceIndex())
              + uint32Length(entry.metadata().getLength())
              + entry.metadata().getLength()
              + uint32Length(entry.value().getLength())
              + entry.value().getLength();
    }
    return body;
  }
}
