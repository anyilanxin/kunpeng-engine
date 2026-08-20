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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.RaftCommitListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raft 分区启动步骤：为本节点承载的每个分区创建 RaftPartition 并注册工厂监听器。
 *
 * <p>分区参数由 {@link RaftGroupFactory} 提供；其中分区配置与快照处理器为必传项，
 * 启动时校验，未提供直接抛出异常。
 *
 * @param <T> 分区组上下文类型
 */
public final class RaftPartitionGroupStartupStep<T extends RaftGroupContext>
    implements PartitionStartup<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RaftPartitionGroupStartupStep.class);

  private final RaftGroupFactory factory;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicationService;
  private final ManagedPartitionTopologyService topologyService;
  private final MeterRegistry meterRegistry;
  private final Map<Integer, RaftPartition> partitionRegistry;
  private final NodePartitionMetadata groupMeta;

  public RaftPartitionGroupStartupStep(
      final RaftGroupFactory factory,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicationService,
      final ManagedPartitionTopologyService topologyService,
      final MeterRegistry meterRegistry,
      final Map<Integer, RaftPartition> partitionRegistry,
      final NodePartitionMetadata groupMeta) {
    this.factory = factory;
    this.membershipService = membershipService;
    this.communicationService = communicationService;
    this.topologyService = topologyService;
    this.meterRegistry = meterRegistry;
    this.partitionRegistry = partitionRegistry;
    this.groupMeta = groupMeta;
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
      final var partitionConfig = factory.getRaftPartitionConfig();
      if (partitionConfig == null) {
        throw new IllegalStateException("RaftGroupFactory 未提供分区配置: " + context.groupName());
      }
      for (int i = 1; i <= groupMeta.partitionCount(); i++) {
        final var partitionId = PartitionId.from(context.groupName(), i);
        final var snapshotHandler = provideSnapshotHandler(partitionId);
        final var partition = new RaftPartition(
            partitionMetadata(partitionId),
            partitionConfig,
            new File(context.groupDataDirectory().toFile(), String.valueOf(i)),
            meterRegistry,
            snapshotHandler,
            membershipService,
            communicationService,
            topologyService);
        registerListeners(partition, partitionId);
        partitionRegistry.put(i, partition);
        LOG.debug("已创建分区 {}-{}", context.groupName(), i);
      }
      LOG.info("分区组 {} 的 {} 个分区已创建", context.groupName(), groupMeta.partitionCount());
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
      final var futures = new ArrayList<CompletableFuture<Void>>();
      for (final var partition : partitionRegistry.values()) {
        // 先停周期快照，再拍终局快照并关闭业务资源，最后停 raft server
        partition.stopSnapshotSchedule();
        futures.add(partition.closeHandler().thenCompose(v -> partition.close()));
      }
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .whenComplete((result, error) -> {
            partitionRegistry.clear();
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

  // ===== 内部 =====

  private SnapshotHandler provideSnapshotHandler(final PartitionId partitionId) {
    final var supplier = factory.getSnapshotHandler(partitionId);
    final var handler = supplier != null ? supplier.get() : null;
    if (handler == null) {
      throw new IllegalStateException("RaftGroupFactory 未提供快照处理器: " + partitionId);
    }
    return handler;
  }

  private void registerListeners(final RaftPartition partition, final PartitionId partitionId) {
    supplySet(factory.getRoleChangeListeners(partitionId)).forEach(partition::addRoleChangeListener);
    supplySet(factory.getFailureListeners(partitionId)).forEach(partition::addFailureListener);
    supplySet(factory.getCommitListeners(partitionId)).forEach(partition::addCommitListener);
  }

  /** 分区元数据（成员等由工厂/配置后续提供；此处为空集） */
  private static PartitionMetadata partitionMetadata(final PartitionId partitionId) {
    return new PartitionMetadata(partitionId, Set.of(), Map.of(), 0, null);
  }

  private static <E> Set<E> supplySet(final Supplier<Set<E>> supplier) {
    final var set = supplier != null ? supplier.get() : null;
    return set != null ? set : Set.of();
  }
}
