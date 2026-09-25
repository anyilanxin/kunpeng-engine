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
  public void segmentCreationImmediatelyCreatesEmptyIndexFile() throws IOException {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);

    // 建段即建空 idx：尚未写入任何记录，索引文件就应按容量公式预分配到位
    final var indexFile = new File(directory, "journal-1.idx");
    assertThat(indexFile).exists();
    assertThat(indexFile)
        .hasSize(
            SegmentIndexFile.fileBytes(
                SegmentIndexFile.capacityEntries(MAX_SEGMENT_SIZE, 1)));

    // 空索引文件重启后可正常装载（无条目，读取路径自然回退扫描）
    journal.close();
    journal = openJournal(directory);
    assertThat(journal.getJournalIndex().lookup(1)).isNull();
  }

  @Test
  public void flushPersistsActiveSegmentIndexWithoutClose() throws Exception {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    // 3 条(约 154B/条)保证全部落在同一 segment：隔离"仅凭 flush 封存 active 段"的语义，
    // 排除滚动封存(getNextSegment 也会 persist)对断言的干扰
    appendRecords(journal, 3);
    assertThat(new File(directory, "journal-2.idx")).doesNotExist();
    // 不 close、不滚动段：仅凭 flush 就应推进 idx 头部提交点（offset 5 处的 LE count）
    journal.flush();
    assertThat(readCommittedCount(new File(directory, "journal-1.idx"))).isEqualTo(3);

    // 落盘内容必须可用：kill -9 语义下重开（不再有 close 落盘机会）仍能命中索引
    journal.close();
    journal = openJournal(directory);
    final var indexInfo = journal.getJournalIndex().lookup(2);
    assertThat(indexInfo).as("index must survive restart via flush-persisted file").isNotNull();
    assertThat(indexInfo.index()).isEqualTo(2);
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

  @Test
  public void uncommittedTailBytesAreIgnoredOnReopen() throws IOException {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    appendRecords(journal, 16);
    journal.close();

    // 在提交点之后写入垃圾条目字节：头部 count 未推进，恢复时必须忽略该尾部
    final var indexFile = new File(directory, "journal-1.idx");
    final var committed = readCommittedCount(indexFile);
    assertThat(committed).isGreaterThan(0);
    try (var raf = new java.io.RandomAccessFile(indexFile, "rw")) {
      raf.seek(HEADER_LENGTH + (long) committed * ENTRY_BYTES);
      raf.write(new byte[12]);
    }

    journal = openJournal(directory);
    final var indexInfo = journal.getJournalIndex().lookup(2);
    assertThat(indexInfo).as("committed prefix must survive torn tail").isNotNull();
    assertThat(indexInfo.index()).isEqualTo(2);
  }

  @Test
  public void truncatedLogShrinksPersistedIndexOnReopen() throws Exception {
    final var directory = temporaryFolder.newFolder();

    journal = openJournal(directory);
    appendRecords(journal, 8);
    journal.close();

    final var indexFile = new File(directory, "journal-1.idx");
    final var before = readCommittedCount(indexFile);
    assertThat(before).isGreaterThan(2);

    journal = openJournal(directory);
    journal.deleteAfter(2);
    journal.close();

    // 条目收缩触发全量重写自愈：提交点必须回落
    assertThat(readCommittedCount(indexFile)).isLessThan(before);

    journal = openJournal(directory);
    try (var reader = journal.openReader()) {
      assertThat(reader.seek(1)).isEqualTo(1);
      assertThat(reader.next().index()).isEqualTo(1);
    }
  }

  /** idx 头部布局：magic(4B) + version(1B) + count(4B, LE) + crc(8B)，条目 12B/条。 */
  private static final int COUNT_FIELD_OFFSET = 5;

  private static final int HEADER_LENGTH = 17;

  private static final int ENTRY_BYTES = 12;

  private int readCommittedCount(final File indexFile) throws IOException {
    final var bytes = java.nio.file.Files.readAllBytes(indexFile.toPath());
    return java.nio.ByteBuffer.wrap(bytes, COUNT_FIELD_OFFSET, 4)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .getInt();
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
