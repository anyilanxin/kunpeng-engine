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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class SnapshotEntryTriggerTest {

  @Test
  void firesOnlyWhenDeltaReachesThreshold() {
    final var trigger = new SnapshotEntryTrigger(3, 5);

    assertThat(trigger.tryFire(6)).as("增量 1，未达阈值").isFalse();
    assertThat(trigger.tryFire(7)).as("增量 2，未达阈值").isFalse();
    assertThat(trigger.tryFire(8)).as("增量 3，达到阈值").isTrue();
  }

  @Test
  void rearmsOnlyAfterAnotherFullThreshold() {
    final var trigger = new SnapshotEntryTrigger(3, 0);

    assertThat(trigger.tryFire(3)).isTrue();
    assertThat(trigger.tryFire(5)).as("触发后增量 2，不应再触发").isFalse();
    assertThat(trigger.tryFire(6)).as("触发后增量 3，应再次触发").isTrue();
  }

  @Test
  void zeroThresholdDisablesFiring() {
    final var trigger = new SnapshotEntryTrigger(0, 0);

    assertThat(trigger.tryFire(1)).isFalse();
    assertThat(trigger.tryFire(1_000_000)).isFalse();
  }

  @Test
  void seedFromExistingSnapshotAvoidsImmediateFire() {
    // 重启场景：磁盘已有快照 index=100，种子后应从 100 起算增量，而不是从 0
    final var trigger = new SnapshotEntryTrigger(10, 100);

    assertThat(trigger.tryFire(105)).as("增量 5，未达阈值").isFalse();
    assertThat(trigger.tryFire(110)).as("增量 10，达到阈值").isTrue();
  }
}
