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

import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.ELECTION_LATENCY;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.ELECTION_TRIGGERED;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.HEARTBEAT_MISS;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.HEARTBEAT_TIME;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.ROLE;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.TERM;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.VOTE_GRANTED;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetricsDoc.VOTE_REJECTED;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

/** Raft 角色与选举相关指标采集 */
public class RaftRoleMetrics extends RaftMetrics {

  private final Counter heartbeatMiss;
  private final Timer heartbeatTime;
  private final SettableGauge role;
  private final SettableGauge electionLatency;
  private final SettableGauge term;
  private final Counter electionTriggered;
  private final Counter voteGranted;
  private final Counter voteRejected;

  public RaftRoleMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);

    heartbeatMiss =
        Micrometers.counter(HEARTBEAT_MISS, registry, "partitionGroupName", partitionGroupName);
    heartbeatTime =
        Micrometers.timer(HEARTBEAT_TIME, registry, "partitionGroupName", partitionGroupName);
    role = Micrometers.gauge(ROLE, registry, "partitionGroupName", partitionGroupName);
    electionLatency =
        Micrometers.gauge(ELECTION_LATENCY, registry, "partitionGroupName", partitionName);
    term = Micrometers.gauge(TERM, registry, "partitionGroupName", partitionGroupName);
    electionTriggered =
        Micrometers.counter(ELECTION_TRIGGERED, registry, "partitionGroupName", partitionGroupName);
    voteGranted =
        Micrometers.counter(VOTE_GRANTED, registry, "partitionGroupName", partitionGroupName);
    voteRejected =
        Micrometers.counter(VOTE_REJECTED, registry, "partitionGroupName", partitionGroupName);
  }

  public void becomingInactive() {
    role.set(0);
  }

  public void becomingFollower() {
    role.set(1);
  }

  /** 进入 Candidate 状态意味着一次选举被触发 */
  public void becomingCandidate() {
    role.set(2);
    electionTriggered.increment();
  }

  public void becomingLeader() {
    role.set(3);
  }

  /** 记录当前任期 */
  public void setTerm(final long currentTerm) {
    term.set(currentTerm);
  }

  public void countHeartbeatMiss() {
    heartbeatMiss.increment();
  }

  public void observeHeartbeatInterval(final long milliseconds) {
    heartbeatTime.record(milliseconds, TimeUnit.MILLISECONDS);
  }

  /** 当前已累计的心跳丢失次数（测试断言用） */
  public double getHeartbeatMissCount() {
    return heartbeatMiss.count();
  }

  public void setElectionLatency(final long latencyMs) {
    electionLatency.set(latencyMs);
  }

  /** 记录一次投票被授予 */
  public void countVoteGranted() {
    voteGranted.increment();
  }

  /** 记录一次投票被拒绝 */
  public void countVoteRejected() {
    voteRejected.increment();
  }
}
