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
package com.anyilanxin.kunpeng.eventlog;

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import org.agrona.DirectBuffer;

/**
 * 读侧条目视图：帧内零拷贝（metadata/value 为源 buffer 的视图区间）。
 *
 * <p>读→写转发用 {@link AppendEntry#copyOf(LoggedEntry)}（自有字节拷贝）。
 *
 * <p>生命周期约定：视图指向底层块 buffer，下一次 {@link EventLogReader#next()} 前 访问有效；需要跨迭代持有请用 {@code copyOf} 或立即物化。
 */
public interface LoggedEntry {

  long getPosition();

  /** 引发本条写入的源条目 position；无源返回 -1 */
  long getSourcePosition();

  /** -1 表示无 key */
  long getKey();

  long getTimestamp();

  boolean isSkipProcessing();

  DirectBuffer getMetadata();

  int getMetadataOffset();

  int getMetadataLength();

  DirectBuffer getValue();

  int getValueOffset();

  int getValueLength();

  /** 把 metadata 解进既有载体（零分配读路径） */
  default void readMetadata(final BufferReader reader) {
    reader.wrap(getMetadata(), getMetadataOffset(), getMetadataLength());
  }

  /** 把 value 解进既有载体（零分配读路径） */
  default void readValue(final BufferReader reader) {
    reader.wrap(getValue(), getValueOffset(), getValueLength());
  }
}
