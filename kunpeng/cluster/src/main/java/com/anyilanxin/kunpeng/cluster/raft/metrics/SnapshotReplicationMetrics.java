/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;

public class SnapshotReplicationMetrics extends RaftMetrics {

  private final SettableGauge count;
  private final SettableGauge duration;

  public SnapshotReplicationMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    count =
        Micrometers.gauge(
            SnapshotReplicationMetricDocs.COUNT,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
    duration =
        Micrometers.gauge(
            SnapshotReplicationMetricDocs.DURATION,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
  }

  public void incrementCount() {
    count.inc();
  }

  public void decrementCount() {
    count.dec();
  }

  public void setCount(final int value) {
    count.set(value);
  }

  public void observeDuration(final long durationMillis) {
    duration.set(durationMillis);
  }
}
