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

import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrameDecoder;
import com.anyilanxin.kunpeng.eventlog.storage.EventStoreReader;
import java.util.NoSuchElementException;
import org.agrona.DirectBuffer;

/**
 * 拉取式条目读游标：跨块迭代 + seek 族。
 *
 * <p>游标不变量：{@code peeked == true ⟺ entry 视图 == 解码器当前条目}。 peekNext 惰性推进解码器并装载视图；next 消费视图（peeked 置
 * false，解码器原地不动）； 下一次 peekNext 再推进。返回的视图实例复用，仅到下一次 next 前有效。
 */
public final class EventLogReaderImpl implements EventLogReader {

  private final EventStoreReader storeReader;
  private final BatchFrameDecoder decoder = new BatchFrameDecoder();
  private final LoggedEntryImpl entry = new LoggedEntryImpl();

  private DirectBuffer block;
  private boolean blockOpen; // 当前块已 wrap 且未耗尽
  private boolean peeked; // 视图 == 解码器当前条目
  private long lastPosition;
  private boolean closed;

  public EventLogReaderImpl(final EventStoreReader storeReader) {
    this.storeReader = storeReader;
  }

  @Override
  public boolean hasNext() {
    return peekNext() != null;
  }

  @Override
  public LoggedEntry next() {
    final LoggedEntry view = peekNext();
    if (view == null) {
      throw new NoSuchElementException("日志已读到末尾");
    }
    lastPosition = view.getPosition();
    peeked = false;
    return view;
  }

  @Override
  public LoggedEntry peekNext() {
    checkOpen();
    if (peeked) {
      return entry;
    }
    if (advanceOne()) {
      entry.wrap(block, decoder);
      peeked = true;
      return entry;
    }
    return null;
  }

  @Override
  public boolean seek(final long position) {
    checkOpen();
    if (position < 0) {
      // 负哨兵（如导出器"从未导出"的 -1）→ 定位到首条, 恒成功（与旧实现一致）
      seekToFirstEntry();
      return true;
    }
    return seekTo(position, false);
  }

  @Override
  public boolean seekToNextEntry(final long position) {
    checkOpen();
    if (position < 0) {
      // 负哨兵: 从头开始即恢复成功——空日志下等待新事件即可, 不视为致命错误（与旧实现一致,
      // 否则新数据目录启动时 Exporter 恢复直接失败）
      seekToFirstEntry();
      return true;
    }
    final long next = position + 1;
    if (seekTo(next, false) && peekNext().getPosition() == next) {
      return true; // 恰好命中下一条
    }
    // 落点越过 next（烧毁 gap）或无更晚条目（尾部追平）:
    // 请求的 position 自身存在即视为定位成功——等待新事件即可（与旧实现
    // getNextEventPosition()==next || getCurrentPosition()==position 等价）
    final boolean exists =
        seekTo(position, false) && peekNext() != null && peekNext().getPosition() == position;
    peeked = false; // 存在性校验用: position 视为已越过, 不作为下一条返回（防重复处理）
    return exists;
  }

  @Override
  public void seekToFirstEntry() {
    checkOpen();
    resetCursor();
    storeReader.seek(0);
  }

  @Override
  public long seekToEnd() {
    checkOpen();
    resetCursor();
    storeReader.seek(Long.MAX_VALUE);
    long last = 0;
    while (advanceOne()) {
      entry.wrap(block, decoder);
      last = entry.getPosition();
    }
    lastPosition = last;
    return last;
  }

  @Override
  public long getPosition() {
    return lastPosition;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      storeReader.close();
    }
  }

  /** 推进解码器到下一条目（块耗尽自动取下一块）；全部耗尽返回 false */
  private boolean advanceOne() {
    while (true) {
      if (blockOpen && decoder.nextEntry()) {
        return true;
      }
      if (!loadNextBlock()) {
        return false;
      }
    }
  }

  private boolean seekTo(final long position, final boolean strictlyAfter) {
    resetCursor();
    storeReader.seek(position);
    while (advanceOne()) {
      entry.wrap(block, decoder);
      final long entryPosition = entry.getPosition();
      if (entryPosition > position || (!strictlyAfter && entryPosition == position)) {
        peeked = true; // 命中: 视图 = 解码器当前, 等待消费
        return true;
      }
      // 未到目标, 继续推进
    }
    return false;
  }

  private void resetCursor() {
    blockOpen = false;
    peeked = false;
    block = null;
  }

  private boolean loadNextBlock() {
    try {
      if (!storeReader.hasNext()) {
        blockOpen = false;
        return false;
      }
      block = storeReader.next();
      decoder.wrap(block);
      blockOpen = true;
      return true;
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException("日志块读取失败（存储损坏?）", e);
    }
  }

  private void checkOpen() {
    if (closed) {
      throw new IllegalStateException("reader 已关闭");
    }
  }
}
