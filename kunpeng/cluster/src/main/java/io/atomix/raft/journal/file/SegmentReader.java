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
package io.atomix.raft.journal.file;

import com.google.common.base.Preconditions;
import io.atomix.raft.journal.JournalRecord;
import io.atomix.raft.journal.record.BinaryRecordSerializer;
import io.atomix.raft.journal.record.JournalRecordReaderUtil;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 单个 segment 上的顺序记录读取器。
 *
 * <p>底层游标直接架在 segment 的内存映射上，支持顺序迭代（{@link #hasNext}/{@link #next}）、
 * 回到 segment 起点（{@link #reset}）以及借助稀疏索引的定位（{@link #seek}）。
 */
final class SegmentReader implements Iterator<JournalRecord> {

  private final int descriptorSize;
  private final JournalRecordReaderUtil recordDecoder;
  private final ByteBuffer mappedBytes;
  private final JournalIndex sparseIndex;
  private final Segment owningSegment;
  private long lastReadIndex;

  SegmentReader(final ByteBuffer buffer, final Segment segment, final JournalIndex index) {
    this.mappedBytes = buffer;
    this.owningSegment = segment;
    this.sparseIndex = index;
    this.descriptorSize = segment.descriptor().encodingLength();
    this.recordDecoder = new JournalRecordReaderUtil(new BinaryRecordSerializer());
    reset();
  }

  /** @return 读取器在 segment 内的字节偏移（不含头部描述符区域） */
  int getOffsetInSegment() {
    return mappedBytes.position() - descriptorSize;
  }

  @Override
  public boolean hasNext() {
    // segment 被并发删除后不允许继续读取
    if (!owningSegment.isOpen()) {
      return false;
    }
    // 下一条记录存在时，其帧版本字段必然非零
    return FrameUtil.hasValidVersion(mappedBytes);
  }

  @Override
  public JournalRecord next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    // 消费掉帧版本字节，让 position 指向记录头
    FrameUtil.readVersion(mappedBytes);

    final JournalRecord entry =
        recordDecoder.read(mappedBytes, getNextIndex(), FrameUtil.getLength());
    lastReadIndex = entry.index();
    return entry;
  }

  /** 将读取器回退到 segment 内第一条记录处。 */
  void reset() {
    mappedBytes.position(descriptorSize);
    lastReadIndex = owningSegment.index() - 1;
  }

  /**
   * 定位到不小于 {@code index} 的第一条记录。
   *
   * <p>先尝试稀疏索引直接跳转，再顺序扫描剩余部分；若目标索引此前未被索引过，扫描途中
   * 顺带把经过的记录补录进索引。
   *
   * @param index 目标日志索引
   */
  void seek(final long index) {
    Preconditions.checkState(
        owningSegment.isOpen(), "Segment is already closed. Reader must reset to a valid index.");

    final long firstIndex = owningSegment.index();
    final long lastIndex = owningSegment.lastIndex();
    reset();

    final IndexInfo hint = sparseIndex.lookup(index - 1);
    if (hint != null && hint.index() >= firstIndex && hint.index() <= lastIndex) {
      mappedBytes.position(hint.position());
      lastReadIndex = hint.index() - 1;
    }

    // 索引命中的位置可能离目标很远（例如该 segment 从未被索引过），此时边扫边建索引
    final boolean buildIndexWhileSeeking = !sparseIndex.hasIndexed(index);
    while (getNextIndex() < index && hasNext()) {
      final int entryOffset = mappedBytes.position();
      final JournalRecord entry = next();
      if (buildIndexWhileSeeking) {
        sparseIndex.index(entry, entryOffset);
      }
    }
  }

  /** 关闭读取器，向所属 segment 注销自身。 */
  void close() {
    owningSegment.onReaderClosed(this);
  }

  /** @return 下一条将被读到的日志索引 */
  long getNextIndex() {
    return lastReadIndex + 1;
  }
}
