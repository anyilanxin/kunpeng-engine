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
package io.atomix.raft.partition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.atomix.cluster.PartitionId;
import io.atomix.primitive.partition.PartitionMetadata;
import io.atomix.raft.partition.impl.RaftPartitionServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 针对 {@link RaftPartition#stepDownForLeaderBalancing()} 与 {@link RaftPartition#stepDown()} 的行为验证。 */
final class StepDownTest {

  private static final String GROUP_NAME = "core-group";

  @AutoClose MeterRegistry registry = new SimpleMeterRegistry();

  /** 构造一个指定优先级选举开关、且尚未启动 server 的分区实例。 */
  private RaftPartition newPartition(final Path dir, final boolean priorityElectionEnabled) {
    final var id = new PartitionId(GROUP_NAME, 3);
    final var meta = new PartitionMetadata(id, Set.of(), Map.of(), 1, null);
    final var cfg = new RaftPartitionConfig();
    cfg.setPriorityElectionEnabled(priorityElectionEnabled);
    return new RaftPartition(meta, cfg, dir.toFile(), registry);
  }

  /** 通过反射把一个 mock server 塞进分区，免去真正启动服务的开销。 */
  private RaftPartitionServer injectMockServer(final RaftPartition partition) throws Exception {
    final var server = mock(RaftPartitionServer.class);
    when(server.stepDown()).thenReturn(CompletableFuture.completedFuture(null));
    final Field field = RaftPartition.class.getDeclaredField("server");
    field.setAccessible(true);
    field.set(partition, server);
    return server;
  }

  @Test
  void leaderBalancingStepsDownWhenPriorityElectionOn(@TempDir final Path dir) throws Exception {
    // given：开启优先级选举，并注入一个可调用的 mock server
    final var partition = newPartition(dir, true);
    final var server = injectMockServer(partition);

    // when：触发 leader 均衡让位
    partition.stepDownForLeaderBalancing();

    // then：让位请求被下发给 server
    verify(server).stepDown();
  }

  @Test
  void leaderBalancingSkipsStepDownWhenPriorityElectionOff(@TempDir final Path dir)
      throws Exception {
    // given：关闭优先级选举
    final var partition = newPartition(dir, false);
    final var server = injectMockServer(partition);

    // when
    partition.stepDownForLeaderBalancing();

    // then：不会调用 server 的 stepDown
    verify(server, never()).stepDown();
  }

  @Test
  void leaderBalancingIsNoOpWithoutServer(@TempDir final Path dir) {
    // given：分区未启动，server 字段为 null
    final var partition = newPartition(dir, true);

    // when
    final var future = partition.stepDownForLeaderBalancing();

    // then：不做任何事且正常完成
    assertThat(future).isCompletedWithValue(null);
  }

  @Test
  void directStepDownIgnoresPriorityElectionFlag(@TempDir final Path dir) throws Exception {
    // given：即便优先级选举被关闭，直接 stepDown 也应生效
    final var partition = newPartition(dir, false);
    final var server = injectMockServer(partition);

    // when
    partition.stepDown();

    // then：无条件调用 server.stepDown
    verify(server).stepDown();
  }
}
