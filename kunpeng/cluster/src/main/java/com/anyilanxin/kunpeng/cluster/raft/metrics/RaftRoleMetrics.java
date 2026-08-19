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
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class RaftRoleMetrics extends RaftMetrics {

  private final SettableGauge role;
  private final Counter heartbeatMiss;
  private final Timer heartbeatTime;
  private final SettableGauge electionLatency;

  public RaftRoleMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    role =
        Micrometers.gauge(
            RaftRoleMetricDocs.ROLE,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
    heartbeatMiss =
        Micrometers.counter(
            RaftRoleMetricDocs.HEARTBEAT_MISS_COUNT,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
    heartbeatTime =
        Micrometers.timer(
            RaftRoleMetricDocs.HEARTBEAT_TIME,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
    electionLatency =
        Micrometers.gauge(
            RaftRoleMetricDocs.ELECTION_LATENCY,
            meterRegistry,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition);
  }

  public void becomingFollower() {
    role.set(1);
  }

  public void becomingCandidate() {
    role.set(2);
  }

  public void becomingLeader() {
    role.set(3);
  }

  public void countHeartbeatMiss() {
    heartbeatMiss.increment();
  }

  public void observeHeartbeatInterval(final long milliseconds) {
    heartbeatTime.record(Duration.ofMillis(milliseconds));
  }

  /** 读取指定注册中心内心跳丢失计数（指标不存在时返回 0） */
  public static double getHeartbeatMissCount(final MeterRegistry registry, final String partition) {
    final Counter counter =
        registry
            .find(RaftRoleMetricDocs.HEARTBEAT_MISS_COUNT.getName())
            .tags("partitionGroupName", partition, "partition", partition)
            .counter();
    return counter == null ? 0d : counter.count();
  }

  public void setElectionLatency(final long latencyMs) {
    electionLatency.set(latencyMs);
  }
}
