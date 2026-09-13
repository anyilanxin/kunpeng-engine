/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.configuration.broker;

import java.time.Duration;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.unit.DataSize;

/** raft 基础配置，定义心跳间隔、选举超时、快照与请求超时等参数。 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class RaftCfg implements ConfigurationEntry {
  private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMillis(250);
  public static final Duration DEFAULT_ELECTION_TIMEOUT = Duration.ofMillis(2500);
  public static final int DEFAULT_MAX_APPENDS_PER_FOLLOWER = 6;
  public static final boolean DEFAULT_ENABLE_PRIORITY_ELECTION = true;
  private static final FlushConfig DEFAULT_FLUSH_CONFIG = new FlushConfig(true, Duration.ZERO);
  public static final DataSize DEFAULT_MAX_APPEND_BATCH_SIZE = DataSize.ofKilobytes(32);
  private boolean enablePriorityElection = DEFAULT_ENABLE_PRIORITY_ELECTION;
  public static final Duration DEFAULT_SNAPSHOT_REQUEST_TIMEOUT = Duration.ofMillis(2500);
  public static final DataSize DEFAULT_SNAPSHOT_CHUNK_SIZE = DataSize.ofGigabytes(1);
  private static final Duration DEFAULT_CONFIGURATION_CHANGE_TIMEOUT = Duration.ofSeconds(10);
  // Requests should time out faster than the election timeout to ensure that a single missed
  // heartbeat does not cause immediate re-election.
  public static final DataSize DEFAULT_MAX_MESSAGE_SIZE = DataSize.ofMegabytes(4);
  private static final Duration DEFAULT_REQUEST_TIMEOUT = DEFAULT_ELECTION_TIMEOUT;
  private static final Duration DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT = Duration.ofSeconds(0);
  private static final int DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT = 3;
  private static final int DEFAULT_PREFER_SNAPSHOT_REPLICATION_THRESHOLD = 100;
  private static final boolean DEFAULT_PREALLOCATE_SEGMENT_FILES = true;
  private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  private Duration snapshotRequestTimeout = DEFAULT_SNAPSHOT_REQUEST_TIMEOUT;
  private DataSize snapshotChunkSize = DEFAULT_SNAPSHOT_CHUNK_SIZE;
  private Duration configurationChangeTimeout = DEFAULT_CONFIGURATION_CHANGE_TIMEOUT;
  private Duration maxQuorumResponseTimeout = DEFAULT_MAX_QUORUM_RESPONSE_TIMEOUT;
  private int minStepDownFailureCount = DEFAULT_MIN_STEP_DOWN_FAILURE_COUNT;
  private int preferSnapshotReplicationThreshold = DEFAULT_PREFER_SNAPSHOT_REPLICATION_THRESHOLD;
  private int maxAppendsPerFollower = DEFAULT_MAX_APPENDS_PER_FOLLOWER;
  private boolean preallocateSegmentFiles = DEFAULT_PREALLOCATE_SEGMENT_FILES;
  private FlushConfig flush = DEFAULT_FLUSH_CONFIG;
  private DataSize maxAppendBatchSize = DEFAULT_MAX_APPEND_BATCH_SIZE;
  private Duration electionTimeout = DEFAULT_ELECTION_TIMEOUT;
  private Duration heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
  private DataSize maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

  /** 刷盘配置，定义是否启用刷盘及其延迟时间。 */
  public record FlushConfig(boolean enabled, Duration delayTime) {
    public FlushConfig(final boolean enabled, final Duration delayTime) {
      this.enabled = enabled;
      this.delayTime = delayTime == null ? Duration.ZERO : delayTime;
    }
  }

  public long getMaxAppendBatchSizeInBytes() {
    return Optional.ofNullable(maxAppendBatchSize).orElse(DEFAULT_MAX_APPEND_BATCH_SIZE).toBytes();
  }

  public long getMaxMessageSizeInBytes() {
    return maxMessageSize.toBytes();
  }
}
