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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * seekToAsqn 定位语义：读取器应停在 asqn 不大于目标值的最后一条记录上。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SegmentedJournalSeekToAsqnTest {

  private static final int MAX_SEGMENT_SIZE = 1024;
  private static final int RECORD_PAYLOAD_SIZE = 128;
  private static final int RECORD_COUNT = 16;
  private static final long FIRST_ASQN = 100;

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final JournalMetaStore metaStore = new JournalMetaStore.InMemory();

  private SegmentedJournal journal;

  @After
  public void tearDown() {
    if (journal != null && journal.isOpen()) {
      journal.close();
    }
    meterRegistry.close();
  }

  @Test
  public void seeksToExactAsqn() throws IOException {
    openJournalWithRecords();

    try (var reader = journal.openReader()) {
      final var asqn = asqnOf(6);
      final var seekedIndex = reader.seekToAsqn(asqn);
      final JournalRecord record = reader.next();
      assertThat(seekedIndex).isEqualTo(record.index());
      assertThat(record.asqn()).isEqualTo(asqn);
      assertThat(record.index()).isEqualTo(6);
    }
  }

  @Test
  public void seeksToGreatestLowerOrEqualAsqn() throws IOException {
    openJournalWithRecords();

    try (var reader = journal.openReader()) {
      // 目标 asqn 落在两条记录之间，应停在较小的那条上
      final var between = asqnOf(6) + 5;
      assertThat(reader.seekToAsqn(between)).isEqualTo(6);
      assertThat(reader.next().index()).isEqualTo(6);
      assertThat(reader.next().index()).isEqualTo(7);
    }
  }

  @Test
  public void seekBelowAllAsqnsPositionsAtFirst() throws IOException {
    openJournalWithRecords();

    try (var reader = journal.openReader()) {
      assertThat(reader.seekToAsqn(FIRST_ASQN - 1)).isEqualTo(1);
      assertThat(reader.next().index()).isEqualTo(1);
    }
  }

  @Test
  public void seekAboveAllAsqnsPositionsAtLastMatch() throws IOException {
    openJournalWithRecords();

    try (var reader = journal.openReader()) {
      assertThat(reader.seekToAsqn(asqnOf(RECORD_COUNT) + 1000)).isEqualTo(RECORD_COUNT);
      assertThat(reader.next().index()).isEqualTo(RECORD_COUNT);
    }
  }

  @Test
  public void indexUpperBoundLimitsTheSeekResult() throws IOException {
    openJournalWithRecords();

    try (var reader = journal.openReader()) {
      // asqn 命中索引 8，但上界压到 3：结果必须 <= 3
      final var result = reader.seekToAsqn(asqnOf(8), 3);
      assertThat(result).isLessThanOrEqualTo(3);
      assertThat(reader.next().index()).isEqualTo(result);
      assertThat(reader.next().index()).isEqualTo(result + 1);
    }
  }

  private long asqnOf(final int recordNumber) {
    return FIRST_ASQN + (recordNumber - 1) * 10;
  }

  private void openJournalWithRecords() throws IOException {
    journal =
        SegmentedJournal.builder(meterRegistry)
            .withDirectory(temporaryFolder.newFolder())
            .withMaxSegmentSize(MAX_SEGMENT_SIZE)
            .withFreeDiskSpace(0)
            .withJournalIndexDensity(1)
            .withMetaStore(metaStore)
            .build();
    for (var i = 1; i <= RECORD_COUNT; i++) {
      final var payload = new byte[RECORD_PAYLOAD_SIZE];
      Arrays.fill(payload, (byte) 7);
      journal.append(asqnOf(i), DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
    }
  }
}
