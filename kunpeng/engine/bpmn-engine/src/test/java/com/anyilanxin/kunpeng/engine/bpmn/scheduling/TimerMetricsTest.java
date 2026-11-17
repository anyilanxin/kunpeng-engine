/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/** 引擎调度指标的分区标签隔离测试。 */
class TimerMetricsTest {

  @Test
  void shouldExposeQueueLagAndDedupSizePerPartition() {
    final var registry = new SimpleMeterRegistry();
    final var first = TimerMetrics.of(registry, 1);
    final var second = TimerMetrics.of(registry, 2);
    first.onEnqueue();
    first.observeLag(10);
    second.onEnqueue();
    second.onEnqueue();
    second.observeLag(20);
    final var firstDedup = new RegistryMetrics.BoundedRegistryMetrics(registry, 1);
    final var secondDedup = new RegistryMetrics.BoundedRegistryMetrics(registry, 2);
    firstDedup.forLifeCycle(TimerLifeCycle.TRIGGER).accept(3);
    secondDedup.forLifeCycle(TimerLifeCycle.TRIGGER).accept(5);

    assertEquals(1, queued(registry, 1));
    assertEquals(2, queued(registry, 2));
    assertEquals(1, lagCount(registry, 1));
    assertEquals(1, lagCount(registry, 2));
    assertEquals(3, dedupSize(registry, 1));
    assertEquals(5, dedupSize(registry, 2));
  }

  private static double queued(final SimpleMeterRegistry registry, final int partition) {
    return registry
        .get("kunpeng.engine.timers.queued")
        .tag("partition", String.valueOf(partition))
        .gauge()
        .value();
  }

  private static long lagCount(final SimpleMeterRegistry registry, final int partition) {
    return registry
        .get("kunpeng.engine.timers.lag")
        .tag("partition", String.valueOf(partition))
        .timer()
        .count();
  }

  private static double dedupSize(final SimpleMeterRegistry registry, final int partition) {
    return registry
        .get("kunpeng.engine.timers.dedup.size")
        .tags("intent", "TimerLifeCycle.TRIGGER", "partition", String.valueOf(partition))
        .gauge()
        .value();
  }
}
