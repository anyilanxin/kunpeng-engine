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
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 稀疏索引持久化：重启后无需全量扫描即可命中旧 segment 的索引。 */
public class SegmentIndexPersistenceTest {

  private static final int MAX_SEGMENT_SIZE = 1024;
  private static final int RECORD_PAYLOAD_SIZE = 128;

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
  public void reopenLoadsPersistedSegmentIndexes() throws IOException {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    appendRecords(journal, 32);
    journal.close();

    journal = openJournal(directory);

    // 重启后索引应为空重建状态的反例：旧 segment 中部的索引必须能直接命中
    final var indexInfo = journal.getJournalIndex().lookup(2);
    assertThat(indexInfo).as("index of a mid-first-segment record must be loaded").isNotNull();
    assertThat(indexInfo.index()).isEqualTo(2);

    // 命中的位置必须可直接用于读取：seek 到该索引能读到正确记录
    try (var reader = journal.openReader()) {
      assertThat(reader.seek(2)).isEqualTo(2);
      assertThat(reader.next().index()).isEqualTo(2);
    }
  }

  @Test
  public void deleteUntilRemovesIndexFilesOfDeletedSegments() throws IOException {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    appendRecords(journal, 32);
    journal.close();

    journal = openJournal(directory);
    // 删除首个 segment（索引 1..首个 segment 覆盖范围）
    final var firstSegmentLastIndex = journal.getFirstSegment().lastIndex();
    assertThat(journal.deleteUntil(firstSegmentLastIndex + 1)).isTrue();

    assertThat(new File(directory, "journal-1.idx")).doesNotExist();
    assertThat(new File(directory, "journal-2.idx")).exists();
    journal.close();
  }

  @Test
  public void corruptedIndexFileFallsBackToScan() throws IOException {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    appendRecords(journal, 32);
    journal.close();

    // 篡改首条索引条目的 position 字段：若被接受，seek 会落到记录中间导致解析失败
    final var indexFile = new File(directory, "journal-1.idx");
    assertThat(indexFile).exists();
    try (var raf = new java.io.RandomAccessFile(indexFile, "rw")) {
      final var original = raf.readByte();
      raf.seek(17 + 8);
      raf.write(original ^ 0x1);
    }

    journal = openJournal(directory);
    try (var reader = journal.openReader()) {
      assertThat(reader.seek(2)).isEqualTo(2);
      assertThat(reader.next().index()).isEqualTo(2);
      assertThat(reader.seek(1)).isEqualTo(1);
      assertThat(reader.next().index()).isEqualTo(1);
    }
  }

  private SegmentedJournal openJournal(final File directory) {
    return SegmentedJournal.builder(meterRegistry)
        .withDirectory(directory)
        .withMaxSegmentSize(MAX_SEGMENT_SIZE)
        .withFreeDiskSpace(0)
        .withJournalIndexDensity(1)
        .withMetaStore(metaStore)
        .build();
  }

  private void appendRecords(final SegmentedJournal journal, final int count) {
    for (var i = 0; i < count; i++) {
      final var payload = new byte[RECORD_PAYLOAD_SIZE];
      Arrays.fill(payload, (byte) 7);
      journal.append(DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
    }
  }
}
