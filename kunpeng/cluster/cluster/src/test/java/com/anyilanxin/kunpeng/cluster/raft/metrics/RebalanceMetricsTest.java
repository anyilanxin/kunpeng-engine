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
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class RebalanceMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final RebalanceMetrics metrics = new RebalanceMetrics("group-1", registry);

  @Test
  void shouldExposePausedGaugeReflectingTransferState() {
    // given
    assertThat(pausedGauge()).as("gauge starts cleared").isZero();

    // when
    metrics.setPartitionPaused(true);

    // then
    assertThat(pausedGauge()).isEqualTo(1.0);

    // when
    metrics.setPartitionPaused(false);

    // then
    assertThat(pausedGauge()).isZero();
  }

  @Test
  void shouldRecordPauseDuration() {
    // when
    metrics.observePauseDuration(Duration.ofMillis(250));

    // then
    final var timer = registry.find("zeebe_cluster_rebalance_partition_pause_duration").timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(250);
  }

  private double pausedGauge() {
    return registry.get("zeebe_cluster_rebalance_partition_paused").gauge().value();
  }
}
