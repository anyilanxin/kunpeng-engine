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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupProcess;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点级 Raft 调度管理服务。
 *
 * <p>启动时先检查磁盘元数据（{@code .raft-meta}）：有则按记录恢复全部分区组；无则按传入参数
 * 初始化并写盘。每个分区组通过工厂注册中心获取类型工厂，构造上下文后由
 * {@code StartupProcess} 调度有序启动（顺序）与有序关闭（倒序）。
 */
public final class RaftOrchestrationService implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(RaftOrchestrationService.class);
  private static final java.util.concurrent.Executor DIRECT = Runnable::run;

  private final PartitionGroupFactoryRegistry factoryRegistry;
  private final NodeMetadataStore metadataStore;
  private final Map<String, StartupProcess<? extends RaftGroupContext>> processes =
      new ConcurrentHashMap<>();
  private final Map<String, RaftGroupContext> contexts = new ConcurrentHashMap<>();
  private volatile boolean started;

  public RaftOrchestrationService(
      final PartitionGroupFactoryRegistry factoryRegistry,
      final java.nio.file.Path dataDirectory) {
    this.factoryRegistry = factoryRegistry;
    this.metadataStore = new NodeMetadataStore(dataDirectory);
  }

  /**
   * 启动调度服务：磁盘有元数据按记录恢复；无则按传入配置初始化并持久化。
   */
  public ActorFuture<Void> start(
      final MessagingService messagingService,
      final ClusterCommunicationService communicationService,
      final MeterRegistry meterRegistry,
      final RaftOrchestrationConfig startupConfig) {
    final var future = new CompletableActorFuture<Void>();
    try {
      Files.createDirectories(metadataStore.dataDirectory());
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
            ignored -> startPartitionGroup(
                groupMeta, messagingService, communicationService, meterRegistry)
                .thenApply(v -> (Void) null, DIRECT),
            DIRECT);
      }

      chain.onComplete((result, error) -> {
        if (error == null) {
          started = true;
          LOG.info("Raft 调度服务启动完成，共管理 {} 个分区组", contexts.size());
          future.complete(null);
        } else {
          LOG.error("Raft 调度服务启动失败", error);
          future.completeExceptionally(error);
        }
      });
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  /** 启动单个分区组：构造上下文 → 组装启动步骤 → StartupProcess 顺序执行 */
  @SuppressWarnings("unchecked")
  private <T extends RaftGroupContext> ActorFuture<T> startPartitionGroup(
      final NodePartitionMetadata meta,
      final MessagingService messagingService,
      final ClusterCommunicationService communicationService,
      final MeterRegistry meterRegistry) {
    final var factoryOpt = factoryRegistry.<T>get(meta.groupType());
    if (factoryOpt.isEmpty()) {
      return CompletableActorFuture.completedExceptionally(
          new IllegalArgumentException("未注册的分区组类型: " + meta.groupType()));
    }
    final var factory = factoryOpt.get();
    final var groupDir = metadataStore.groupDataDirectory(meta.groupName());
    final T context = factory.createContext(
        meta, groupDir, messagingService, communicationService, meterRegistry);

    // 组装启动步骤列表（工厂提供有序步骤；Raft 核心步骤由工厂安排在正确位置）
    final var steps = new ArrayList<com.anyilanxin.kunpeng.scheduler.startup.StartupStep<T>>(
        factory.startupSteps(context));
    final var process = new StartupProcess<T>(steps);

    contexts.put(meta.groupName(), context);
    processes.put(meta.groupName(), process);

    LOG.info("启动分区组 {} (type={}, steps={})", meta.groupName(), meta.groupType(),
        steps.stream().map(com.anyilanxin.kunpeng.scheduler.startup.StartupStep::getName).toList());

    return process.startup(null, context);
  }

  /** 停止单个分区组（StartupProcess 倒序关闭） */
  @SuppressWarnings("unchecked")
  public ActorFuture<Void> stopPartitionGroup(final String groupName) {
    final var process = (StartupProcess<RaftGroupContext>) processes.get(groupName);
    final var context = contexts.get(groupName);
    if (process == null || context == null) {
      return CompletableActorFuture.completedExceptionally(
          new IllegalArgumentException("未管理的分区组: " + groupName));
    }
    LOG.info("停止分区组: {}", groupName);
    return process.shutdown(null, context);
  }

  /** 获取指定分区组的上下文 */
  public Optional<RaftGroupContext> getPartitionGroup(final String groupName) {
    return Optional.ofNullable(contexts.get(groupName));
  }

  /** 获取全部分区组名 */
  public java.util.Set<String> getPartitionGroupNames() {
    return java.util.Set.copyOf(contexts.keySet());
  }

  /** 获取元数据存储 */
  public NodeMetadataStore metadataStore() {
    return metadataStore;
  }

  /** 停止全部分区组 */
  public ActorFuture<Void> stopAll() {
    ActorFuture<Void> chain = CompletableActorFuture.completed(null);
    for (final var groupName : List.copyOf(contexts.keySet())) {
      chain = chain.andThen(
          ignored -> stopPartitionGroup(groupName), DIRECT);
    }
    return chain;
  }

  @Override
  public void close() {
    if (started) {
      stopAll();
      started = false;
    }
    metadataStore.persist();
  }
}
