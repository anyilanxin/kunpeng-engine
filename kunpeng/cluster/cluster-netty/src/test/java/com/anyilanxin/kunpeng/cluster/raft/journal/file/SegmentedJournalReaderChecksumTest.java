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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 热读路径的 CRC 校验开关：默认校验损坏记录并抛异常，关闭后跳过校验直接返回记录。 */
public class SegmentedJournalReaderChecksumTest {

  private static final int MAX_SEGMENT_SIZE = 8192;
  private static final int RECORD_PAYLOAD_SIZE = 128;
  private static final int RECORD_COUNT = 8;

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
  public void corruptedRecordFailsReadByDefault() throws IOException {
    journal = openJournal(temporaryFolder.newFolder(), true);
    appendRecords();

    corruptFirstRecordPayload();

    try (var reader = journal.openReader()) {
      reader.seekToFirst();
      assertThatThrownBy(reader::next).isInstanceOf(CorruptedJournalException.class);
    }
  }

  @Test
  public void corruptedRecordIsReadableWhenVerificationDisabled() throws IOException {
    journal = openJournal(temporaryFolder.newFolder(), false);
    appendRecords();

    corruptFirstRecordPayload();

    try (var reader = journal.openReader()) {
      reader.seekToFirst();
      final var record = reader.next();
      assertThat(record.index()).isEqualTo(1);
      // 后续未损坏的记录仍可正常读取
      for (var expectedIndex = 2L; expectedIndex <= RECORD_COUNT; expectedIndex++) {
        assertThat(reader.hasNext()).isTrue();
        assertThat(reader.next().index()).isEqualTo(expectedIndex);
      }
      assertThat(reader.hasNext()).isFalse();
    }
  }

  private SegmentedJournal openJournal(final File directory, final boolean verifyReadChecksum) {
    return SegmentedJournal.builder(meterRegistry)
        .withDirectory(directory)
        .withMaxSegmentSize(MAX_SEGMENT_SIZE)
        .withFreeDiskSpace(0)
        .withVerifyReadChecksum(verifyReadChecksum)
        .withMetaStore(metaStore)
        .build();
  }

  private void appendRecords() {
    for (var i = 0; i < RECORD_COUNT; i++) {
      final var payload = new byte[RECORD_PAYLOAD_SIZE];
      Arrays.fill(payload, (byte) 7);
      journal.append(DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
    }
  }

  /** 借独立文件通道篡改首条记录的载荷字节；写的是同一页缓存，内存映射读取立即可见。 */
  private void corruptFirstRecordPayload() throws IOException {
    final var segmentFile = journal.getFirstSegment().file().file();
    final var descriptorLength = SegmentDescriptorSerializer.currentEncodingLength();
    // 首条记录帧始于描述符之后；+100 一定落在 128 字节载荷内部，不会碰到帧版本或元数据结构
    final int corruptOffset = descriptorLength + 100;
    try (var raf = new RandomAccessFile(segmentFile, "rw")) {
      final var original = raf.readByte();
      raf.seek(corruptOffset);
      raf.write(original ^ 0x1);
    }
  }
}
