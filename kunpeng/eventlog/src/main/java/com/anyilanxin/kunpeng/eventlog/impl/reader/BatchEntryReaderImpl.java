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

/**
 * 按源 position 聚合的批读实现：同一 sourcePosition 的<b>连续</b>条目归为一个批 （处理单条源事件产生的结果在日志中连续存放）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
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
    // 源位置比较对无源批(source=-1)失效——-1>-1 永假会顺序吃掉整条日志; 改按条目 position 定位
    // (负哨兵→首条, 非负→position 之后首条), 与上游 LogStreamBatchReaderImpl 语义一致
    return reader.seekToNextEntry(position);
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
