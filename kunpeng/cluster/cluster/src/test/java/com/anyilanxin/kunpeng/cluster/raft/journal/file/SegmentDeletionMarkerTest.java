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
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * segment 软删除标记文件的命名与复用语义：标记名必须确定（不含进程内计数器）， 同编号 segment 重建后再次删除时能覆盖同名标记。
 */
public class SegmentDeletionMarkerTest {

  private static final int MAX_SEGMENT_SIZE = 1024;
  private static final int RECORD_PAYLOAD_SIZE = 128;
  private static final int RECORD_COUNT = 32;

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final JournalMetaStore metaStore = new JournalMetaStore.InMemory();

  private SegmentedJournal journal;
  private File journalDirectory;

  @After
  public void tearDown() {
    if (journal != null && journal.isOpen()) {
      journal.close();
    }
    meterRegistry.close();
  }

  @Test
  public void recognizesDeterministicMarkerName() {
    assertThat(SegmentFile.isDeletedSegmentFile("journal", "journal-1.log.deleted")).isTrue();
    assertThat(SegmentFile.isDeletedSegmentFile("journal", "journal-42.log.deleted")).isTrue();
    assertThat(SegmentFile.isDeletedSegmentFile("journal", "journal-1.log")).isFalse();
    assertThat(SegmentFile.isDeletedSegmentFile("other", "journal-1.log.deleted")).isFalse();
  }

  @Test
  public void deleteUntilRenamesSegmentToDeterministicMarker() throws IOException {
    openJournalWithRecords();

    // 停在首个 segment 上的读取器会阻止标记文件被立即物理删除
    try (final var reader = journal.openReader()) {
      reader.seekToFirst();
      journal.deleteUntil(journal.getLastIndex());

      assertThat(markerFile(1)).exists();
    }
  }

  @Test
  public void recreatedSegmentReusesMarkerNameWithoutCollision() throws IOException {
    openJournalWithRecords();

    // reader1 停在旧的首个 segment 上，阻止其标记文件被立即物理删除
    try (final var reader1 = journal.openReader()) {
      reader1.seekToFirst();
      journal.deleteUntil(journal.getLastIndex());
      assertThat(markerFile(1)).exists();

      // 重置后 journal-1.log 以同一编号重建；再次删除时必须能覆盖同名旧标记
      journal.reset(1);
      appendRecords();

      // reader2 停在重建后的首个 segment 上，保留新标记以便断言
      try (final var reader2 = journal.openReader()) {
        reader2.seekToFirst();
        journal.deleteUntil(journal.getLastIndex());

        assertThat(markerFile(1)).exists();
      }
    }
  }

  @Test
  public void deleteUntilDecrementsJournalSizeGauge() throws IOException {
    openJournalWithRecords();

    final var remaining = lastLogSegmentFile();
    final var sizeBefore = journalSizeGauge();

    journal.deleteUntil(journal.getLastIndex());

    // 只剩最后一个 segment：磁盘占用应精确等于该文件大小
    assertThat(journalSizeGauge()).isEqualTo(remaining.length());
    assertThat(journalSizeGauge()).isLessThan(sizeBefore);
  }

  private File markerFile(final long segmentId) {
    return new File(journalDirectory, "journal-%d.log.deleted".formatted(segmentId));
  }

  private File lastLogSegmentFile() {
    final var logs =
        Arrays.stream(journalDirectory.listFiles((dir, name) -> name.endsWith(".log")))
            .sorted(Comparator.comparingLong(this::segmentIdOf))
            .toList();
    assertThat(logs).isNotEmpty();
    return logs.get(logs.size() - 1);
  }

  private long segmentIdOf(final File file) {
    final var name = file.getName();
    return Long.parseLong(name.substring(name.indexOf('-') + 1, name.indexOf('.')));
  }

  private double journalSizeGauge() {
    return meterRegistry.get("atomix_journal_size_bytes").gauge().value();
  }

  private void openJournalWithRecords() throws IOException {
    journalDirectory = temporaryFolder.newFolder();
    journal =
        SegmentedJournal.builder(meterRegistry)
            .withDirectory(journalDirectory)
            .withMaxSegmentSize(MAX_SEGMENT_SIZE)
            .withFreeDiskSpace(0)
            .withJournalIndexDensity(1)
            .withMetaStore(metaStore)
            .build();
    appendRecords();
  }

  private void appendRecords() {
    for (var i = 1; i <= RECORD_COUNT; i++) {
      final var payload = new byte[RECORD_PAYLOAD_SIZE];
      Arrays.fill(payload, (byte) 7);
      journal.append(100 + i, DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
    }
  }
}
