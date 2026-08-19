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

public class RaftReplicationMetrics extends RaftMetrics {

  private final SettableGauge commitIndex;
  private final SettableGauge appendIndex;

  public RaftReplicationMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    commitIndex =
        Micrometers.gauge(
            RaftReplicationMetricDocs.COMMIT_INDEX,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
    appendIndex =
        Micrometers.gauge(
            RaftReplicationMetricDocs.APPEND_INDEX,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
  }

  public void setCommitIndex(final long value) {
    commitIndex.set(value);
  }

  public void setAppendIndex(final long value) {
    appendIndex.set(value);
  }
}
