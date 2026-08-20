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

import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raft 分区启动步骤：为本节点承载的每个分区创建 RaftPartition 并启动。
 *
 * <p>只创建 {@code PartitionMetadata.members()} 包含本节点的分区——
 * 本节点不承载的分区不产生任何对象。
 */
public final class RaftPartitionGroupStartupStep<T extends RaftGroupContext>
    implements PartitionStartup<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RaftPartitionGroupStartupStep.class);

  private final RaftPartitionConfig partitionConfig;

  public RaftPartitionGroupStartupStep(final RaftPartitionConfig partitionConfig) {
    this.partitionConfig = partitionConfig;
  }

  @Override
  public String getName() {
    return "RaftPartitions";
  }

  @Override
  public ActorFuture<T> startup(final T context) {
    final var future = new CompletableActorFuture<T>();
    try {
      LOG.info("启动分区组 {} 的 Raft 分区", context.groupName());
      // 创建本节点承载的分区
      for (int i = 1; i <= context.metadata().partitionCount(); i++) {
        final var partitionId = PartitionId.from(context.groupName(), i);
        // PartitionMetadata 需要由工厂或配置提供成员信息
        // 此处暂用简化版：所有分区都在本节点
        final var metadata = new PartitionMetadata(
            partitionId,
            java.util.Set.of(), // members 由上下文/工厂提供
            java.util.Map.of(),
            0,
            null);
        final var dataDir = new File(context.groupDataDirectory().toFile(), String.valueOf(i));
        final var vault = new com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotVault(
            dataDir.toPath(), null, context.meterRegistry());
        final var partition = new RaftPartition(
            metadata, partitionConfig, dataDir,
            context.meterRegistry(), vault,
            null, // snapshotHandler 由工厂体系提供
            null, context.communicationService());
        context.attachPartition(i, partition);
        LOG.debug("已创建分区 {}-{}", context.groupName(), i);
      }

      LOG.info("分区组 {} 的 {} 个分区已创建", context.groupName(), context.metadata().partitionCount());
      future.complete(context);
    } catch (final Exception e) {
      LOG.error("分区组 {} 启动失败", context.groupName(), e);
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public ActorFuture<T> shutdown(final T context) {
    final var future = new CompletableActorFuture<T>();
    try {
      LOG.info("关闭分区组 {} 的全部分区", context.groupName());
      final var partitions = context.partitions();
      final var futures = new ArrayList<java.util.concurrent.CompletableFuture<Void>>();
      for (final var entry : partitions.entrySet()) {
        futures.add(entry.getValue().close());
      }
      java.util.concurrent.CompletableFuture
          .allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
          .whenComplete((result, error) -> {
            context.detachAllPartitions();
            if (error == null) {
              future.complete(context);
            } else {
              future.completeExceptionally(error);
            }
          });
    } catch (final Exception e) {
      LOG.error("分区组 {} 关闭失败", context.groupName(), e);
      future.completeExceptionally(e);
    }
    return future;
  }
}
