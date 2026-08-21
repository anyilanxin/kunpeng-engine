/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.raft.metrics;

import static io.atomix.raft.metrics.RebalanceMetricsDoc.PARTITION_PAUSED;
import static io.atomix.raft.metrics.RebalanceMetricsDoc.PARTITION_PAUSE_DURATION;
import static io.atomix.raft.metrics.RebalanceMetricsDoc.PARTITION_TRANSFER_DURATION;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.atomix.raft.LeadershipTransferResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;

/** 协作式 Leader 转移（rebalance）相关指标采集 */
public class RebalanceMetrics extends RaftMetrics {

  private final MeterRegistry meterRegistry;
  private final Timer partitionPauseDuration;
  private final SettableGauge partitionPaused;

  public RebalanceMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    this.meterRegistry = meterRegistry;
    partitionPauseDuration =
        Micrometers.timer(
            PARTITION_PAUSE_DURATION,
            meterRegistry,
            "partition",
            partition,
            "physicalTenant",
            partitionGroupName);
    partitionPaused =
        Micrometers.gauge(
            PARTITION_PAUSED,
            meterRegistry,
            "partition",
            partition,
            "physicalTenant",
            partitionGroupName);
  }

  /** 记录一次分区暂停的持续时长 */
  public void observePauseDuration(final Duration duration) {
    partitionPauseDuration.record(duration);
  }

  /** 记录分区是否因 Leader 转移而暂停 */
  public void setPartitionPaused(final boolean paused) {
    partitionPaused.set(paused ? 1 : 0);
  }

  /** 记录一次 Leader 转移尝试的耗时（按结果打标签） */
  public void observeTransferDuration(
      final LeadershipTransferResult result, final Duration duration) {
    Micrometers.timer(
            PARTITION_TRANSFER_DURATION,
            meterRegistry,
            "result",
            result.name(),
            "partition",
            partition,
            "physicalTenant",
            partitionGroupName)
        .record(duration);
  }
}
