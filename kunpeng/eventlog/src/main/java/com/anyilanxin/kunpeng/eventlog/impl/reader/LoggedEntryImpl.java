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
package com.anyilanxin.kunpeng.eventlog.impl.reader;

import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrameDecoder;
import org.agrona.DirectBuffer;

/** 读侧条目视图实现：从解码器当前条目拷贝标量与区间（视图零拷贝，实例复用） */
public final class LoggedEntryImpl implements LoggedEntry {

  private DirectBuffer block;
  private long position;
  private long sourcePosition;
  private long key;
  private long timestamp;
  private boolean skipProcessing;
  private int metadataOffset;
  private int metadataLength;
  private int valueOffset;
  private int valueLength;

  /** 从解码器当前条目装载（块 buffer 视图） */
  public void wrap(final DirectBuffer block, final BatchFrameDecoder decoder) {
    this.block = block;
    this.position = decoder.entryPosition();
    this.sourcePosition = decoder.entrySourcePosition();
    this.key = decoder.entryKey();
    this.timestamp = decoder.timestamp();
    this.skipProcessing = decoder.entrySkipProcessing();
    this.metadataOffset = decoder.entryMetadataOffset();
    this.metadataLength = decoder.entryMetadataLength();
    this.valueOffset = decoder.entryValueOffset();
    this.valueLength = decoder.entryValueLength();
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  public long getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public long getKey() {
    return key;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean isSkipProcessing() {
    return skipProcessing;
  }

  @Override
  public DirectBuffer getMetadata() {
    return block;
  }

  @Override
  public int getMetadataOffset() {
    return metadataOffset;
  }

  @Override
  public int getMetadataLength() {
    return metadataLength;
  }

  @Override
  public DirectBuffer getValue() {
    return block;
  }

  @Override
  public int getValueOffset() {
    return valueOffset;
  }

  @Override
  public int getValueLength() {
    return valueLength;
  }
}
