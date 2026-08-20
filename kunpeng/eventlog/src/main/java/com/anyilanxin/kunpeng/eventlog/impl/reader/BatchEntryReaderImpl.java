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

import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import java.util.NoSuchElementException;

/** 按源 position 聚合的批读实现：同一 sourcePosition 的<b>连续</b>条目归为一个批 （处理单条源事件产生的结果在日志中连续存放）。 */
public final class BatchEntryReaderImpl implements BatchEntryReader {

  private final EventLogReader reader;
  private final BatchImpl batch = new BatchImpl();

  public BatchEntryReaderImpl(final EventLogReader reader) {
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    return reader.peekNext() != null;
  }

  @Override
  public Batch next() {
    final LoggedEntry head = reader.peekNext();
    if (head == null) {
      throw new NoSuchElementException("日志已读到末尾");
    }
    batch.begin(head);
    return batch;
  }

  @Override
  public boolean seekToNextBatch(final long position) {
    while (true) {
      final LoggedEntry peeked = reader.peekNext();
      if (peeked == null) {
        return false;
      }
      if (peeked.getSourcePosition() > position) {
        return true;
      }
      reader.next();
    }
  }

  @Override
  public void close() {
    reader.close();
  }

  private final class BatchImpl implements Batch {

    private long sourcePosition;
    private long firstPosition;
    private LoggedEntry current;

    void begin(final LoggedEntry head) {
      this.sourcePosition = head.getSourcePosition();
      this.firstPosition = head.getPosition();
      this.current = null;
    }

    @Override
    public boolean hasNext() {
      final LoggedEntry peeked = reader.peekNext();
      return peeked != null && peeked.getSourcePosition() == sourcePosition;
    }

    @Override
    public LoggedEntry next() {
      if (!hasNext()) {
        throw new NoSuchElementException("批已读完");
      }
      current = reader.next();
      return current;
    }

    @Override
    public void head() {
      reader.seek(firstPosition);
      current = null;
    }

    @Override
    public LoggedEntry current() {
      return current;
    }
  }
}
