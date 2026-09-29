/*
 * Copyright 2016-present Open Networking Foundation
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

import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftServiceMetricsDoc.COMPACTION_TIME;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.CloseableTime;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Raft 日志压缩相关指标采集 */
public final class RaftServiceMetrics extends RaftMetrics {

  private final Timer compactionTime;
  private final MeterRegistry registry;

  public RaftServiceMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);
    compactionTime =
        Micrometers.timer(COMPACTION_TIME, registry, "partitionGroupName", partitionGroupName);
    this.registry = registry;
  }

  /** 压缩计时句柄，关闭时记录一次压缩耗时 */
  public CloseableSilently compactionTime() {
    return new CloseableTime(compactionTime, registry).start();
  }
}
