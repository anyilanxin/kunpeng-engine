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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.BusinessMetaEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.RaftLogEntry;
import java.util.HashMap;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/** {@link BusinessMetaEntry} SBE 序列化 roundtrip 测试。 */
class BusinessMetaEntrySerializerTest {

  private final RaftEntrySBESerializer serializer = new RaftEntrySBESerializer();

  @Test
  void shouldRoundTripThroughRaftLogFrame() {
    final BusinessMetaEntry entry =
        new BusinessMetaEntry(Map.of("sourceId", "5", "agentSourceIds", "1,3,5"));

    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[serializer.getBusinessMetaEntrySerializedLength(entry)]);
    serializer.writeBusinessMetaEntry(7L, entry, buffer, 0);

    final RaftLogEntry decoded = serializer.readRaftLogEntry(buffer);
    assertEquals(7L, decoded.term());
    assertTrue(decoded.isBusinessMetaEntry());
    final BusinessMetaEntry restored = decoded.getBusinessMetaEntry();
    assertEquals(2, restored.entries().size());
    assertEquals("5", restored.entries().get("sourceId"));
    assertEquals("1,3,5", restored.entries().get("agentSourceIds"));
  }

  @Test
  void shouldRoundTripEmptyEntries() {
    final BusinessMetaEntry entry = new BusinessMetaEntry(Map.of());
    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[serializer.getBusinessMetaEntrySerializedLength(entry)]);
    serializer.writeBusinessMetaEntry(3L, entry, buffer, 0);

    final RaftLogEntry decoded = serializer.readRaftLogEntry(buffer);
    assertTrue(decoded.isBusinessMetaEntry());
    assertEquals(0, decoded.getBusinessMetaEntry().entries().size());
  }

  @Test
  void shouldBeInvisibleToApplicationConsumers() {
    final RaftLogEntry entry = new RaftLogEntry(1L, new BusinessMetaEntry(Map.of("sourceId", "5")));
    // 非 ApplicationEntry：业务日志消费端天然跳过；不占用业务 asqn 序列
    assertFalse(entry.isApplicationEntry());
    assertTrue(entry.isBusinessMetaEntry());
    assertTrue(entry.getLowestAsqn().isEmpty());
  }

  @Test
  void shouldCalculateActualSerializedSize() {
    final BusinessMetaEntry entry =
        new BusinessMetaEntry(Map.of("sourceId", "5", "agentSourceIds", "1,3,5"));
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
    final int written = serializer.writeBusinessMetaEntry(7L, entry, buffer, 0);
    assertEquals(serializer.getBusinessMetaEntrySerializedLength(entry), written);
  }

  @Test
  void shouldWriteBusinessMetaEntryAtAnyOffset() {
    // given
    final int offset = 10;
    final BusinessMetaEntry entry =
        new BusinessMetaEntry(Map.of("sourceId", "5", "agentSourceIds", "1,3,5"));

    // when
    final UnsafeBuffer buffer =
        new UnsafeBuffer(
            new byte[offset + serializer.getBusinessMetaEntrySerializedLength(entry)]);
    final int length = serializer.writeBusinessMetaEntry(7L, entry, buffer, offset);
    final RaftLogEntry decoded =
        serializer.readRaftLogEntry(new UnsafeBuffer(buffer, offset, length));

    // then
    assertEquals(7L, decoded.term());
    assertTrue(decoded.isBusinessMetaEntry());
    final BusinessMetaEntry restored = decoded.getBusinessMetaEntry();
    assertEquals(2, restored.entries().size());
    assertEquals("5", restored.entries().get("sourceId"));
    assertEquals("1,3,5", restored.entries().get("agentSourceIds"));
  }

  @Test
  void shouldRoundTripMultiByteUtf8() {
    final BusinessMetaEntry entry = new BusinessMetaEntry(Map.of("来源", "节点-5"));
    final UnsafeBuffer buffer =
        new UnsafeBuffer(new byte[serializer.getBusinessMetaEntrySerializedLength(entry)]);
    serializer.writeBusinessMetaEntry(1L, entry, buffer, 0);

    final RaftLogEntry decoded = serializer.readRaftLogEntry(buffer);
    assertEquals("节点-5", decoded.getBusinessMetaEntry().entries().get("来源"));
  }

  @Test
  void shouldRejectNullEntriesAndValues() {
    // Map 形态由类型保证 key 唯一；null key/value 一并拒绝
    assertThrows(NullPointerException.class, () -> new BusinessMetaEntry(null));
    final Map<String, String> withNullValue = new HashMap<>();
    withNullValue.put("sourceId", null);
    assertThrows(NullPointerException.class, () -> new BusinessMetaEntry(withNullValue));
    final Map<String, String> withNullKey = new HashMap<>();
    withNullKey.put(null, "5");
    assertThrows(NullPointerException.class, () -> new BusinessMetaEntry(withNullKey));
    assertNotNull(new BusinessMetaEntry(Map.of("sourceId", "5")));
  }

  @Test
  void shouldRejectTooManyEntries() {
    // SBE group numInGroup 为 uint8，上限 254
    final Map<String, String> tooMany = new HashMap<>();
    for (int i = 0; i <= BusinessMetaEntry.MAX_ITEMS; i++) {
      tooMany.put("key-" + i, String.valueOf(i));
    }
    assertThrows(IllegalArgumentException.class, () -> new BusinessMetaEntry(tooMany));
  }
}
