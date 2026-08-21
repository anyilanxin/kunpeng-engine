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
package com.anyilanxin.kunpeng.cluster.raft.journal.record;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import java.util.Objects;
import org.agrona.DirectBuffer;

/**
 * 已从磁盘读出的日志记录。
 *
 * <p>一条完整记录由两部分组成：头部 {@link RecordMetadata}（校验和、长度）与体部
 * {@link RecordData}（索引、序号、负载数据）。除此之外还携带了覆盖整条记录原始字节的
 * {@code serializedRecord} 视图，以及该记录在 segment 中占用的总字节数（含帧头）。
 */
public final class PersistedJournalRecord implements JournalRecord {

  /** 记录头。 */
  private final RecordMetadata metadata;

  /** 记录体。 */
  private final RecordData record;

  /** 覆盖记录原始字节的缓冲视图（依赖底层映射内存，不可越界存活）。 */
  private final DirectBuffer serializedRecord;

  /** 记录占用的总字节数（帧版本/长度字段 + 头部 + 体部）。 */
  private final int size;

  public PersistedJournalRecord(
      final RecordMetadata metadata,
      final RecordData record,
      final DirectBuffer serializedRecord,
      final int size) {
    this.metadata = metadata;
    this.record = record;
    this.serializedRecord = serializedRecord;
    this.size = size;
  }

  /** 记录头。 */
  public RecordMetadata metadata() {
    return metadata;
  }

  /** 记录体。 */
  public RecordData record() {
    return record;
  }

  /** 覆盖记录原始字节的缓冲视图（依赖底层映射内存，不可越界存活）。 */
  public DirectBuffer serializedRecord() {
    return serializedRecord;
  }

  /** 记录占用的总字节数（帧版本/长度字段 + 头部 + 体部）。 */
  public int size() {
    return size;
  }

  @Override
  public DirectBuffer data() {
    return record.data();
  }

  @Override
  public long asqn() {
    return record.asqn();
  }

  @Override
  public long index() {
    return record.index();
  }

  @Override
  public long checksum() {
    return metadata.checksum();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PersistedJournalRecord)) {
      return false;
    }
    final PersistedJournalRecord that = (PersistedJournalRecord) o;
    return size == that.size
        && Objects.equals(metadata, that.metadata)
        && Objects.equals(record, that.record)
        && Objects.equals(serializedRecord, that.serializedRecord);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, record, serializedRecord, size);
  }

  @Override
  public String toString() {
    return "PersistedJournalRecord{metadata="
        + metadata
        + ", record="
        + record
        + ", size="
        + size
        + '}';
  }
}
