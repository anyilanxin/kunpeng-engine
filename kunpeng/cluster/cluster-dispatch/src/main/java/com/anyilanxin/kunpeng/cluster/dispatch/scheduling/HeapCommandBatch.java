/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.utils.Either;

import java.util.*;
import java.util.function.Consumer;

/**
 * 基于堆内列表的 {@link CommandBatch} 可变实现。
 *
 * <p>每次 {@link #append} 都先征询 {@link CommandBatch.CapacityProbe}：探针拒绝时返回 {@link
 * BatchCapacityExceededException}、批次保持原样；接受时条目入列并累加字节数。 非线程安全，限单个收集者使用。
 */
public final class HeapCommandBatch implements CommandBatch {

  private final List<CommandRecord> records = new ArrayList<>();
  private final CapacityProbe capacityProbe;
  private int byteSize;

  /** 创建由 {@code capacityProbe} 把关容量的空批次。 */
  public HeapCommandBatch(final CapacityProbe capacityProbe) {
    this.capacityProbe = capacityProbe;
  }

  /** 返回探针恒拒的空批次（只读用途）。 */
  public static CommandBatch empty() {
    return new HeapCommandBatch((count, bytes) -> false);
  }

  /**
   * 物化并追加一条记录。
   *
   * @param key 记录 key，无 key 传 {@code -1}
   * @param sourceIndex 批内回指（本批第 N 条引发本条写入），无回指传 {@code -1}
   * @param metadata 记录元数据
   * @param valueWriter 记录值，入批时物化为独立字节拷贝
   * @return 超出容量时返回 {@link BatchCapacityExceededException}，成功返回右值
   */
  public Either<RuntimeException, Void> append(
      final long key,
      final int sourceIndex,
      final AdminRecordMetadata metadata,
      final BufferWriter valueWriter) {
    final var record = CommandRecord.materialize(key, sourceIndex, metadata, valueWriter);
    final int recordLength = record.getLength();

    if (!capacityProbe.test(records.size() + 1, byteSize + recordLength)) {
      return Either.left(
          new BatchCapacityExceededException(recordLength, records.size(), byteSize));
    }

    records.add(record);
    byteSize += recordLength;
    return Either.right(null);
  }

  /**
   * 返回按当前占用再容纳 {@code recordLength} 字节是否仍满足容量约束。
   *
   * <p>适用于「想先验证而不立即入批」的场景。
   */
  public boolean admits(final int recordLength) {
    return capacityProbe.test(records.size() + 1, byteSize + recordLength);
  }

  /** 返回批内记录的累计字节数。 */
  public int byteSize() {
    return byteSize;
  }

  @Override
  public Iterator<CommandRecord> iterator() {
    return records.iterator();
  }

  @Override
  public void forEach(final Consumer<? super CommandRecord> action) {
    records.forEach(action);
  }

  @Override
  public Spliterator<CommandRecord> spliterator() {
    return records.spliterator();
  }

  @Override
  public List<AppendEntry> entries() {
    return Collections.unmodifiableList(records);
  }
}
