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
package io.atomix.raft.metrics;

import static io.atomix.raft.metrics.LeaderMetricsDoc.APPEND_DATA_RATE;
import static io.atomix.raft.metrics.LeaderMetricsDoc.APPEND_ENTRIES_LATENCY;
import static io.atomix.raft.metrics.LeaderMetricsDoc.APPEND_RATE;
import static io.atomix.raft.metrics.LeaderMetricsDoc.COMMIT_RATE;
import static io.atomix.raft.metrics.LeaderMetricsDoc.NON_COMMITTED_ENTRIES;
import static io.atomix.raft.metrics.LeaderMetricsDoc.NON_REPLICATED_ENTRIES;
import static io.atomix.raft.metrics.LeaderMetricsDoc.REPLICATION_LAG_BYTES;
import static io.atomix.raft.metrics.LeaderMetricsDoc.SNAPSHOT_INSTALL_SENT_BYTES;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Leader 追加与复制相关指标采集 */
public class LeaderAppenderMetrics extends RaftMetrics implements CloseableSilently {
  private final MeterRegistry meterRegistry;
  private final Counter commitRate;
  private final SettableGauge nonCommittedEntriesValue;
  private final Map<String, SettableGauge> nonReplicatedEntries = new ConcurrentHashMap<>();
  private final Map<String, SettableGauge> replicationLagBytes = new ConcurrentHashMap<>();

  public LeaderAppenderMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    this.meterRegistry = meterRegistry;
    commitRate =
        Micrometers.counter(COMMIT_RATE, meterRegistry, "partitionGroupName", partitionGroupName);
    nonCommittedEntriesValue =
        Micrometers.gauge(
            NON_COMMITTED_ENTRIES, meterRegistry, "partitionGroupName", partitionGroupName);
  }

  /** 记录一次向 Follower 追加条目的完成耗时 */
  public void appendComplete(final long latencyms, final String memberId) {
    Micrometers.timer(
            APPEND_ENTRIES_LATENCY,
            meterRegistry,
            "follower",
            memberId,
            "partitionGroupName",
            partitionGroupName,
            "partition",
            partition)
        .record(latencyms, TimeUnit.MILLISECONDS);
  }

  /** 记录一次向 Follower 追加的条目数与数据量 */
  public void observeAppend(
      final String memberId, final int appendedEntries, final long appendedBytes) {
    Micrometers.counter(APPEND_RATE, meterRegistry, "follower", memberId, "partitionGroupName", partitionGroupName)
        .increment(appendedEntries);
    Micrometers.counter(APPEND_DATA_RATE, meterRegistry, "follower", memberId, "partitionGroupName", partitionGroupName)
        .increment(appendedBytes / 1024f);
  }

  /** 记录一次条目提交 */
  public void observeCommit() {
    commitRate.increment();
  }

  /** 记录 Leader 上尚未提交的条目数 */
  public void observeNonCommittedEntries(final long remainingEntries) {
    nonCommittedEntriesValue.set(remainingEntries);
  }

  /** 记录各 Follower 尚未复制的条目数 */
  public void observeRemainingEntries(final String memberId, final long remainingEntries) {
    nonReplicatedEntries
        .computeIfAbsent(
            memberId,
            id ->
                Micrometers.gauge(
                    NON_REPLICATED_ENTRIES,
                    meterRegistry,
                    "follower",
                    id,
                    "partitionGroupName",
                    partitionGroupName))
        .set(remainingEntries);
  }

  /** 记录各 Follower 的复制滞后字节数 */
  public void observeReplicationLagBytes(final String memberId, final long lagBytes) {
    replicationLagBytes
        .computeIfAbsent(
            memberId,
            id ->
                Micrometers.gauge(
                    REPLICATION_LAG_BYTES,
                    meterRegistry,
                    "partition",
                    partition,
                    "physicalTenant",
                    partitionGroupName,
                    "follower",
                    id))
        .set(lagBytes);
  }

  /** 记录向 Follower 发送快照安装分片的字节数 */
  public void observeInstallSent(final String memberId, final long bytes) {
    Micrometers.counter(
            SNAPSHOT_INSTALL_SENT_BYTES,
            meterRegistry,
            "follower",
            memberId,
            "partitionGroupName",
            partitionGroupName)
        .increment(bytes);
  }

  @Override
  public void close() {
    meterRegistry.remove(commitRate);
    nonCommittedEntriesValue.close();
    nonReplicatedEntries.values().forEach(SettableGauge::close);
    replicationLagBytes.values().forEach(SettableGauge::close);
  }
}
