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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class RaftPartitionConfigTest {

  @Test
  void hasSnapshotDefaults() {
    final var config = new RaftPartitionConfig();
    assertThat(config.getSnapshotInterval()).isEqualTo(Duration.ofMinutes(5));
    assertThat(config.getMaxSnapshotCount()).isEqualTo(1);
    assertThat(config.getSnapshotEntryTriggerThreshold()).isEqualTo(100_000);
    assertThat(config.getSnapshotTransferMaxBatchSize()).isEqualTo(4 * 1024 * 1024);
  }

  @Test
  void allowsOverridingSnapshotSettings() {
    final var config = new RaftPartitionConfig();
    config.setSnapshotInterval(Duration.ofSeconds(30));
    config.setMaxSnapshotCount(1);
    config.setSnapshotEntryTriggerThreshold(50_000);
    config.setSnapshotTransferMaxBatchSize(1024 * 1024);
    assertThat(config.getSnapshotInterval()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.getMaxSnapshotCount()).isEqualTo(1);
    assertThat(config.getSnapshotEntryTriggerThreshold()).isEqualTo(50_000);
    assertThat(config.getSnapshotTransferMaxBatchSize()).isEqualTo(1024 * 1024);
  }

  /** 0 是合法取值：禁用条数触发，仅保留周期触发。 */
  @Test
  void allowsDisablingSnapshotEntryTrigger() {
    final var config = new RaftPartitionConfig();
    config.setSnapshotEntryTriggerThreshold(0);
    assertThat(config.getSnapshotEntryTriggerThreshold()).isZero();
  }

  @Test
  void hasPriorityDecayGapDefaultOfOne() {
    final var config = new RaftPartitionConfig();
    assertThat(config.getPriorityDecayGap()).isEqualTo(1);

    config.setPriorityDecayGap(10);
    assertThat(config.getPriorityDecayGap()).isEqualTo(10);
  }
}
