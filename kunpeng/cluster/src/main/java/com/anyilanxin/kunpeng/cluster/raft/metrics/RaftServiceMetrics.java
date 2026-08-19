/*
 * Copyright 2016-present Open Networking Foundation
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class RaftServiceMetrics extends RaftMetrics {

  private final Timer compactionTime;

  public RaftServiceMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    compactionTime =
        Micrometers.timer(
            RaftServiceMetricDocs.COMPACTION_TIME,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
  }

  public void compactionTime(final long latencyms) {
    compactionTime.record(Duration.ofMillis(latencyms));
  }
}
