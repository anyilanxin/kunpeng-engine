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

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalReader;
import com.anyilanxin.kunpeng.cluster.raft.journal.file.record.RecordData;
import com.anyilanxin.kunpeng.cluster.raft.journal.file.record.SBESerializer;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SegmentedJournalReaderTest {

  private static final int ENTRIES_PER_SEGMENT = 4;

  @TempDir
  Path directory;

  private final DirectBuffer data = new UnsafeBuffer("test".getBytes(StandardCharsets.UTF_8));

  private JournalReader reader;
  private SegmentedJournal journal;

  @BeforeEach
  void setup() {
    final int entrySize = FrameUtil.getLength() + getSerializedSize(data);

    journal =
      SegmentedJournal.builder()
        .withDirectory(directory.resolve("data").toFile())
        .withMaxSegmentSize(
          entrySize * ENTRIES_PER_SEGMENT + JournalSegmentDescriptor.getEncodingLength())
        .withJournalIndexDensity(5)
        .build();
    reader = journal.openReader();
  }

  @Test
  void shouldReadAfterCompact() {
    // given
    for (int i = 1; i <= ENTRIES_PER_SEGMENT * 5; i++) {
      assertThat(journal.append(i, data).index()).isEqualTo(i);
    }
    assertThat(reader.hasNext()).isTrue();

    // when - compact up to the first index of segment 3
    final int indexToCompact = ENTRIES_PER_SEGMENT * 2 + 1;
    journal.deleteUntil(indexToCompact);
    reader.seekToFirst();

    // then
    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next().index()).isEqualTo(indexToCompact);
  }

  @Test
  void shouldSeekToAnyIndexInMultipleSegments() {
    // given
    long asqn = 1;

    for (int i = 1; i <= ENTRIES_PER_SEGMENT * 2; i++) {
      journal.append(asqn++, data).index();
    }

    for (int i = 1; i < ENTRIES_PER_SEGMENT * 2; i++) {
      // when
      reader.seek(i);

      // then
      assertThat(reader.hasNext()).isTrue();
      assertThat(reader.next().index()).isEqualTo(i);
    }
  }

  @Test
  void shouldSeekToAnyAsqnInMultipleSegments() {
    // given

    for (int i = 1; i <= ENTRIES_PER_SEGMENT * 2; i++) {
      journal.append(i, data).index();
    }

    for (int i = 1; i <= ENTRIES_PER_SEGMENT * 2; i++) {
      // when
      reader.seekToAsqn(i);

      // then
      assertThat(reader.hasNext()).isTrue();
      assertThat(reader.next().asqn()).isEqualTo(i);
    }
  }

  @Test
  void shouldNotReadWhenAccessingDeletedSegment() {
    // given
    journal.append(data);
    final var reader = journal.openReader();

    // when
    journal.reset(100);

    // then
    assertThat(reader.hasNext()).isFalse();
  }

  @Test
  void shouldReadAfterReset() {
    // given
    journal.append(data);
    final var reader = journal.openReader();
    final int resetIndex = 100;
    journal.reset(resetIndex);
    journal.append(data);

    // when
    reader.seekToFirst();

    // then
    assertThat(reader.next().index()).isEqualTo(resetIndex);
  }

  private int getSerializedSize(final DirectBuffer data) {
    final var record = new RecordData(Long.MAX_VALUE, Long.MAX_VALUE, data);
    final var serializer = new SBESerializer();
    final ByteBuffer buffer = ByteBuffer.allocate(128);
    return serializer.writeData(record, new UnsafeBuffer(buffer), 0).get()
      + serializer.getMetadataLength();
  }
}
