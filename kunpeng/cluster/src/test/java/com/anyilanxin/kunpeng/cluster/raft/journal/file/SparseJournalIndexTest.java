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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sparse journal index test.
 */
class SparseJournalIndexTest {

  @Test
  void shouldNotFindIndexWhenNotReachedDensity() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);

    // when
    final IndexInfo position = index.lookup(1);

    // then
    assertNull(position);
  }

  public static JournalRecord asJournalRecord(final long index, final long asqn) {
    return new TestJournalRecord(index, asqn, 0, null);
  }

  @Test
  void shouldFindIndexWhenReachedDensity() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);

    // when
    index.index(asJournalRecord(1, 1), 2);
    index.index(asJournalRecord(2, 2), 4);
    index.index(asJournalRecord(3, 3), 6);
    index.index(asJournalRecord(4, 4), 8);
    index.index(asJournalRecord(5, 5), 10);

    // then
    assertEquals(5, index.lookup(5).index());
    assertEquals(10, index.lookup(5).position());
    assertEquals(5, index.lookupAsqn(5));
  }

  @Test
  void shouldFindLowerIndexWhenNotReachedDensity() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 1), 2);
    index.index(asJournalRecord(2, 2), 4);
    index.index(asJournalRecord(3, 3), 6);
    index.index(asJournalRecord(4, 4), 8);
    index.index(asJournalRecord(5, 5), 10);

    // when
    index.index(asJournalRecord(6, 6), 12);
    index.index(asJournalRecord(7, 7), 14);
    index.index(asJournalRecord(8, 8), 16);

    // then
    assertEquals(5, index.lookup(8).index());
    assertEquals(10, index.lookup(8).position());
    assertEquals(5, index.lookupAsqn(8));
  }

  @Test
  void shouldFindNextIndexWhenReachedDensity() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 1), 2);
    index.index(asJournalRecord(2, 2), 4);
    index.index(asJournalRecord(3, 3), 6);
    index.index(asJournalRecord(4, 4), 8);
    index.index(asJournalRecord(5, 5), 10);
    index.index(asJournalRecord(6, 6), 12);
    index.index(asJournalRecord(7, 7), 14);
    index.index(asJournalRecord(8, 8), 16);

    // when
    index.index(asJournalRecord(9, 9), 18);
    index.index(asJournalRecord(10, 10), 20);

    // then
    assertEquals(10, index.lookup(10).index());
    assertEquals(20, index.lookup(10).position());
    assertEquals(10, index.lookupAsqn(10));
  }

  @Test
  void shouldTruncateIndex() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 10), 2);
    index.index(asJournalRecord(2, 20), 4);
    index.index(asJournalRecord(3, 30), 6);
    index.index(asJournalRecord(4, 40), 8);
    index.index(asJournalRecord(5, 50), 10);
    index.index(asJournalRecord(6, 60), 12);
    index.index(asJournalRecord(7, 70), 14);
    index.index(asJournalRecord(8, 80), 16);
    index.index(asJournalRecord(9, 90), 18);
    index.index(asJournalRecord(10, 100), 20);

    // when
    index.deleteAfter(8);

    // then
    assertEquals(5, index.lookup(8).index());
    assertEquals(10, index.lookup(8).position());
    assertEquals(5, index.lookup(10).index());
    assertEquals(10, index.lookup(10).position());
    assertEquals(5, index.lookupAsqn(80));
    assertEquals(5, index.lookupAsqn(90));
  }

  @Test
  void shouldTruncateCompleteIndex() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 10), 2);
    index.index(asJournalRecord(2, 20), 4);
    index.index(asJournalRecord(3, 30), 6);
    index.index(asJournalRecord(4, 40), 8);
    index.index(asJournalRecord(5, 50), 10);
    index.index(asJournalRecord(6, 60), 12);
    index.index(asJournalRecord(7, 70), 14);
    index.index(asJournalRecord(8, 80), 16);
    index.index(asJournalRecord(9, 90), 18);
    index.index(asJournalRecord(10, 100), 20);
    index.deleteAfter(8);

    // when
    index.deleteAfter(4);

    // then
    assertNull(index.lookup(4));
    assertNull(index.lookup(5));
    assertNull(index.lookup(8));
    assertNull(index.lookup(10));
    assertNull(index.lookupAsqn(40));
    assertNull(index.lookupAsqn(50));
    assertNull(index.lookupAsqn(80));
    assertNull(index.lookupAsqn(100));
  }

  @Test
  void shouldNotCompactIndex() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 10), 2);
    index.index(asJournalRecord(2, 20), 4);
    index.index(asJournalRecord(3, 30), 6);
    index.index(asJournalRecord(4, 40), 8);
    index.index(asJournalRecord(5, 50), 10);
    index.index(asJournalRecord(6, 60), 12);
    index.index(asJournalRecord(7, 70), 14);
    index.index(asJournalRecord(8, 80), 16);
    index.index(asJournalRecord(9, 90), 18);
    index.index(asJournalRecord(10, 100), 20);

    // when
    index.deleteUntil(8);

    // then
    assertNull(index.lookup(8));
    assertEquals(10, index.lookup(10).index());
    assertEquals(20, index.lookup(10).position());
  }

  @Test
  void shouldCompactIndex() {
    // given - every 5 index is added
    final JournalIndex index = new SparseJournalIndex(5);
    // index entries
    index.index(asJournalRecord(1, 10), 2);
    index.index(asJournalRecord(2, 20), 4);
    index.index(asJournalRecord(3, 30), 6);
    index.index(asJournalRecord(4, 40), 8);
    index.index(asJournalRecord(5, 50), 10);
    index.index(asJournalRecord(6, 60), 12);
    index.index(asJournalRecord(7, 70), 14);
    index.index(asJournalRecord(8, 80), 16);
    index.index(asJournalRecord(9, 90), 18);
    index.index(asJournalRecord(10, 100), 20);
    // when
    index.deleteUntil(11);

    // then
    assertNull(index.lookup(4));
    assertNull(index.lookup(5));
    assertNull(index.lookup(8));
    assertNull(index.lookupAsqn(40));
    assertNull(index.lookupAsqn(50));
    assertNull(index.lookupAsqn(80));
  }

  @Test
  void shouldFindAsqnWithInBound() {
    // given - every 2nd index is added
    final JournalIndex index = new SparseJournalIndex(2);

    // when
    index.index(asJournalRecord(1, 1), 2);
    index.index(asJournalRecord(2, 2), 4);
    index.index(asJournalRecord(3, 3), 6);
    index.index(asJournalRecord(4, 4), 8);
    index.index(asJournalRecord(5, 5), 10);
    index.index(asJournalRecord(6, 6), 10);

    // then
    assertNull(index.lookupAsqn(5, 1));
    assertEquals(2, index.lookupAsqn(5, 3));
    assertEquals(2, index.lookupAsqn(5, 3));
    assertEquals(4, index.lookupAsqn(5, 4));
    assertEquals(4, index.lookupAsqn(5, 5));
    assertEquals(4, index.lookupAsqn(Long.MAX_VALUE, 5));
    assertEquals(6, index.lookupAsqn(Long.MAX_VALUE, 6));
  }
}
