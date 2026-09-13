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

import com.anyilanxin.kunpeng.eventlog.serialize.VarInt.Cursor;
import org.agrona.DirectBuffer;

/**
 * 批帧解码：校验头 + 游标式条目遍历，全部视图零拷贝（实例可复用，wrap 后重置）。
 *
 * <p>校验策略 fail-fast：坏 magic/版本/长度前缀与 {@link #nextEntry()} 越界立即抛 {@link
 * IllegalArgumentException}（调用方包装为存储损坏异常）。
 */
public final class BatchFrameDecoder {

  private DirectBuffer buffer;
  private final Cursor cursor = new Cursor(0);
  private int frameEnd;

  private long firstPosition;
  private int entryCount;
  private long sourcePosition;
  private long timestamp;
  private int entryIndex = -1;

  // 当前条目解析结果（视图区间）
  private long entryKey;
  private int entryFlags;
  private int sourceIndex;
  private int metadataOffset;
  private int metadataLength;
  private int valueOffset;
  private int valueLength;

  /** 解析并校验批帧头；游标定位到首条目之前 */
  public BatchFrameDecoder wrap(final DirectBuffer block) {
    buffer = block;
    if (block.capacity() < BatchFrame.FIXED_HEADER_BYTES
        || block.getByte(0) != (byte) BatchFrame.MAGIC_1
        || block.getByte(1) != (byte) BatchFrame.MAGIC_2) {
      throw notBatchFrame("magic 不符（可能由旧格式 journal 写入, 需清空数据目录）");
    }
    final int version = block.getByte(2) & 0xFF;
    if (version != BatchFrame.VERSION) {
      throw notBatchFrame("不支持的批帧版本: " + version);
    }
    // flags(1B) 暂不解释（bit0 预留 CRC）
    cursor.offset = BatchFrame.FIXED_HEADER_BYTES;
    final int bodyLength = VarInt.readUInt32(block, cursor);
    frameEnd = cursor.offset + bodyLength;
    if (frameEnd > block.capacity()) {
      throw notBatchFrame("batchLength 越界: body=" + bodyLength + " capacity=" + block.capacity());
    }
    firstPosition = VarInt.readUInt64(block, cursor);
    entryCount = VarInt.readUInt32(block, cursor);
    sourcePosition = VarInt.readInt64(block, cursor);
    timestamp = VarInt.readUInt64(block, cursor);
    entryIndex = -1;
    return this;
  }

  private static IllegalArgumentException notBatchFrame(final String detail) {
    return new IllegalArgumentException("非法批帧: " + detail);
  }

  public long firstPosition() {
    return firstPosition;
  }

  public int entryCount() {
    return entryCount;
  }

  /** 批级 sourcePosition（-1 = 无） */
  public long sourcePosition() {
    return sourcePosition;
  }

  public long timestamp() {
    return timestamp;
  }

  /** 帧尾后一字节偏移（不含） */
  public int frameEnd() {
    return frameEnd;
  }

  /** 当前条目下标（0 起；尚未开始返回 -1） */
  public int entryIndex() {
    return entryIndex;
  }

  /** 推进到下一条目；已到批尾返回 false */
  public boolean nextEntry() {
    if (entryIndex + 1 >= entryCount) {
      return false;
    }
    entryIndex++;
    entryKey = VarInt.readInt64(buffer, cursor);
    entryFlags = buffer.getByte(cursor.offset++) & 0xFF;
    sourceIndex = VarInt.readInt32(buffer, cursor);
    metadataLength = VarInt.readUInt32(buffer, cursor);
    metadataOffset = cursor.offset;
    if (metadataOffset + metadataLength > frameEnd) {
      throw notBatchFrame("metadata 越界 @entry " + entryIndex);
    }
    cursor.offset += metadataLength;
    valueLength = VarInt.readUInt32(buffer, cursor);
    valueOffset = cursor.offset;
    if (valueOffset + valueLength > frameEnd) {
      throw notBatchFrame("value 越界 @entry " + entryIndex);
    }
    cursor.offset += valueLength;
    return true;
  }

  public long entryKey() {
    return entryKey;
  }

  public boolean entrySkipProcessing() {
    return (entryFlags & BatchFrame.ENTRY_FLAG_SKIP_PROCESSING) != 0;
  }

  /** 批内回指下标；-1 = 无 */
  public int entrySourceIndex() {
    return sourceIndex;
  }

  /** 还原为绝对 position = 批级 source（无批内回指时） */
  public long entrySourcePosition() {
    return sourceIndex >= 0 ? firstPosition + sourceIndex : sourcePosition;
  }

  /** 当前条目 position（= firstPosition + 下标） */
  public long entryPosition() {
    return firstPosition + entryIndex;
  }

  public int entryMetadataOffset() {
    return metadataOffset;
  }

  public int entryMetadataLength() {
    return metadataLength;
  }

  public int entryValueOffset() {
    return valueOffset;
  }

  public int entryValueLength() {
    return valueLength;
  }
}
