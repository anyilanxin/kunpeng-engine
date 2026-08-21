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
package io.atomix.raft.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.atomix.cluster.MemberId;
import io.atomix.raft.RebalanceConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class LeadershipTransferInitiateRequestTest {

  private static final RebalanceConfiguration CONFIGURED =
      new RebalanceConfiguration(8 * 1024 * 1024, Duration.ofSeconds(10), 3);

  @Test
  void shouldKeepTheConfiguredSettingsWhenTheCoordinatorOverridesNothing() {
    // given
    final var request = requestBuilder().build();

    // when
    final var effective = request.effectiveConfiguration(CONFIGURED);

    // then
    assertThat(effective).isEqualTo(CONFIGURED);
  }

  @Test
  void shouldApplyEveryOverrideTheCoordinatorSent() {
    // given
    final var request =
        requestBuilder()
            .withReplicationLagThreshold(4096)
            .withReplicationTimeout(Duration.ofSeconds(30))
            .withMaxTransferAttempts(7)
            .build();

    // when
    final var effective = request.effectiveConfiguration(CONFIGURED);

    // then
    assertThat(effective).isEqualTo(new RebalanceConfiguration(4096, Duration.ofSeconds(30), 7));
  }

  @Test
  void shouldKeepTheConfiguredSettingsTheCoordinatorLeftUnset() {
    // given
    final var request = requestBuilder().withMaxTransferAttempts(7).build();

    // when
    final var effective = request.effectiveConfiguration(CONFIGURED);

    // then
    assertThat(effective)
        .isEqualTo(
            new RebalanceConfiguration(
                CONFIGURED.replicationLagThreshold(), CONFIGURED.replicationTimeout(), 7));
  }

  @Test
  void shouldRejectANegativeReplicationLagThresholdOverride() {
    // given / when / then
    assertThatThrownBy(() -> requestBuilder().withReplicationLagThreshold(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectANonPositiveReplicationTimeoutOverride() {
    // given / when / then
    assertThatThrownBy(() -> requestBuilder().withReplicationTimeout(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> requestBuilder().withReplicationTimeout(Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectANonPositiveMaxTransferAttemptsOverride() {
    // given / when / then
    assertThatThrownBy(() -> requestBuilder().withMaxTransferAttempts(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static LeadershipTransferInitiateRequest.Builder requestBuilder() {
    return LeadershipTransferInitiateRequest.builder()
        .withDesiredLeader(MemberId.from("2"))
        .withCoordinator(MemberId.from("1"))
        .withCoordinatorConfigVersion(4)
        .withCorrelationId(0x5eed_0b01L);
  }
}
