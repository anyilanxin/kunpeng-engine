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

import com.anyilanxin.kunpeng.eventlog.impl.append.AppendEntryImpl;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;

/**
 * 待追加条目：{@code key + 批内回指 + 不透明 metadata/value 字节}。
 *
 * <p>本模块对 metadata/value 只做字节透传（编进批帧/从批帧解出），不理解其内容—— typed record 与 entry 的互转由 protocol-impl
 * 侧的桥接工厂完成。
 *
 * <ul>
 *   <li>{@link #key()}：{@code -1} 表示无 key
 *   <li>{@link #sourceIndex()}：批内回指（本批第 N 条引发本条写入），{@code -1} 表示无； 解码侧还原为绝对 sourcePosition =
 *       firstPosition + sourceIndex
 *   <li>{@link #isSkipProcessing()}：标记该条已被处理（重放/补写场景跳过引擎处理）
 * </ul>
 */
public interface AppendEntry {

  long key();

  int sourceIndex();

  boolean isSkipProcessing();

  BufferWriter metadata();

  BufferWriter value();

  /** metadata + value 字节数（帧编码用） */
  default int getLength() {
    return metadata().getLength() + value().getLength();
  }

  static AppendEntry of(final long key, final BufferWriter metadata, final BufferWriter value) {
    return new AppendEntryImpl(key, -1, false, metadata, value);
  }

  static AppendEntry of(
      final long key,
      final BufferWriter metadata,
      final BufferWriter value,
      final int sourceIndex) {
    return new AppendEntryImpl(key, sourceIndex, false, metadata, value);
  }

  /** 标记为已处理（对应旧 ofProcessed 语义） */
  static AppendEntry skipped(final AppendEntry entry) {
    return new AppendEntryImpl(
        entry.key(), entry.sourceIndex(), true, entry.metadata(), entry.value());
  }

  /** 读侧条目 → 自有拷贝的可写条目（读→写转发场景） */
  static AppendEntry copyOf(final LoggedEntry entry) {
    return AppendEntryImpl.copyOf(entry);
  }
}
