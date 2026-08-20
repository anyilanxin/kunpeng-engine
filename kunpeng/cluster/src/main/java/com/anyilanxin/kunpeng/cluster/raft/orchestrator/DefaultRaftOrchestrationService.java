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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 Raft 编排服务实现。
 *
 * <p>启动顺序：分区拓扑服务 → 分区管理器（actor）→ 逐组启动分区。
 * 停止倒序：停止全部分区组 → 关闭管理器 actor → 停止拓扑服务 → 持久化元数据。
 *
 * <p>磁盘元数据（{@code .raft-meta}）：有则按记录恢复全部分区组；无则按传入
 * {@link RaftOrchestrationConfig} 初始化并写盘。
 */
public final class DefaultRaftOrchestrationService implements ManagedRaftOrchestrationService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultRaftOrchestrationService.class);
  private static final Executor DIRECT = Runnable::run;

  private final Map<String, RaftGroupFactory> factories;
  private final Path dataRoot;
  private final MeterRegistry meterRegistry;
  private final ClusterMembershipService membershipService;
  private final MessagingService messagingService;
  private final ClusterCommunicationService communicationService;
  private final ActorScheduler actorScheduler;
  private final RaftOrchestrationConfig startupConfig;
  private final NodeMetadataStore metadataStore;

  private volatile DefaultPartitionTopologyService topologyService;
  private volatile DefaultPartitionManager partitionManager;
  private volatile boolean started;

  public DefaultRaftOrchestrationService(
      final Map<String, RaftGroupFactory> factories,
      final Path dataRoot,
      final MeterRegistry meterRegistry,
      final ClusterMembershipService membershipService,
      final MessagingService messagingService,
      final ClusterCommunicationService communicationService,
      final ActorScheduler actorScheduler,
      final RaftOrchestrationConfig startupConfig) {
    this.factories = factories;
    this.dataRoot = dataRoot;
    this.meterRegistry = meterRegistry;
    this.membershipService = membershipService;
    this.messagingService = messagingService;
    this.communicationService = communicationService;
    this.actorScheduler = actorScheduler;
    this.startupConfig = startupConfig;
    this.metadataStore = new NodeMetadataStore(dataRoot);
  }

  // ===== Managed 生命周期 =====

  @Override
  public CompletableFuture<RaftOrchestrationService> start() {
    final var future = new CompletableFuture<RaftOrchestrationService>();
    try {
      Files.createDirectories(metadataStore.dataDirectory());
      // 先启动分区拓扑服务：分区启动后的角色变化才能被记录并广播
      final var topology = new DefaultPartitionTopologyService(membershipService);
      topology.start().join();
      topologyService = topology;
      // 启动分区管理器 actor
      final var manager = new DefaultPartitionManager(
          factories, metadataStore, membershipService, messagingService,
          communicationService, topology, meterRegistry);
      partitionManager = manager;
      actorScheduler.submitActor(manager).join();

      final var localMeta = metadataStore.load();
      final List<NodePartitionMetadata> groupsToStart;
      if (localMeta.isPresent()) {
        LOG.info("按本地元数据恢复 {} 个分区组", localMeta.get().size());
        groupsToStart = List.copyOf(localMeta.get().values());
      } else {
        LOG.info("首次启动，按传入参数初始化 {} 个分区组", startupConfig.partitionGroups().size());
        groupsToStart = startupConfig.partitionGroups().stream()
            .map(cfg -> new NodePartitionMetadata(
                cfg.groupName(), cfg.groupType(),
                cfg.partitionCount(), cfg.replicationFactor(),
                cfg.localPartitions(), Map.of(),
                System.currentTimeMillis(), System.currentTimeMillis()))
            .toList();
        groupsToStart.forEach(metadataStore::savePartitionGroup);
      }

      // 逐组顺序启动
      ActorFuture<Void> chain = CompletableActorFuture.completed(null);
      for (final var meta : groupsToStart) {
        final var groupMeta = meta;
        chain = chain.andThen(
            ignored -> manager.startPartitionGroup(groupMeta)
                .thenApply(v -> (Void) null, DIRECT),
            DIRECT);
      }
      chain.onComplete((result, error) -> {
        if (error == null) {
          started = true;
          LOG.info("Raft 编排服务启动完成，共管理 {} 个分区组",
              manager.getPartitionGroupNames().size());
          future.complete(this);
        } else {
          LOG.error("Raft 编排服务启动失败", error);
          future.completeExceptionally(error);
        }
      });
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public boolean isRunning() {
    return started;
  }

  @Override
  public CompletableFuture<Void> stop() {
    final var future = new CompletableFuture<Void>();
    if (!started) {
      future.complete(null);
      return future;
    }
    final var manager = partitionManager;
    if (manager != null) {
      manager.stopAll().onComplete((result, error) -> {
        try {
          manager.close();
          final var topology = topologyService;
          if (topology != null) {
            topology.stop().join();
          }
          metadataStore.persist();
          started = false;
          if (error == null) {
            future.complete(null);
          } else {
            future.completeExceptionally(error);
          }
        } catch (final Exception e) {
          future.completeExceptionally(e);
        }
      }, DIRECT);
    } else {
      metadataStore.persist();
      started = false;
      future.complete(null);
    }
    return future;
  }

  // ===== 能力入口 =====

  @Override
  public PartitionTopologyService topologyService() {
    return topologyService;
  }

  @Override
  public PartitionManager partitionManager() {
    return partitionManager;
  }

  @Override
  public NodeMetadataStore metadataStore() {
    return metadataStore;
  }
}
