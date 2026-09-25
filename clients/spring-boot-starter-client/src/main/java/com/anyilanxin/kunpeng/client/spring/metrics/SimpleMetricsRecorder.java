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
package com.anyilanxin.kunpeng.client.spring.metrics;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super simple class to record metrics in memory. Typically used for test cases
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SimpleMetricsRecorder implements MetricsRecorder {

  public HashMap<String, AtomicLong> counters = new HashMap<>();

  public HashMap<String, Long> timers = new HashMap<>();

  @Override
  public void increase(
      final String metricName, final String action, final String type, final int count) {
    final String key = key(metricName, action, type);
    if (!counters.containsKey(key)) {
      counters.put(key, new AtomicLong(count));
    } else {
      counters.get(key).addAndGet(count);
    }
  }

  @Override
  public void executeWithTimer(
      final String metricName, final String jobType, final Runnable methodToExecute) {
    final long startTime = System.currentTimeMillis();
    methodToExecute.run();
    timers.put(metricName + "#" + jobType, System.currentTimeMillis() - startTime);
  }

  private String key(final String metricName, final String action, final String type) {
    final String key = metricName + "#" + action + "#" + type;
    return key;
  }

  public long getCount(final String metricName, final String action, final String type) {
    if (!counters.containsKey(key(metricName, action, type))) {
      return 0;
    }
    return counters.get(key(metricName, action, type)).get();
  }
}
