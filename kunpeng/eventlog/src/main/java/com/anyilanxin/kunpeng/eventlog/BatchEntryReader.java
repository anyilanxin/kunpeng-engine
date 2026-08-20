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

import java.util.Iterator;

/** 按源 position 聚合的批读（回放/重建场景）：同一 sourcePosition 的连续条目归为一个 {@link Batch}，批间按 sourcePosition 严格递增。 */
public interface BatchEntryReader extends Iterator<BatchEntryReader.Batch>, AutoCloseable {

  @Override
  void close();

  /** 定位到 sourcePosition 大于给定值的下一个批；无更晚的批返回 false */
  boolean seekToNextBatch(long position);

  interface Batch extends Iterator<LoggedEntry> {

    /** 回卷到批首（重放当前批） */
    void head();

    /** 当前条目（未迭代时为批首） */
    LoggedEntry current();
  }
}
