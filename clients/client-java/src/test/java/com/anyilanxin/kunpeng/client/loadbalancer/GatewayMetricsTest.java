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
package com.anyilanxin.kunpeng.client.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic tests for {@link GatewayMetrics}: scoring by active-connections and client-observed
 * latency, error penalty, and the online/offline flag. No gRPC channel/subchannel mocks required.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class GatewayMetricsTest {

  @Test
  void scoreDecreasesAsActiveConnectionsIncreases() {
    final GatewayMetrics light = new GatewayMetrics();
    final GatewayMetrics heavy = new GatewayMetrics();
    light.setActiveConnections(0);
    heavy.setActiveConnections(20);
    assertThat(light.score()).isGreaterThan(heavy.score());
  }

  @Test
  void scoreDecreasesAsLatencyIncreases() {
    final GatewayMetrics fast = new GatewayMetrics();
    final GatewayMetrics slow = new GatewayMetrics();
    // Force the slow metrics EWMA toward a high value via a fake recordStream with old startNano.
    for (int i = 0; i < 5; i++) {
      final long start = System.nanoTime() - 1_000_000_000L; // ~1s latency
      slow.recordStream(start, Status.OK);
    }
    assertThat(slow.latencyEwmaMs()).isGreaterThan(fast.latencyEwmaMs());
    assertThat(fast.score()).isGreaterThan(slow.score());
  }

  @Test
  void recentErrorAppliesPenalty() {
    final GatewayMetrics clean = new GatewayMetrics();
    final GatewayMetrics failed = new GatewayMetrics();
    failed.recordStream(System.nanoTime(), Status.UNAVAILABLE.withDescription("conn refused"));
    assertThat(failed.score()).isLessThan(clean.score());
  }

  @Test
  void okStatusDoesNotApplyErrorPenalty() {
    final GatewayMetrics okMetrics = new GatewayMetrics();
    final GatewayMetrics failedMetrics = new GatewayMetrics();
    okMetrics.recordStream(System.nanoTime(), Status.OK);
    failedMetrics.recordStream(System.nanoTime(), Status.UNAVAILABLE);
    // OK updates latency but must NOT apply the 0.1x error penalty.
    assertThat(okMetrics.score()).isGreaterThan(failedMetrics.score() * 5);
  }

  @Test
  void latencyAffectsScoreEvenWithZeroActiveConnections() {
    final GatewayMetrics metrics = new GatewayMetrics();
    final double before = metrics.score();
    metrics.recordStream(System.nanoTime() - 1_000_000_000L, Status.OK);
    // Higher latency lowers the score even when activeConnections stays at zero.
    assertThat(metrics.score()).isLessThan(before);
  }

  @Test
  void onlineFlagDefaultsTrueAndIsMutable() {
    final GatewayMetrics metrics = new GatewayMetrics();
    assertThat(metrics.online()).isTrue();
    metrics.setOnline(false);
    assertThat(metrics.online()).isFalse();
  }
}
