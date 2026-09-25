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
import static org.awaitility.Awaitility.await;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * segment 创建时机：打开只建首个 segment，滚动时同步创建下一个（不提前在后台备好）， 重置与关闭后不残留多余文件。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SegmentsManagerSegmentCreationTest {

  private static final int MAX_SEGMENT_SIZE = 1024;
  private static final int RECORD_PAYLOAD_SIZE = 128;
  private static final Duration OBSERVE = Duration.ofSeconds(1);

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
  public void openCreatesOnlyTheFirstSegment() throws IOException {
    final var directory = temporaryFolder.newFolder();

    openJournal(directory);

    assertThat(segmentFile(directory, 1)).exists();
    await("no next segment file should be created after open")
        .during(OBSERVE)
        .atMost(Duration.ofSeconds(5))
        .until(() -> !segmentFile(directory, 2).exists());
    assertThat(logSegmentFiles(directory))
        .containsExactly(segmentFile(directory, 1).getName());
  }

  @Test
  public void rollCreatesNextSegmentSynchronously() throws IOException {
    final var directory = temporaryFolder.newFolder();
    openJournal(directory);

    appendUntilRolled(directory);

    final var second = segmentFile(directory, 2);
    assertThat(second).exists();
    assertThat(second).hasSize(MAX_SEGMENT_SIZE);
    // 同步创建：滚动后目录里只有两个数据 segment，没有提前备好的第三个
    assertThat(logSegmentFiles(directory))
        .containsExactly(segmentFile(directory, 1).getName(), second.getName());
  }

  @Test
  public void resetLeavesOnlyTheFirstSegment() throws IOException {
    final var directory = temporaryFolder.newFolder();
    openJournal(directory);
    appendUntilRolled(directory);

    journal.reset(1);

    await("only the recreated first segment should remain after reset")
        .during(OBSERVE)
        .atMost(Duration.ofSeconds(5))
        .until(() -> logSegmentFiles(directory).size() == 1);
    assertThat(logSegmentFiles(directory))
        .containsExactly(segmentFile(directory, 1).getName());
  }

  @Test
  public void closeKeepsDataSegmentsOnly() throws IOException {
    final var directory = temporaryFolder.newFolder();
    openJournal(directory);
    appendUntilRolled(directory);

    journal.close();

    assertThat(logSegmentFiles(directory))
        .containsExactly(segmentFile(directory, 1).getName(), segmentFile(directory, 2).getName());
  }

  private void openJournal(final File directory) {
    journal =
        SegmentedJournal.builder(meterRegistry)
            .withDirectory(directory)
            .withMaxSegmentSize(MAX_SEGMENT_SIZE)
            .withFreeDiskSpace(0)
            .withMetaStore(metaStore)
            .build();
  }

  /** 追加记录直至写入位置越过首个 segment 容量，触发一次滚动。 */
  private void appendUntilRolled(final File directory) {
    final var payload = new byte[RECORD_PAYLOAD_SIZE];
    Arrays.fill(payload, (byte) 7);
    for (int i = 0; i < 64 && !segmentFile(directory, 2).exists(); i++) {
      journal.append(DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
    }
    assertThat(segmentFile(directory, 2)).exists();
  }

  private File segmentFile(final File directory, final int id) {
    return new File(directory, "journal-%d.log".formatted(id));
  }

  /** 目录下全部数据 segment 文件名（不含索引与删除标记），按名称排序。 */
  private List<String> logSegmentFiles(final File directory) {
    final var files =
        directory.listFiles(file -> file.isFile() && SegmentFile.isSegmentFile("journal", file));
    return files == null ? List.of() : Arrays.stream(files).map(File::getName).sorted().toList();
  }
}
