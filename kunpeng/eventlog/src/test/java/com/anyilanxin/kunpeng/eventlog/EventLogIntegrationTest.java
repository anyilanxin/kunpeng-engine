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
package com.anyilanxin.kunpeng.eventlog;

import static com.anyilanxin.kunpeng.eventlog.TestEntries.entry;
import static com.anyilanxin.kunpeng.eventlog.TestEntries.entriesOfSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.anyilanxin.kunpeng.eventlog.AppendResult.Appended;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 端到端集成：写→提交通知→awaiter 回调→读往返 / 重开恢复 / 双写者并发 */
@DisplayName("EventLog 端到端集成")
class EventLogIntegrationTest {

  private InMemoryEventStore store;
  private EventLog log;

  private EventLog build() {
    return EventLog.builder()
        .withEventStore(store)
        .withLogName("integration-test")
        .withPartitionId(42)
        .build();
  }

  @AfterEach
  void tearDown() {
    if (log != null) {
      log.close();
    }
    if (store != null) {
      store.shutdown();
    }
  }

  @Test
  @DisplayName("同步模式: 写→commit→读 全链路往返")
  void syncRoundTrip() {
    store = new InMemoryEventStore();
    log = build();
    final EventLogWriter writer = log.newWriter();

    assertThat(writer.tryAppend(WriteContext.USER_COMMAND,
        List.of(entry(1, -1, false, "m1", "v1"), entry(2, 0, false, "m2", "v2")), 55))
        .isInstanceOf(Appended.class);

    assertThat(log.getLastCommittedPosition()).isEqualTo(2);
    try (EventLogReader reader = log.newReader()) {
      reader.seekToFirstEntry();
      final LoggedEntry first = reader.next();
      assertThat(first.getPosition()).isEqualTo(1);
      assertThat(first.getSourcePosition()).isEqualTo(55);
      final LoggedEntry second = reader.next();
      assertThat(second.getSourcePosition()).isEqualTo(1); // sourceIndex=0 → firstPosition+0
      assertThat(reader.hasNext()).isFalse();
    }
  }

  @Test
  @DisplayName("异步提交: awaiter 收到回调后可读到新条目")
  void asyncCommitNotification() {
    store = new InMemoryEventStore().withAsyncCommit();
    log = build();
    final AtomicLong notified = new AtomicLong();
    log.registerRecordAvailableListener(notified::incrementAndGet);

    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(3), -1);
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(notified.get()).isGreaterThanOrEqualTo(1));

    try (EventLogReader reader = log.newReader()) {
      reader.seekToFirstEntry();
      assertThat(reader.next().getPosition()).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("重开恢复: lastPosition 续号, 水位保持")
  void reopenRecovery() {
    store = new InMemoryEventStore();
    log = build();
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(4), -1);
    log.newWriter().tryAppend(WriteContext.INTERNAL, entriesOfSize(2), -1);
    log.close();

    // 同一存储重开 → 恢复 lastPosition=6
    log = build();
    assertThat(log.getLastCommittedPosition()).isEqualTo(6);
    final AppendResult result =
        log.newWriter().tryAppend(WriteContext.INTERNAL, List.of(entry(9, -1, false, "m", "v")), -1);
    assertThat(((Appended) result).firstPosition()).isEqualTo(7);
  }

  @Test
  @DisplayName("双写者并发: 恢复位 + 流控水位一致")
  void dualWriters() throws Exception {
    store = new InMemoryEventStore();
    log = build();
    final EventLogWriter writerA = log.newWriter();
    final EventLogWriter writerB = log.newWriter();
    final ExecutorService pool = Executors.newFixedThreadPool(2);
    final CountDownLatch start = new CountDownLatch(1);
    final List<Future<Integer>> futures = new CopyOnWriteArrayList<>();
    final int perWriter = 300;

    for (final EventLogWriter writer : List.of(writerA, writerB)) {
      futures.add(pool.submit(() -> {
        start.await();
        int appended = 0;
        for (int i = 0; i < perWriter; i++) {
          if (writer.tryAppend(WriteContext.INTERNAL, entriesOfSize(1), -1)
              instanceof Appended) {
            appended++;
          }
        }
        return appended;
      }));
    }
    start.countDown();
    int total = 0;
    for (final Future<Integer> future : futures) {
      total += future.get(10, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertThat(total).isEqualTo(perWriter * 2);
    assertThat(log.getLastCommittedPosition()).isEqualTo(perWriter * 2);
    assertThat(log.getFlowControl().lastWrittenPosition()).isEqualTo(perWriter * 2);
    assertThat(log.getFlowControl().lastProcessedPosition()).isZero(); // 未消费
  }

  @Test
  @DisplayName("关闭后写入拒绝")
  void closedSemantics() {
    store = new InMemoryEventStore();
    log = build();
    final EventLogWriter writer = log.newWriter();
    log.close();
    assertThat(writer.tryAppend(WriteContext.INTERNAL, entriesOfSize(1), -1)
        instanceof AppendResult.Rejected).isTrue();
  }
}
