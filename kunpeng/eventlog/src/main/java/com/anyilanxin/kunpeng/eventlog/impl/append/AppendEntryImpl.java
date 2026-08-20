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
package com.anyilanxin.kunpeng.eventlog.impl.append;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import org.agrona.ExpandableArrayBuffer;

/**
 * 追加条目实现：两种形态——外源 {@link BufferWriter} 直写帧（零拷贝）， 或 {@link #copyOf(LoggedEntry)} 的自有字节拷贝（读→写转发，脱离源
 * buffer 生命周期）。
 *
 * <p>metadata/value 恒非 null：null 归一化为空载荷（帧编码热路径免判空）。
 */
public final class AppendEntryImpl implements AppendEntry {

  private static final BufferWriter EMPTY =
      new BufferWriter() {
        @Override
        public int getLength() {
          return 0;
        }

        @Override
        public void write(final org.agrona.MutableDirectBuffer buffer, final int offset) {
          // 空载荷无字节可写
        }
      };

  private final long key;
  private final int sourceIndex;
  private final boolean skipProcessing;
  private final BufferWriter metadata;
  private final BufferWriter value;

  public AppendEntryImpl(
      final long key,
      final int sourceIndex,
      final boolean skipProcessing,
      final BufferWriter metadata,
      final BufferWriter value) {
    this.key = key;
    this.sourceIndex = sourceIndex;
    this.skipProcessing = skipProcessing;
    this.metadata = metadata == null ? EMPTY : metadata;
    this.value = value == null ? EMPTY : value;
  }

  @Override
  public long key() {
    return key;
  }

  @Override
  public int sourceIndex() {
    return sourceIndex;
  }

  @Override
  public boolean isSkipProcessing() {
    return skipProcessing;
  }

  @Override
  public BufferWriter metadata() {
    return metadata;
  }

  @Override
  public BufferWriter value() {
    return value;
  }

  /** 读侧条目 → 自有拷贝（metadata/value 复制进新 buffer，源块可回收） */
  public static AppendEntry copyOf(final LoggedEntry entry) {
    return new AppendEntryImpl(
        entry.getKey(),
        -1,
        entry.isSkipProcessing(),
        copy(entry.getMetadata(), entry.getMetadataOffset(), entry.getMetadataLength()),
        copy(entry.getValue(), entry.getValueOffset(), entry.getValueLength()));
  }

  private static BufferWriter copy(
      final org.agrona.DirectBuffer source, final int offset, final int length) {
    final ExpandableArrayBuffer copied = new ExpandableArrayBuffer(Math.max(length, 1));
    if (length > 0) {
      copied.putBytes(0, source, offset, length);
    }
    return new com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter()
        .wrap(copied, 0, length);
  }
}
