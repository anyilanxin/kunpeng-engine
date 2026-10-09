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
package com.anyilanxin.kunpeng.eventlog.impl.reader;

import static com.anyilanxin.kunpeng.eventlog.TestEntries.entriesOfSize;
import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.eventlog.BatchEntryReader;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.InMemoryEventStore;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 按源 position 聚合批读：分组 / head 回卷重放 / seekToNextBatch。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("BatchEntryReader 按源聚合批读")
class BatchEntryReaderTest {

  private InMemoryEventStore store;
  private EventLog log;

  @BeforeEach
  void setUp() {
    store = new InMemoryEventStore();
    log = EventLog.builder()
        .withEventStore(store)
        .withLogName("batch-reader-test")
        .withPartitionId(1)
        .build();
    // 批1: source=100 → positions 1,2,3; 批2: source=200 → positions 4,5; 批3: source=300 → position 6
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(3), 100);
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(2), 200);
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(1), 300);
  }

  @AfterEach
  void tearDown() {
    log.close();
    store.shutdown();
  }

  @Test
  @DisplayName("按 sourcePosition 分组迭代（连续同源归批）")
  void grouping() {
    try (BatchEntryReader reader = log.newBatchReader()) {
      assertThat(reader.hasNext()).isTrue();
      BatchEntryReader.Batch first = reader.next();
      assertThat(count(first)).isEqualTo(3);

      assertThat(reader.hasNext()).isTrue();
      BatchEntryReader.Batch second = reader.next();
      assertThat(count(second)).isEqualTo(2);

      BatchEntryReader.Batch third = reader.next();
      assertThat(count(third)).isEqualTo(1);

      assertThat(reader.hasNext()).isFalse();
    }
  }

  @Test
  @DisplayName("head() 回卷重放当前批 + current() 语义")
  void headReplay() {
    try (BatchEntryReader reader = log.newBatchReader()) {
      final BatchEntryReader.Batch batch = reader.next();
      final LoggedEntry first = batch.next();
      assertThat(first.getPosition()).isEqualTo(1);
      assertThat(batch.current().getPosition()).isEqualTo(1);
      batch.next();
      assertThat(batch.current().getPosition()).isEqualTo(2);

      batch.head(); // 回卷
      assertThat(batch.hasNext()).isTrue();
      assertThat(batch.next().getPosition()).isEqualTo(1);
      assertThat(count(batch)).isEqualTo(2); // 剩余 2,3
    }
  }

  @Test
  @DisplayName("seekToNextBatch 按 position 定位（负值=从头，不吞日志）")
  void seekToNextBatch() {
    try (BatchEntryReader reader = log.newBatchReader()) {
      // 负哨兵（恢复起点 -1）→ 定位到首条
      assertThat(reader.seekToNextBatch(-1)).isTrue();
      assertThat(reader.hasNext()).isTrue();
      assertThat(reader.next().next().getPosition()).isEqualTo(1);

      // 非负: 跳过 position <= 给定值, 落在首个更晚条目
      assertThat(reader.seekToNextBatch(3)).isTrue();
      assertThat(reader.next().next().getPosition()).isEqualTo(4);

      // 尾部追平: 末条 position=6 存在即定位成功, 但无更晚条目
      assertThat(reader.seekToNextBatch(6)).isTrue();
      assertThat(reader.hasNext()).isFalse();
    }
  }

  @Test
  @DisplayName("无源批（生产写入 source=-1）：负值全量可读，非负按 position 续读")
  void seekToNextBatchOnSourcelessBatches() {
    final InMemoryEventStore sourcelessStore = new InMemoryEventStore();
    final EventLog sourcelessLog =
        EventLog.builder()
            .withEventStore(sourcelessStore)
            .withLogName("batch-reader-sourceless-test")
            .withPartitionId(1)
            .build();
    try {
      // 生产路径 2 参 tryAppend → sourcePosition=-1（回归: 旧实现 -1>-1 永假会吞掉整条日志）
      sourcelessLog.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(2));
      sourcelessLog.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(1));

      try (BatchEntryReader reader = sourcelessLog.newBatchReader()) {
        assertThat(reader.seekToNextBatch(-1)).isTrue();
        assertThat(reader.hasNext()).isTrue();
        assertThat(reader.next().next().getPosition()).isEqualTo(1);

        assertThat(reader.seekToNextBatch(1)).isTrue();
        assertThat(reader.next().next().getPosition()).isEqualTo(2);
      }
    } finally {
      sourcelessLog.close();
      sourcelessStore.shutdown();
    }
  }

  private int count(final BatchEntryReader.Batch batch) {
    int count = 0;
    while (batch.hasNext()) {
      batch.next();
      count++;
    }
    return count;
  }
}
