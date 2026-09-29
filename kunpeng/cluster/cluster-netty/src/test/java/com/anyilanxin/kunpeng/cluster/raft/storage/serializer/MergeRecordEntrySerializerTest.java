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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.storage.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.MergeRecordEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.RaftLogEntry;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/** {@link MergeRecordEntry} SBE 序列化 roundtrip 测试。 */
class MergeRecordEntrySerializerTest {

  private final RaftEntrySBESerializer serializer = new RaftEntrySBESerializer();

  @Test
  void shouldRoundTripThroughRaftLogFrame() {
    final MergeRecordEntry entry = new MergeRecordEntry(PartitionId.from("business", 3));

    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[serializer.getMergeRecordEntrySerializedLength(entry)]);
    final int written = serializer.writeMergeRecordEntry(7L, entry, buffer, 0);

    assertEquals(serializer.getMergeRecordEntrySerializedLength(entry), written);
    final RaftLogEntry decoded = serializer.readRaftLogEntry(buffer);
    assertEquals(7L, decoded.term());
    assertTrue(decoded.entry() instanceof MergeRecordEntry);
    assertEquals(PartitionId.from("business", 3), ((MergeRecordEntry) decoded.entry()).sourcePartition());
  }

  @Test
  void shouldWriteMergeRecordEntryAtAnyOffset() {
    // given
    final int offset = 10;
    final MergeRecordEntry entry =
        new MergeRecordEntry(PartitionId.from("tenant-with-dash-1", 42));

    // when
    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[offset + serializer.getMergeRecordEntrySerializedLength(entry)]);
    final int length = serializer.writeMergeRecordEntry(3L, entry, buffer, offset);
    final RaftLogEntry decoded =
        serializer.readRaftLogEntry(new UnsafeBuffer(buffer, offset, length));

    // then - 分组名含 '-' 时按最后一个分隔符解析不歧义
    assertEquals(3L, decoded.term());
    assertEquals(
        PartitionId.from("tenant-with-dash-1", 42),
        ((MergeRecordEntry) decoded.entry()).sourcePartition());
  }

  @Test
  void shouldBeInvisibleToApplicationConsumers() {
    final RaftLogEntry entry = new RaftLogEntry(1L, new MergeRecordEntry(PartitionId.from("business", 1)));
    // 非 ApplicationEntry：业务日志消费端天然跳过；不占用业务 asqn 序列
    assertFalse(entry.isApplicationEntry());
    assertTrue(entry.getLowestAsqn().isEmpty());
  }

  @Test
  void shouldRejectNullSourcePartition() {
    assertThrows(NullPointerException.class, () -> new MergeRecordEntry(null));
  }
}
