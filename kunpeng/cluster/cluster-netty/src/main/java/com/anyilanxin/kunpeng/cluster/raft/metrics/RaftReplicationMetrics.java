/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftReplicationMetricsDoc.APPEND_INDEX;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftReplicationMetricsDoc.COMMIT_INDEX;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;

/** Raft 日志复制位点相关指标采集 */
public class RaftReplicationMetrics extends RaftMetrics {

  private final SettableGauge commitIndex;
  private final SettableGauge appendIndex;

  public RaftReplicationMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);
    commitIndex =
        Micrometers.gauge(COMMIT_INDEX, registry, "partitionGroupName", partitionGroupName);
    appendIndex =
        Micrometers.gauge(APPEND_INDEX, registry, "partitionGroupName", partitionGroupName);
  }

  /** 记录当前提交索引 */
  public void setCommitIndex(final long value) {
    commitIndex.set(value);
  }

  /** 记录最后一条追加条目的索引 */
  public void setAppendIndex(final long value) {
    appendIndex.set(value);
  }
}
