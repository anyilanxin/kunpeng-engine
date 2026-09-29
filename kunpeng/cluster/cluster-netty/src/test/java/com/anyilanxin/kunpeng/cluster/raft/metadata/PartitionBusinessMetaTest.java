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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link PartitionBusinessMeta} 内存状态测试。 */
class PartitionBusinessMetaTest {

  @Test
  void shouldDefaultToEmpty() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    assertEquals(-1, meta.appliedIndex());
    assertEquals(-1, meta.appliedTerm());
    assertTrue(meta.entries().isEmpty());
  }

  @Test
  void shouldApplyAndReadEntries() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5"));
    assertEquals(10, meta.appliedIndex());
    assertEquals(3, meta.appliedTerm());
    assertEquals("5", meta.entries().get("sourceId"));
  }

  @Test
  void shouldReplaceEntriesOnLaterApply() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5", "agentSourceIds", "1,3"));
    meta.apply(11, 3, Map.of("sourceId", "6"));
    assertEquals(11, meta.appliedIndex());
    assertEquals("6", meta.entries().get("sourceId"));
    // 整体覆盖（先清空再添加）：未携带的 key 即删除
    assertFalse(meta.entries().containsKey("agentSourceIds"));
  }

  @Test
  void shouldIgnoreStaleApply() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5"));
    meta.apply(9, 2, Map.of("sourceId", "1"));
    assertEquals(10, meta.appliedIndex());
    assertEquals("5", meta.entries().get("sourceId"));
  }

  @Test
  void shouldIgnoreDuplicateApply() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5"));
    meta.apply(10, 3, Map.of("sourceId", "6"));
    assertEquals("5", meta.entries().get("sourceId"));
  }

  @Test
  void shouldReturnImmutableEntriesView() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5"));
    assertThrows(UnsupportedOperationException.class, () -> meta.entries().put("k", "v"));
  }

  @Test
  void shouldTreatNullApplyAsEmpty() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, null);
    assertTrue(meta.entries().isEmpty());
    assertEquals(10, meta.appliedIndex());
  }

  @Test
  void shouldSnapshotAtomically() {
    final PartitionBusinessMeta meta = new PartitionBusinessMeta();
    meta.apply(10, 3, Map.of("sourceId", "5"));
    final PartitionBusinessMeta.AppliedState state = meta.appliedState();
    assertEquals(10, state.index());
    assertEquals(3, state.term());
    assertEquals("5", state.entries().get("sourceId"));
  }
}
