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
package io.atomix.raft.metrics;

import static io.atomix.raft.metrics.RaftStartupMetricsDoc.BOOTSTRAP_DURATION;
import static io.atomix.raft.metrics.RaftStartupMetricsDoc.JOIN_DURATION;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;

/** Raft 分区服务启动相关指标采集 */
public class RaftStartupMetrics extends RaftMetrics {

  private final SettableGauge bootstrapDuration;
  private final SettableGauge joinDuration;

  public RaftStartupMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);
    bootstrapDuration =
        Micrometers.gauge(BOOTSTRAP_DURATION, registry, "partitionGroupName", partitionGroupName);
    joinDuration =
        Micrometers.gauge(JOIN_DURATION, registry, "partitionGroupName", partitionGroupName);
  }

  /** 记录分区服务引导启动耗时 */
  public void observeBootstrapDuration(final long durationMillis) {
    bootstrapDuration.set(durationMillis);
  }

  /** 记录分区服务加入集群耗时 */
  public void observeJoinDuration(final long durationMillis) {
    joinDuration.set(durationMillis);
  }
}
