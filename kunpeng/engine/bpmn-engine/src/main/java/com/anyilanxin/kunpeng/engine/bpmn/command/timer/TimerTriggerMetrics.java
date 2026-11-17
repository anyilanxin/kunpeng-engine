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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

/** BPMN timer 处理侧指标：记录从业务到期时间到主引擎一次有效处理尝试的延迟；失败重试会形成多次样本。 */
public final class TimerTriggerMetrics {

  private final Timer dueLag;

  public TimerTriggerMetrics(final MeterRegistry registry, final int partitionId) {
    dueLag =
        Timer.builder("kunpeng.engine.timers.due_lag")
            .description("Lag between BPMN timer due date and trigger processing attempt")
            .tag("partition", String.valueOf(partitionId))
            .publishPercentileHistogram()
            .register(registry);
  }

  public void observeDueLag(final long dueDate, final long now) {
    dueLag.record(Math.max(0, now - dueDate), TimeUnit.MILLISECONDS);
  }
}
