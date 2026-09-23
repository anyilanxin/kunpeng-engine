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
 * 从 segment 读回的一条完整日志记录。
 *
 * <p>持有三个视图：头部 {@link JournalRecordMetadata}（校验和与长度）、体部 {@link
 * JournalRecordData}（索引、序号、负载），以及直接覆盖记录原始字节的 {@code raw} 缓冲（零拷贝 交给上层）。{@code totalSize} 是该记录连同前置帧字段在 segment 内占用的字节数。
 */
public final class PersistedJournalRecord implements JournalRecord {

  private final JournalRecordMetadata header;
  private final JournalRecordData body;
  private final DirectBuffer raw;
  private final int totalSize;

  public PersistedJournalRecord(
      final JournalRecordMetadata header,
      final JournalRecordData body,
      final DirectBuffer raw,
      final int totalSize) {
    this.header = header;
    this.body = body;
    this.raw = raw;
    this.totalSize = totalSize;
  }

  /** 记录头（校验和与长度）。 */
  public JournalRecordMetadata metadata() {
    return header;
  }

  /** 记录体（索引、序号、负载）。 */
  public JournalRecordData record() {
    return body;
  }

  /** 直接覆盖记录原始字节的视图；底层若被 unmap 则失效，不可长期持有。 */
  @Override
  public DirectBuffer serializedRecord() {
    return raw;
  }

  /** 帧字段 + 头部 + 体部的总字节数。 */
  @Override
  public int size() {
    return totalSize;
  }

  @Override
  public DirectBuffer data() {
    return body.data();
  }

  @Override
  public long asqn() {
    return body.asqn();
  }

  @Override
  public long index() {
    return body.index();
  }

  @Override
  public long checksum() {
    return header.checksum();
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof final PersistedJournalRecord record)) {
      return false;
    }
    return totalSize == record.totalSize
        && Objects.equals(header, record.header)
        && Objects.equals(body, record.body)
        && Objects.equals(raw, record.raw);
  }

  @Override
  public int hashCode() {
    return Objects.hash(header, body, raw, totalSize);
  }

  @Override
  public String toString() {
    return "PersistedJournalRecord{header=%s, body=%s, totalSize=%d}"
        .formatted(header, body, totalSize);
  }
}
