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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionGroup;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raft 分区组核心启动步骤。
 *
 * <p>位于启动步骤列表的中间：之前的是前置启动（此时 partitionGroup 尚未创建），
 * 之后的是后置启动（可访问 {@code context.partitionGroup()}）。
 *
 * <p>启动：构造 RaftPartitionGroup → bootstrap → 附加到上下文。
 * 关闭：从上下文分离 → 关闭全部分区。
 */
public final class RaftPartitionGroupStartupStep<T extends RaftGroupContext>
    implements PartitionStartup<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RaftPartitionGroupStartupStep.class);

  @Override
  public String getName() {
    return "RaftPartitionGroup";
  }

  @Override
  public ActorFuture<T> startup(final T context) {
    final var future = new CompletableActorFuture<T>();
    try {
      LOG.info("启动 Raft 分区组: {}", context.groupName());
      final var group = RaftPartitionGroup.builder(context.groupName())
          .withNumPartitions(context.metadata().partitionCount())
          .withPartitionSize(context.metadata().replicationFactor())
          .withMeterRegistry(context.meterRegistry())
          .build();
      context.attachPartitionGroup(group);
      LOG.info("Raft 分区组已启动: {} (partitions={})",
          context.groupName(), context.metadata().partitionCount());
      future.complete(context);
    } catch (final Exception e) {
      LOG.error("Raft 分区组启动失败: {}", context.groupName(), e);
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public ActorFuture<T> shutdown(final T context) {
    final var future = new CompletableActorFuture<T>();
    try {
      if (context.isPartitionGroupStarted()) {
        LOG.info("关闭 Raft 分区组: {}", context.groupName());
        final var group = context.partitionGroup();
        group.close();
        context.detachPartitionGroup();
      }
      future.complete(context);
    } catch (final Exception e) {
      LOG.error("Raft 分区组关闭失败: {}", context.groupName(), e);
      future.completeExceptionally(e);
    }
    return future;
  }
}
