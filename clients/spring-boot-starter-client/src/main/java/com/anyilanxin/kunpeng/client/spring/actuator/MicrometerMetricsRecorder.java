/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.spring.actuator;

import com.anyilanxin.kunpeng.client.spring.metrics.MetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micrometer 指标记录器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MicrometerMetricsRecorder implements MetricsRecorder {

  private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerMetricsRecorder.class);

  private final MeterRegistry meterRegistry;
  private final Map<String, Counter> counters = new ConcurrentHashMap<>();

  public MicrometerMetricsRecorder(final MeterRegistry meterRegistry) {
    LOGGER.info(
        "Enabling Micrometer based metrics for camunda-spring-sdk (available via Actuator)");
    this.meterRegistry = meterRegistry;
  }

  protected Counter newCounter(final String metricName, final String action, final String jobType) {
    final List<Tag> tags = new ArrayList<>();
    if (action != null && !action.isEmpty()) {
      tags.add(Tag.of("action", action));
    }
    if (jobType != null && !jobType.isEmpty()) {
      tags.add(Tag.of("type", jobType));
    }
    return meterRegistry.counter(metricName, tags);
  }

  @Override
  public void increase(
      final String metricName, final String action, final String type, final int count) {
    final String key = metricName + "#" + action + '#' + type;
    final Counter counter =
        counters.computeIfAbsent(key, k -> newCounter(metricName, action, type));
    counter.increment(count);
  }

  @Override
  public void executeWithTimer(
      final String metricName, final String jobType, final Runnable methodToExecute) {
    final Timer timer = meterRegistry.timer(metricName, "type", jobType);
    timer.record(methodToExecute);
  }
}
