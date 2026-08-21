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

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

/** 校验 {@link LeaderAppenderMetrics} 中复制延迟 gauge 的命名、标签与生命周期行为。 */
final class LeaderAppenderMetricsTest {

  private static final String GAUGE_NAME = "zeebe_raft_replication_lag_bytes";
  private static final String TENANT = "acme";
  private static final String PARTITION_TAG = "7";

  @AutoClose private final MeterRegistry registry = new SimpleMeterRegistry();
  private final LeaderAppenderMetrics victim = new LeaderAppenderMetrics("acme-7", registry);

  private double lagOf(final String follower) {
    return registry.get(GAUGE_NAME).tag("follower", follower).gauge().value();
  }

  @Test
  void registersLagGaugeUnderPartitionAndTenantTags() {
    // when：上报一次某个 follower 的延迟字节数
    victim.observeReplicationLagBytes("node-a", 8192);

    // then：gauge 命名与标签均按租户/分区/follower 维度注册，值等于上报值
    assertThat(
            registry
                .get(GAUGE_NAME)
                .tag("partition", PARTITION_TAG)
                .tag("physicalTenant", TENANT)
                .tag("follower", "node-a")
                .gauge()
                .value())
        .isEqualTo(8192.0);
  }

  @Test
  void overwritesLagGaugeWithLatestObservation() {
    // given：先上报一个较大的延迟
    victim.observeReplicationLagBytes("node-a", 16384);

    // when：同一 follower 延迟下降
    victim.observeReplicationLagBytes("node-a", 256);

    // then：gauge 反映最新一次观测
    assertThat(lagOf("node-a")).isEqualTo(256.0);
  }

  @Test
  void keepsSeparateGaugePerFollower() {
    // when：两个 follower 各自上报
    victim.observeReplicationLagBytes("node-a", 4096);
    victim.observeReplicationLagBytes("node-b", 512);

    // then：各自的 gauge 互不覆盖
    assertThat(lagOf("node-a")).isEqualTo(4096.0);
    assertThat(lagOf("node-b")).isEqualTo(512.0);
  }

  @Test
  void removesAllLagGaugesWhenClosed() {
    // given
    victim.observeReplicationLagBytes("node-a", 4096);

    // when：关闭指标对象
    victim.close();

    // then：注册表中不再存在该 gauge
    assertThat(registry.find(GAUGE_NAME).gauge()).isNull();
  }
}
