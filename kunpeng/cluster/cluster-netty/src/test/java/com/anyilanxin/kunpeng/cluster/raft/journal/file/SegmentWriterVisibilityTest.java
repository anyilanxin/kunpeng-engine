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
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 写侧状态的跨线程可见性契约：读线程必须观察到单调递增的 lastIndex，并能完整读出并发追加的记录。
 *
 * <p>SegmentWriter 的 {@code lastEntry}/{@code lastEntryPosition} 会被读线程经由 {@code
 * Segment#lastIndex()} 访问，字段必须保证可见性（volatile）。内存可见性缺陷无法确定性复现，
 * 本测试作为并发契约的回归守卫。
 */
public class SegmentWriterVisibilityTest {

  private static final int MAX_SEGMENT_SIZE = 8192;
  private static final int RECORD_PAYLOAD_SIZE = 128;
  private static final int RECORD_COUNT = 500;

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
  public void lastIndexIsMonotonicAndConvergentForConcurrentReaders() throws Exception {
    final var directory = temporaryFolder.newFolder();
    journal =
        SegmentedJournal.builder(meterRegistry)
            .withDirectory(directory)
            .withMaxSegmentSize(MAX_SEGMENT_SIZE)
            .withFreeDiskSpace(0)
            .withMetaStore(metaStore)
            .build();

    final var appenderDone = new CountDownLatch(1);
    final var failure = new AtomicBoolean(false);
    final var observedMaxIndex = new AtomicLong(0);

    final var appender =
        new Thread(
            () -> {
              try {
                for (var i = 0; i < RECORD_COUNT; i++) {
                  appendRecord();
                }
              } catch (final Exception e) {
                failure.set(true);
              } finally {
                appenderDone.countDown();
              }
            },
            "visibility-test-appender");
    appender.start();

    // 读线程持续采样 lastIndex：只允许单调不减，且不能观察到超过写入总量的幻影索引
    while (!appenderDone.await(10, TimeUnit.MILLISECONDS)) {
      final var observed = journal.getLastIndex();
      if (observed < observedMaxIndex.get() || observed > RECORD_COUNT) {
        failure.set(true);
        break;
      }
      observedMaxIndex.set(observed);
    }
    appender.join();
    assertThat(failure.get()).as("appender or observer failed").isFalse();

    // 写入完成后，lastIndex 必须收敛到确切值
    await("last index must converge to appended count")
        .atMost(Duration.ofSeconds(10))
        .until(() -> journal.getLastIndex() == RECORD_COUNT);
    assertThat(observedMaxIndex.get()).isLessThanOrEqualTo(RECORD_COUNT);

    // 读取器必须能从头到尾完整读出并发追加的记录（覆盖 SegmentReader.hasNext 的跨线程路径）
    try (var reader = journal.openReader()) {
      reader.seekToFirst();
      var count = 0;
      var previousIndex = 0L;
      while (reader.hasNext()) {
        final JournalRecord record = reader.next();
        assertThat(record.index()).isEqualTo(previousIndex + 1);
        previousIndex = record.index();
        count++;
      }
      assertThat(count).isEqualTo(RECORD_COUNT);
    }
  }

  private void appendRecord() {
    final var payload = new byte[RECORD_PAYLOAD_SIZE];
    Arrays.fill(payload, (byte) 7);
    journal.append(DirectBufferWriter.writerFor(new UnsafeBuffer(payload)));
  }
}
