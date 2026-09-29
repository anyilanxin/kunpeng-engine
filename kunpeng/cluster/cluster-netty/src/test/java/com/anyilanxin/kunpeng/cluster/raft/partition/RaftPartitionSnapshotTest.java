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

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.RUNTIME_DIRECTORY;
import static org.assertj.core.api.Assertions.assertThat;

/** 未启动分区上 {@link RaftPartition#takeSnapshot()} 与 {@link RaftPartition#close()} 的行为。 */
final class RaftPartitionSnapshotTest {

  @AutoClose MeterRegistry registry = new SimpleMeterRegistry();

  private RaftPartition newPartition(final Path dir) {
    final var id = new PartitionId("core-group", 3);
    final var meta = new PartitionMetadata(id, Set.of(), Map.of(), 1, null);
    return new RaftPartition(
      meta, new RaftPartitionConfig(), dir, dir.resolve(RUNTIME_DIRECTORY), registry, null, null, null);
  }

  @Test
  void takeSnapshotFailsWhenNotStarted(@TempDir final Path dir) {
    final var partition = newPartition(dir);
    assertThat(partition.takeSnapshot()).isCompletedExceptionally();
  }

  @Test
  void closeCompletesWhenNotStarted(@TempDir final Path dir) {
    final var partition = newPartition(dir);
    assertThat(partition.close()).isCompletedWithValue(null);
  }
}
