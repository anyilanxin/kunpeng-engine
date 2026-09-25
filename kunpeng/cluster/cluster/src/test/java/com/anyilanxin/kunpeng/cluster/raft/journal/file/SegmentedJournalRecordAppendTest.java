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
 * 以 {@code append(JournalRecord)}（跟随者复制路径）追加的记录必须可完整回读且校验和一致。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SegmentedJournalRecordAppendTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final JournalMetaStore metaStore = new JournalMetaStore.InMemory();

  private SegmentedJournal sourceJournal;
  private SegmentedJournal targetJournal;

  @After
  public void tearDown() {
    if (sourceJournal != null && sourceJournal.isOpen()) {
      sourceJournal.close();
    }
    if (targetJournal != null && targetJournal.isOpen()) {
      targetJournal.close();
    }
    meterRegistry.close();
  }

  @Test
  public void appendedJournalRecordRoundTripsWithChecksum() throws IOException {
    final var sourceDirectory = temporaryFolder.newFolder("source");
    final var targetDirectory = temporaryFolder.newFolder("target");
    sourceJournal = openJournal(sourceDirectory);
    targetJournal = openJournal(targetDirectory);

    final var payload = new byte[128];
    Arrays.fill(payload, (byte) 9);
    final JournalRecord original =
        sourceJournal.append(DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));

    targetJournal.append(original);
    assertThat(targetJournal.getLastIndex()).isEqualTo(original.index());

    try (var reader = targetJournal.openReader()) {
      reader.seekToFirst();
      assertThat(reader.hasNext()).isTrue();
      final var readBack = reader.next();
      assertThat(readBack.index()).isEqualTo(original.index());
      assertThat(readBack.asqn()).isEqualTo(original.asqn());
      assertThat(readBack.checksum()).isEqualTo(original.checksum());
      assertThat(readBack.data()).isEqualTo(original.data());
      assertThat(reader.hasNext()).isFalse();
    }
  }

  private SegmentedJournal openJournal(final File directory) {
    return SegmentedJournal.builder(meterRegistry)
        .withDirectory(directory)
        .withMaxSegmentSize(8192)
        .withFreeDiskSpace(0)
        .withMetaStore(metaStore)
        .build();
  }
}
