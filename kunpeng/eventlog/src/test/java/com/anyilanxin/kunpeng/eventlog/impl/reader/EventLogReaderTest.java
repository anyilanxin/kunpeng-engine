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

import static com.anyilanxin.kunpeng.eventlog.TestEntries.entry;
import static com.anyilanxin.kunpeng.eventlog.TestEntries.entriesOfSize;
import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.eventlog.AppendResult.Appended;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.InMemoryEventStore;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 拉取式读游标：seek 族 / 跨块遍历 / gap 容忍 / peek 不消费 */
@DisplayName("EventLogReader 读游标")
class EventLogReaderTest {

  private InMemoryEventStore store;
  private EventLog log;

  @BeforeEach
  void setUp() {
    store = new InMemoryEventStore();
    log = EventLog.builder()
        .withEventStore(store)
        .withLogName("reader-test")
        .withPartitionId(1)
        .build();
    // 三个批: positions 1-2, 3-5, 6-6
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(2), 100);
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(3), 100);
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(1), 100);
  }

  @AfterEach
  void tearDown() {
    log.close();
    store.shutdown();
  }

  private void assertPositions(final EventLogReader reader, final long... positions) {
    for (final long expected : positions) {
      assertThat(reader.hasNext()).as("应还有 position %d", expected).isTrue();
      final LoggedEntry entry = reader.next();
      assertThat(entry.getPosition()).isEqualTo(expected);
    }
    assertThat(reader.hasNext()).isFalse();
  }

  @Test
  @DisplayName("顺序遍历跨块: 1..6 连续")
  void sequentialAcrossBlocks() {
    try (EventLogReader reader = log.newReader()) {
      reader.seekToFirstEntry();
      assertPositions(reader, 1, 2, 3, 4, 5, 6);
      assertThat(reader.getPosition()).isEqualTo(6);
    }
  }

  @Test
  @DisplayName("seek 精确命中 / seekToNextEntry 严格大于")
  void seekFamily() {
    try (EventLogReader reader = log.newReader()) {
      assertThat(reader.seek(4)).isTrue();
      assertThat(reader.peekNext().getPosition()).isEqualTo(4);
      assertPositions(reader, 4, 5, 6);

      assertThat(reader.seekToNextEntry(2)).isTrue();
      assertThat(reader.peekNext().getPosition()).isEqualTo(3);

      assertThat(reader.seek(3)).isTrue();
      assertThat(reader.peekNext().getPosition()).isEqualTo(3);

      // 尾部追平: position 存在即成功, 等待新事件（与旧 getCurrentPosition()==position 语义一致）
      assertThat(reader.seekToNextEntry(6)).isTrue();
      assertThat(reader.peekNext()).isNull();
      // 越过末尾且该 position 不存在 → false（完整性错误）
      assertThat(reader.seekToNextEntry(99)).isFalse();
      assertThat(reader.seek(999)).isFalse();
    }
  }

  @Test
  @DisplayName("seekToEnd 返回 lastPosition（恢复续号依据）")
  void seekToEnd() {
    try (EventLogReader reader = log.newReader()) {
      assertThat(reader.seekToEnd()).isEqualTo(6);
    }
  }

  @Test
  @DisplayName("peekNext 不推进游标")
  void peekDoesNotConsume() {
    try (EventLogReader reader = log.newReader()) {
      reader.seekToFirstEntry();
      assertThat(reader.peekNext().getPosition()).isEqualTo(1);
      assertThat(reader.peekNext().getPosition()).isEqualTo(1);
      assertThat(reader.next().getPosition()).isEqualTo(1);
      assertThat(reader.peekNext().getPosition()).isEqualTo(2);
    }
  }

  @Test
  @DisplayName("条目字段与载荷逐项断言（key/source/skip/metadata/value 视图）")
  void entryFields() {
    log.newWriter().tryAppend(WriteContext.INTERNAL,
        List.of(entry(42, -1, true, "M1", "V1")), 77);
    try (EventLogReader reader = log.newReader()) {
      reader.seek(7);
      final LoggedEntry entry = reader.next();
      assertThat(entry.getKey()).isEqualTo(42);
      assertThat(entry.getSourcePosition()).isEqualTo(77);
      assertThat(entry.isSkipProcessing()).isTrue();
      assertThat(entry.getTimestamp()).isPositive();
      assertThat(entry.getMetadataLength()).isEqualTo(2);
      assertThat(entry.getValueLength()).isEqualTo(2);
      final byte[] metadata = new byte[2];
      entry.getMetadata().getBytes(entry.getMetadataOffset(), metadata);
      assertThat(new String(metadata)).isEqualTo("M1");
      final byte[] value = new byte[2];
      entry.getValue().getBytes(entry.getValueOffset(), value);
      assertThat(new String(value)).isEqualTo("V1");
    }
  }

  @Test
  @DisplayName("gap 容忍: 烧毁批产生的 position 跳跃不报错")
  void gapTolerated() {
    store.failNextAppends(1);
    // position 7-8 烧毁
    final Object burned = log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(2), -1);
    assertThat(burned).isInstanceOf(Appended.class);
    // position 9 正常
    log.newWriter().tryAppend(WriteContext.INTERNAL, List.of(entry(1, -1, false, "m", "v")), -1);
    try (EventLogReader reader = log.newReader()) {
      reader.seek(6);
      assertThat(reader.next().getPosition()).isEqualTo(6);
      // 7-8 为 gap, 直接跳到 9
      assertThat(reader.next().getPosition()).isEqualTo(9);
    }
  }
  @Test
  @DisplayName("回归: 空日志负哨兵(-1)恢复成功——exporter 新数据目录启动场景")
  void emptyLogNegativeSentinelRecovery() {
    final InMemoryEventStore emptyStore = new InMemoryEventStore();
    final EventLog emptyLog = EventLog.builder()
        .withEventStore(emptyStore).withLogName("empty-recovery").withPartitionId(1).build();
    try (EventLogReader reader = emptyLog.newReader()) {
      // 旧语义: position < 0 → 定位到首条且恒成功（等待新事件, 不视为致命错误）
      assertThat(reader.seekToNextEntry(-1)).isTrue();
      assertThat(reader.seek(-1)).isTrue();
      assertThat(reader.hasNext()).isFalse();
    }
    emptyLog.close();
    emptyStore.shutdown();
  }
}
