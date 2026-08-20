/*
 * Copyright 2018-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.utils.memory.MemorySize;
import java.time.Duration;

/** 分区配置（含选举、复制、存储） */
public class RaftPartitionConfig {

  private static final Duration DEFAULT_ELECTION_TIMEOUT = Duration.ofMillis(2500);
  private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMillis(250);
  private static final boolean DEFAULT_PRIORITY_ELECTION = true;
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final int DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT = 3;
  private static final Duration DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT = Duration.ofSeconds(0);
  private static final int DEFAULT_SNAPSHOT_REPLICATION_THRESHOLD = 100;
  private static final String DATA_PREFIX = ".data";
  private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
  private static final boolean DEFAULT_FLUSH_EXPLICITLY = true;
  private static final long DEFAULT_FREE_DISK_SPACE = 1024L * 1024 * 1024;
  private static final int DEFAULT_JOURNAL_INDEX_DENSITY = 100;

  // ===== 选举与复制 =====
  private Duration electionTimeout = DEFAULT_ELECTION_TIMEOUT;
  private Duration heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
  private int maxAppendsPerFollower = 2;
  private int maxAppendBatchSize = 32 * 1024;
  private boolean priorityElectionEnabled = DEFAULT_PRIORITY_ELECTION;
  private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  private int minStepDownFailureCount = DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT;
  private Duration maxQuorumResponseTimeout = DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT;
  private int preferSnapshotReplicationThreshold = DEFAULT_SNAPSHOT_REPLICATION_THRESHOLD;

  // ===== 存储 =====
  private String directory;
  private long segmentSize = DEFAULT_MAX_SEGMENT_SIZE;
  private boolean flushExplicitly = DEFAULT_FLUSH_EXPLICITLY;
  private long freeDiskSpace = DEFAULT_FREE_DISK_SPACE;
  private int journalIndexDensity = DEFAULT_JOURNAL_INDEX_DENSITY;

  public Duration getElectionTimeout() {
    return electionTimeout;
  }

  public RaftPartitionConfig setElectionTimeout(final Duration electionTimeout) {
    this.electionTimeout = electionTimeout;
    return this;
  }

  public Duration getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public RaftPartitionConfig setHeartbeatInterval(final Duration heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
    return this;
  }

  public int getMaxAppendsPerFollower() {
    return maxAppendsPerFollower;
  }

  public RaftPartitionConfig setMaxAppendsPerFollower(final int maxAppendsPerFollower) {
    this.maxAppendsPerFollower = maxAppendsPerFollower;
    return this;
  }

  public int getMaxAppendBatchSize() {
    return maxAppendBatchSize;
  }

  public RaftPartitionConfig setMaxAppendBatchSize(final int maxAppendBatchSize) {
    this.maxAppendBatchSize = maxAppendBatchSize;
    return this;
  }

  public boolean isPriorityElectionEnabled() {
    return priorityElectionEnabled;
  }

  public RaftPartitionConfig setPriorityElectionEnabled(final boolean enable) {
    priorityElectionEnabled = enable;
    return this;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public RaftPartitionConfig setRequestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public int getMinStepDownFailureCount() {
    return minStepDownFailureCount;
  }

  public RaftPartitionConfig setMinStepDownFailureCount(final int minStepDownFailureCount) {
    this.minStepDownFailureCount = minStepDownFailureCount;
    return this;
  }

  public Duration getMaxQuorumResponseTimeout() {
    return maxQuorumResponseTimeout;
  }

  public RaftPartitionConfig setMaxQuorumResponseTimeout(
      final Duration maxQuorumResponseTimeout) {
    this.maxQuorumResponseTimeout = maxQuorumResponseTimeout;
    return this;
  }

  public int getPreferSnapshotReplicationThreshold() {
    return preferSnapshotReplicationThreshold;
  }

  public RaftPartitionConfig setPreferSnapshotReplicationThreshold(
      final int preferSnapshotReplicationThreshold) {
    this.preferSnapshotReplicationThreshold = preferSnapshotReplicationThreshold;
    return this;
  }

  // ===== 存储配置 =====

  /** 分区数据目录 */
  public String getDirectory(final String groupName) {
    return directory != null
        ? directory
        : System.getProperty("atomix.data", DATA_PREFIX) + "/" + groupName;
  }

  public RaftPartitionConfig setDirectory(final String directory) {
    this.directory = directory;
    return this;
  }

  /** 日志段大小 */
  public MemorySize getSegmentSize() {
    return MemorySize.from(segmentSize);
  }

  public RaftPartitionConfig setSegmentSize(final MemorySize segmentSize) {
    this.segmentSize = segmentSize.bytes();
    return this;
  }

  /** 是否显式刷盘保证正确性（follower 每次追加刷、leader 提交时刷） */
  public boolean shouldFlushExplicitly() {
    return flushExplicitly;
  }

  public RaftPartitionConfig setFlushExplicitly(final boolean flushExplicitly) {
    this.flushExplicitly = flushExplicitly;
    return this;
  }

  /** 分配新日志段时的最小剩余磁盘空间 */
  public long getFreeDiskSpace() {
    return freeDiskSpace;
  }

  public RaftPartitionConfig setFreeDiskSpace(final long freeDiskSpace) {
    this.freeDiskSpace = freeDiskSpace;
    return this;
  }

  /** 日志索引密度 */
  public int getJournalIndexDensity() {
    return journalIndexDensity;
  }

  public RaftPartitionConfig setJournalIndexDensity(final int journalIndexDensity) {
    this.journalIndexDensity = journalIndexDensity;
    return this;
  }
}
