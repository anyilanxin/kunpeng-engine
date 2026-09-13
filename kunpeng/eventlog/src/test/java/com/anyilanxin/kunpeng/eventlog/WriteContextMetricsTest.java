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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 按 WriteContext 维度的指标采集验证 */
@DisplayName("WriteContext 指标采集")
class WriteContextMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private InMemoryEventStore store;
  private EventLog log;

  private EventLog build() {
    return EventLog.builder()
        .withEventStore(store)
        .withLogName("ctx-metrics")
        .withPartitionId(1)
        .withMeterRegistry(registry)
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
  @Timeout(30)
  @DisplayName("五个上下文各自的追加批数/条目数正确打标")
  void perContextAppendedCounters() {
    store = new InMemoryEventStore();
    log = build();
    final EventLogWriter writer = log.newWriter();

    writer.tryAppend(WriteContext.USER_COMMAND, entry(1, -1, false, "m", "v"));
    writer.tryAppend(WriteContext.USER_COMMAND, entriesOfSize(3));
    writer.tryAppend(WriteContext.PROCESSING_RESULT, entry(2, -1, false, "m", "v"));
    writer.tryAppend(WriteContext.INTER_PARTITION, entriesOfSize(2));
    writer.tryAppend(WriteContext.SCHEDULED, entry(3, -1, false, "m", "v"));
    writer.tryAppend(WriteContext.INTERNAL, entry(4, -1, false, "m", "v"));

    for (final WriteContext context : WriteContext.values()) {
      final var count =
          registry.find("eventlog.append.context.count").tag("context", context.name()).counter();
      final var entries =
          registry.find("eventlog.append.context.entries").tag("context", context.name()).counter();
      assertThat(count).as("%s 批数", context).isNotNull();
      assertThat(entries).as("%s 条目数", context).isNotNull();
    }

    assertThat(
            registry.find("eventlog.append.context.count").tag("context", "USER_COMMAND").counter()
                .count())
        .isEqualTo(2.0);
    assertThat(
            registry.find("eventlog.append.context.entries").tag("context", "USER_COMMAND").counter()
                .count())
        .isEqualTo(4.0);
    assertThat(
            registry.find("eventlog.append.context.entries").tag("context", "INTER_PARTITION")
                .counter()
                .count())
        .isEqualTo(2.0);
    // 汇总与分项一致
    assertThat(registry.find("eventlog.append.count").counter().count()).isEqualTo(6.0);
  }

  @Test
  @Timeout(30)
  @DisplayName("拒绝按 context+reason 打标（速率耗尽与非法参数）")
  void perContextRejectionCounters() {
    store = new InMemoryEventStore();
    log = EventLog.builder()
        .withEventStore(store)
        .withLogName("ctx-reject")
        .withPartitionId(1)
        .withMeterRegistry(registry)
        .withMaxBatchSize(8)
        .build();
    final EventLogWriter writer = log.newWriter();

    // 超限 → INVALID_ARGUMENT
    final var oversize = writer.tryAppend(WriteContext.INTER_PARTITION,
        List.of(entry(1, -1, false, "meta-12345678", "value-12345678")));
    assertThat(oversize).isInstanceOf(AppendResult.Rejected.class);
    // 空批 → INVALID_ARGUMENT
    writer.tryAppend(WriteContext.SCHEDULED, List.of());
    // 关闭后 → CLOSED
    log.close();
    writer.tryAppend(WriteContext.INTERNAL, entry(9, -1, false, "m", "v"));

    assertThat(
            registry.find("eventlog.append.context.rejected")
                .tag("context", "INTER_PARTITION").tag("reason", "INVALID_ARGUMENT").counter()
                .count())
        .isEqualTo(1.0);
    assertThat(
            registry.find("eventlog.append.context.rejected")
                .tag("context", "SCHEDULED").tag("reason", "INVALID_ARGUMENT").counter().count())
        .isEqualTo(1.0);
    assertThat(
            registry.find("eventlog.append.context.rejected")
                .tag("context", "INTERNAL").tag("reason", "CLOSED").counter().count())
        .isEqualTo(1.0);
    // 汇总拒绝计数同步
    assertThat(registry.find("eventlog.append.rejected.invalid").counter().count())
        .isEqualTo(2.0);
  }
}
