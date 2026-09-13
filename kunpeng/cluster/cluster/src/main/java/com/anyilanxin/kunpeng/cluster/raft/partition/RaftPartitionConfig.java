/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import java.time.Duration;

/** Configurations for a single partition. */
public class RaftPartitionConfig {

  private static final Duration DEFAULT_ELECTION_TIMEOUT = Duration.ofMillis(2500);
  private static final Duration DEFAULT_SNAPSHOT_REQUEST_TIMEOUT = Duration.ofMillis(2500);
  private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMillis(250);
  private static final boolean DEFAULT_PRIORITY_ELECTION = true;
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final int DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT = 3;
  private static final Duration DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT = Duration.ofSeconds(0);
  private static final int DEFAULT_SNAPSHOT_REPLICATION_THRESHOLD = 100;
  private static final boolean DEFAULT_RECEIVE_ON_LEGACY_SUBJECT = true;
  // Keep in sync with the default in ExperimentalRaftCfg, which usually overrides this.
  private static final Duration DEFAULT_CONFIGURATION_CHANGE_TIMEOUT = Duration.ofSeconds(10);
  private static final long DEFAULT_REBALANCE_REPLICATION_LAG_THRESHOLD = 8L * 1024 * 1024;
  private static final Duration DEFAULT_REBALANCE_REPLICATION_TIMEOUT = Duration.ofSeconds(10);
  private static final int DEFAULT_REBALANCE_MAX_TRANSFER_ATTEMPTS = 3;
  private static final Duration DEFAULT_SNAPSHOT_INTERVAL = Duration.ofMinutes(5);
  private static final int DEFAULT_MAX_SNAPSHOT_COUNT = 1;
  private static final int DEFAULT_SNAPSHOT_ENTRY_TRIGGER_THRESHOLD = 100_000;
  private static final Duration DEFAULT_SNAPSHOT_MERGE_AWAIT_TIMEOUT = Duration.ofMinutes(5);

  /**
   * 优先级选举 target 的最小衰减步长。实际衰减取 max(此值, target/5)：小优先级范围（本项目
   * 常用的 1~5）下保持近线性的逐级放权；大范围（如 1~100）下按比例指数收敛，避免低优先级
   * 节点等待 O(N) 个选举超时。jraft 的 decayPriorityGap 默认 10 且下限钳到 10，因其典型
   * 优先级范围达上百；此处默认 1 以保持小范围下的既有节奏。
   */
  private static final int DEFAULT_PRIORITY_DECAY_GAP = 1;

  private Duration electionTimeout = DEFAULT_ELECTION_TIMEOUT;
  private Duration heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
  private int maxAppendsPerFollower = 2;
  private int maxAppendBatchSize = 32 * 1024;
  private boolean priorityElectionEnabled = DEFAULT_PRIORITY_ELECTION;
  private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  private Duration snapshotRequestTimeout = DEFAULT_SNAPSHOT_REQUEST_TIMEOUT;
  private int minStepDownFailureCount = DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT;
  private Duration maxQuorumResponseTimeout = DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT;
  private int preferSnapshotReplicationThreshold = DEFAULT_SNAPSHOT_REPLICATION_THRESHOLD;
  private long rebalanceReplicationLagThreshold = DEFAULT_REBALANCE_REPLICATION_LAG_THRESHOLD;
  private Duration rebalanceReplicationTimeout = DEFAULT_REBALANCE_REPLICATION_TIMEOUT;
  private int rebalanceMaxTransferAttempts = DEFAULT_REBALANCE_MAX_TRANSFER_ATTEMPTS;
  private RaftStorageConfig storageConfig = new RaftStorageConfig();
  private EntryValidator entryValidator;
  private Duration configurationChangeTimeout = DEFAULT_CONFIGURATION_CHANGE_TIMEOUT;
  private int snapshotChunkSize;

  /** 快照跨分区传输的批量分片累计字节上限（一批多片，单文件不限大小可跨批）。 */
  private int snapshotTransferMaxBatchSize = DEFAULT_SNAPSHOT_TRANSFER_MAX_BATCH_SIZE;

  private boolean receiveOnLegacySubject = DEFAULT_RECEIVE_ON_LEGACY_SUBJECT;

  /** 快照周期拍摄间隔。 */
  private Duration snapshotInterval = DEFAULT_SNAPSHOT_INTERVAL;

  /** 常规快照最大保留数量。 */
  private int maxSnapshotCount = DEFAULT_MAX_SNAPSHOT_COUNT;

  /**
   * 自上次快照水位起 commit index 推进达到该阈值时额外触发一次快照（与 {@link
   * #snapshotInterval} 周期触发互补：高写入速率下按条数及时截断日志，低速率下靠周期兜底）；
   * 0 表示禁用，仅保留周期触发。
   */
  private int snapshotEntryTriggerThreshold = DEFAULT_SNAPSHOT_ENTRY_TRIGGER_THRESHOLD;

  /** 合并快照完成等待超时：源分区推送后等待目标分区确认合并完成的最长时间。 */
  private Duration snapshotMergeAwaitTimeout = DEFAULT_SNAPSHOT_MERGE_AWAIT_TIMEOUT;

  /** 优先级选举 target 每次衰减的最小步长，实际衰减为 max(此值, target/5)。 */
  private int priorityDecayGap = DEFAULT_PRIORITY_DECAY_GAP;

  /** 快照跨分区传输批量的缺省累计字节上限（4 MiB）。 */
  private static final int DEFAULT_SNAPSHOT_TRANSFER_MAX_BATCH_SIZE = 4 * 1024 * 1024;

  /**
   * Returns the Raft leader election timeout.
   *
   * @return the Raft leader election timeout
   */
  public Duration getElectionTimeout() {
    return electionTimeout;
  }

  /**
   * Sets the leader election timeout.
   *
   * @param electionTimeout the leader election timeout
   * @return the Raft partition group configuration
   */
  public RaftPartitionConfig setElectionTimeout(final Duration electionTimeout) {
    this.electionTimeout = electionTimeout;
    return this;
  }

  /**
   * Returns the heartbeat interval.
   *
   * @return the heartbeat interval
   */
  public Duration getHeartbeatInterval() {
    return heartbeatInterval;
  }

  /**
   * Sets the heartbeat interval.
   *
   * @param heartbeatInterval the heartbeat interval
   * @return the Raft partition group configuration
   */
  public RaftPartitionConfig setHeartbeatInterval(final Duration heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
    return this;
  }

  public int getMaxAppendsPerFollower() {
    return maxAppendsPerFollower;
  }

  public void setMaxAppendsPerFollower(final int maxAppendsPerFollower) {
    this.maxAppendsPerFollower = maxAppendsPerFollower;
  }

  public int getMaxAppendBatchSize() {
    return maxAppendBatchSize;
  }

  public void setMaxAppendBatchSize(final int maxAppendBatchSize) {
    this.maxAppendBatchSize = maxAppendBatchSize;
  }

  /** 优先级选举 target 每次衰减的最小步长（实际衰减 max(此值, target/5)），默认 1。 */
  public int getPriorityDecayGap() {
    return priorityDecayGap;
  }

  public RaftPartitionConfig setPriorityDecayGap(final int priorityDecayGap) {
    this.priorityDecayGap = priorityDecayGap;
    return this;
  }

  public boolean isPriorityElectionEnabled() {
    return priorityElectionEnabled;
  }

  public void setPriorityElectionEnabled(final boolean enable) {
    priorityElectionEnabled = enable;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  /**
   * Sets the timeout for every requests send between the replicas.
   *
   * @param requestTimeout the request timeout
   */
  public void setRequestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Duration getSnapshotRequestTimeout() {
    return snapshotRequestTimeout;
  }

  /**
   * Sets the timeout for every snapshot request sent by raft leaders to the followers.
   *
   * @param snapshotRequestTimeout the request timeout
   */
  public void setSnapshotRequestTimeout(final Duration snapshotRequestTimeout) {
    this.snapshotRequestTimeout = snapshotRequestTimeout;
  }

  public int getSnapshotChunkSize() {
    return snapshotChunkSize;
  }

  public void setSnapshotChunkSize(final int snapshotChunkSize) {
    this.snapshotChunkSize = snapshotChunkSize;
  }

  public int getSnapshotTransferMaxBatchSize() {
    return snapshotTransferMaxBatchSize;
  }

  public void setSnapshotTransferMaxBatchSize(final int snapshotTransferMaxBatchSize) {
    this.snapshotTransferMaxBatchSize = snapshotTransferMaxBatchSize;
  }

  public Duration getConfigurationChangeTimeout() {
    return configurationChangeTimeout;
  }

  public void setConfigurationChangeTimeout(final Duration configurationChangeTimeout) {
    this.configurationChangeTimeout = configurationChangeTimeout;
  }

  public int getMinStepDownFailureCount() {
    return minStepDownFailureCount;
  }

  /**
   * If the leader is not able to reach the quorum, the leader may step down. This is triggered
   * after minStepDownFailureCount number of requests fails to get a response from the quorum of
   * followers as well as if the last response was received before maxQuorumResponseTime.
   *
   * @param minStepDownFailureCount The number of failures after which a leader considers stepping
   *     down.
   */
  public void setMinStepDownFailureCount(final int minStepDownFailureCount) {
    this.minStepDownFailureCount = minStepDownFailureCount;
  }

  public Duration getMaxQuorumResponseTimeout() {
    return maxQuorumResponseTimeout;
  }

  /**
   * If the leader is not able to reach the quorum, the leader may step down. This is triggered
   * after minStepDownFailureCount number of requests fails to get a response from the quorum of
   * followers as well as if the last response was received before maxQuorumResponseTime.
   *
   * <p>When this value is zero, it uses a default value of electionTimeout * 2
   *
   * @param maxQuorumResponseTimeout the quorum response time out to trigger leader step down
   */
  public void setMaxQuorumResponseTimeout(final Duration maxQuorumResponseTimeout) {
    this.maxQuorumResponseTimeout = maxQuorumResponseTimeout;
  }

  public int getPreferSnapshotReplicationThreshold() {
    return preferSnapshotReplicationThreshold;
  }

  public void setPreferSnapshotReplicationThreshold(final int preferSnapshotReplicationThreshold) {
    this.preferSnapshotReplicationThreshold = preferSnapshotReplicationThreshold;
  }

  /**
   * The maximum replication lag, in bytes, that the desired leader may have for the current leader
   * to attempt a coordinated leadership transfer. Above it the partition is skipped with {@code
   * LAG_TOO_HIGH}.
   *
   * <p>Operators can override this value for each rebalance request - this is a default value that
   * applies when no override is specified.
   */
  public long getRebalanceReplicationLagThreshold() {
    return rebalanceReplicationLagThreshold;
  }

  public void setRebalanceReplicationLagThreshold(final long rebalanceReplicationLagThreshold) {
    this.rebalanceReplicationLagThreshold = rebalanceReplicationLagThreshold;
  }

  /**
   * How long the current leader waits (paused, declining writes) for the desired leader to finish
   * replicating during a coordinated leadership transfer before cancelling with {@code
   * REPLICATION_TIMED_OUT}.
   *
   * <p>Operators can override this value for each rebalance request - this is a default value that
   * applies when no override is specified.
   */
  public Duration getRebalanceReplicationTimeout() {
    return rebalanceReplicationTimeout;
  }

  public void setRebalanceReplicationTimeout(final Duration rebalanceReplicationTimeout) {
    this.rebalanceReplicationTimeout = rebalanceReplicationTimeout;
  }

  /**
   * The maximum number of TimeoutNow requests the current leader sends (including the initial
   * request) before reporting {@code TIMEOUT_NOW_EXHAUSTED} during a leadership transfer.
   *
   * <p>Operators can override this value for each rebalance request - this is a default value that
   * applies when no override is specified.
   */
  public int getRebalanceMaxTransferAttempts() {
    return rebalanceMaxTransferAttempts;
  }

  public void setRebalanceMaxTransferAttempts(final int rebalanceMaxTransferAttempts) {
    this.rebalanceMaxTransferAttempts = rebalanceMaxTransferAttempts;
  }

  public RaftStorageConfig getStorageConfig() {
    return storageConfig;
  }

  public void setStorageConfig(final RaftStorageConfig storageConfig) {
    this.storageConfig = storageConfig;
  }

  public EntryValidator getEntryValidator() {
    return entryValidator;
  }

  public void setEntryValidator(final EntryValidator entryValidator) {
    this.entryValidator = entryValidator;
  }

  public boolean isReceiveOnLegacySubject() {
    return receiveOnLegacySubject;
  }

  public void setReceiveOnLegacySubject(final boolean receiveOnLegacySubject) {
    this.receiveOnLegacySubject = receiveOnLegacySubject;
  }

  /** 快照周期拍摄间隔。 */
  public Duration getSnapshotInterval() {
    return snapshotInterval;
  }

  public RaftPartitionConfig setSnapshotInterval(final Duration snapshotInterval) {
    this.snapshotInterval = snapshotInterval;
    return this;
  }

  /** 常规快照最大保留数量。 */
  public int getMaxSnapshotCount() {
    return maxSnapshotCount;
  }

  public RaftPartitionConfig setMaxSnapshotCount(final int maxSnapshotCount) {
    this.maxSnapshotCount = maxSnapshotCount;
    return this;
  }

  /** 条数触发快照的 commit index 推进阈值，0 表示禁用（默认 {@value
   * #DEFAULT_SNAPSHOT_ENTRY_TRIGGER_THRESHOLD}）。 */
  public int getSnapshotEntryTriggerThreshold() {
    return snapshotEntryTriggerThreshold;
  }

  public RaftPartitionConfig setSnapshotEntryTriggerThreshold(
      final int snapshotEntryTriggerThreshold) {
    this.snapshotEntryTriggerThreshold = snapshotEntryTriggerThreshold;
    return this;
  }

  /** 合并快照完成等待超时：源分区推送后等待目标分区确认合并完成的最长时间（默认 {@value
   * #DEFAULT_SNAPSHOT_MERGE_AWAIT_TIMEOUT}）。 */
  public Duration getSnapshotMergeAwaitTimeout() {
    return snapshotMergeAwaitTimeout;
  }

  public RaftPartitionConfig setSnapshotMergeAwaitTimeout(
      final Duration snapshotMergeAwaitTimeout) {
    this.snapshotMergeAwaitTimeout = snapshotMergeAwaitTimeout;
    return this;
  }

  @Override
  public String toString() {
    return "RaftPartitionConfig{"
        + "electionTimeout="
        + electionTimeout
        + ", heartbeatInterval="
        + heartbeatInterval
        + ", maxAppendsPerFollower="
        + maxAppendsPerFollower
        + ", maxAppendBatchSize="
        + maxAppendBatchSize
        + ", priorityElectionEnabled="
        + priorityElectionEnabled
        + ", requestTimeout="
        + requestTimeout
        + ", snapshotRequestTimeout="
        + snapshotRequestTimeout
        + ", snapshotChunkSize="
        + snapshotChunkSize
        + ", configurationChangeTimeout="
        + configurationChangeTimeout
        + ", minStepDownFailureCount="
        + minStepDownFailureCount
        + ", maxQuorumResponseTimeout="
        + maxQuorumResponseTimeout
        + ", preferSnapshotReplicationThreshold="
        + preferSnapshotReplicationThreshold
        + ", rebalanceReplicationLagThreshold="
        + rebalanceReplicationLagThreshold
        + ", rebalanceReplicationTimeout="
        + rebalanceReplicationTimeout
        + ", rebalanceMaxTransferAttempts="
        + rebalanceMaxTransferAttempts
        + ", receiveOnLegacySubject="
        + receiveOnLegacySubject
        + ", snapshotInterval="
        + snapshotInterval
        + ", maxSnapshotCount="
        + maxSnapshotCount
        + ", snapshotEntryTriggerThreshold="
        + snapshotEntryTriggerThreshold
        + ", snapshotMergeAwaitTimeout="
        + snapshotMergeAwaitTimeout
        + ", priorityDecayGap="
        + priorityDecayGap
        + '}';
  }
}
