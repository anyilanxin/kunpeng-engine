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
package com.anyilanxin.kunpeng.engine.bpmn.command.timer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** timer 到期延迟指标测试。 */
class TimerTriggerMetricsTest {

  @Test
  void shouldRecordActualDueLag() {
    final var registry = new SimpleMeterRegistry();
    final var metrics = new TimerTriggerMetrics(registry, 1);

    metrics.observeDueLag(1_000, 1_250);

    final var timer = registry.get("kunpeng.engine.timers.due_lag").tag("partition", "1").timer();
    assertEquals(1, timer.count());
    assertEquals(250, timer.totalTime(TimeUnit.MILLISECONDS));
  }

  @Test
  void shouldSeparateDueLagByPartition() {
    final var registry = new SimpleMeterRegistry();
    new TimerTriggerMetrics(registry, 1).observeDueLag(1_000, 1_100);
    new TimerTriggerMetrics(registry, 2).observeDueLag(1_000, 1_300);

    assertEquals(
        100,
        registry
            .get("kunpeng.engine.timers.due_lag")
            .tag("partition", "1")
            .timer()
            .totalTime(TimeUnit.MILLISECONDS));
    assertEquals(
        300,
        registry
            .get("kunpeng.engine.timers.due_lag")
            .tag("partition", "2")
            .timer()
            .totalTime(TimeUnit.MILLISECONDS));
  }
}
