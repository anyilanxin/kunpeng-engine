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
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupProcess;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 默认分区管理器（actor）：负责分区组/分区的调度。
 *
 * <p>作为 {@link Actor} 单线程串行化启动流程，以自身作为 {@code ConcurrencyControl}
 * 驱动 {@link StartupProcess}：先执行工厂前置步骤，再创建并启动 Raft 分区，最后执行
 * 工厂后置步骤；停止时倒序关闭。
 */
public final class DefaultPartitionManager extends Actor implements PartitionManager {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPartitionManager.class);

  private final Map<String, RaftGroupFactory> factories;
  private final NodeMetadataStore metadataStore;
  private final ClusterMembershipService membershipService;
  private final MessagingService messagingService;
  private final ClusterCommunicationService communicationService;
  private final ManagedPartitionTopologyService topologyService;
  private final MeterRegistry meterRegistry;
  private final Map<String, RaftGroupContext> contexts = new ConcurrentHashMap<>();
  private final Map<String, StartupProcess<RaftGroupContext>> processes = new ConcurrentHashMap<>();
  private final Map<String, Map<Integer, RaftPartition>> partitions = new ConcurrentHashMap<>();

  public DefaultPartitionManager(
    final Map<String, RaftGroupFactory> factories,
    final NodeMetadataStore metadataStore,
    final ClusterMembershipService membershipService,
    final MessagingService messagingService,
    final ClusterCommunicationService communicationService,
    final ManagedPartitionTopologyService topologyService,
    final MeterRegistry meterRegistry) {
    this.factories = factories;
    this.metadataStore = metadataStore;
    this.membershipService = membershipService;
    this.messagingService = messagingService;
    this.communicationService = communicationService;
    this.topologyService = topologyService;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public ActorFuture<RaftGroupContext> startPartitionGroup(final NodePartitionMetadata meta) {
    final CompletableActorFuture<RaftGroupContext> result = new CompletableActorFuture<>();
    actor.run(
      () -> {
        try {
          startPartitionGroupInternal(meta)
            .onComplete(
              (v, err) -> {
                if (err == null) {
                  result.complete(v);
                } else {
                  result.completeExceptionally(err);
                }
              },
              actor);
        } catch (final Exception e) {
          result.completeExceptionally(e);
        }
      });
    return result;
  }

  private ActorFuture<RaftGroupContext> startPartitionGroupInternal(final NodePartitionMetadata meta) {
    final var factory = factories.get(meta.groupType());
    if (factory == null) {
      throw new IllegalArgumentException("未注册的分区组类型: " + meta.groupType());
    }
    final var groupDir = metadataStore.groupDataDirectory(meta.groupName());
    final var context = new RaftGroupContext() {
    };
    context.init(partitionMetadata(meta), groupDir, this);

    final var partitionRegistry =
      partitions.computeIfAbsent(meta.groupName(), k -> new ConcurrentHashMap<>());
    final var partitionStep =
      new RaftPartitionGroupStartupStep<>(
        factory,
        membershipService,
        communicationService,
        topologyService,
        meterRegistry,
        partitionRegistry,
        meta);

    // 启动链：前置步骤 → Raft 分区 → 后置步骤（StartupProcess 倒序关闭）
    final var steps = new ArrayList<StartupStep<RaftGroupContext>>();
    steps.addAll(supplyList(factory.getBeforeStartupSteps(context)));
    steps.add(partitionStep);
    steps.addAll(supplyList(factory.getAfterStartupSteps(context)));

    final var process = new StartupProcess<>(steps);
    contexts.put(meta.groupName(), context);
    processes.put(meta.groupName(), process);

    LOG.info("启动分区组 {} (type={}, steps={})", meta.groupName(), meta.groupType(),
      steps.stream().map(StartupStep::getName).toList());
    return process.startup(context.concurrencyControl(), context);
  }

  @Override
  public ActorFuture<Void> stopPartitionGroup(final String groupName) {
    final var process = processes.get(groupName);
    final var context = contexts.get(groupName);
    if (process == null || context == null) {
      return CompletableActorFuture.completedExceptionally(
        new IllegalArgumentException("未管理的分区组: " + groupName));
    }
    LOG.info("停止分区组: {}", groupName);
    return process.shutdown(context.concurrencyControl(), context);
  }

  @Override
  public ActorFuture<Void> stopAll() {
    ActorFuture<Void> chain = CompletableActorFuture.completed(null);
    for (final var groupName : List.copyOf(contexts.keySet())) {
      chain = chain.andThen(ignored -> stopPartitionGroup(groupName), this);
    }
    return chain;
  }

  @Override
  public Optional<RaftGroupContext> getPartitionGroup(final String groupName) {
    return Optional.ofNullable(contexts.get(groupName));
  }

  @Override
  public Set<String> getPartitionGroupNames() {
    return Set.copyOf(contexts.keySet());
  }

  // ===== 远程调度（由 RemoteScheduleHandler 调用，本节点作为目标节点执行） =====

  @Override
  public CompletableFuture<Void> startRemoteGroup(
    final String groupName,
    final String groupType,
    final int partitionCount,
    final int replicationFactor) {
    LOG.info("远程调度启动分区组: {} (type={}, partitions={}, replication={})",
      groupName, groupType, partitionCount, replicationFactor);

    if (contexts.containsKey(groupName)) {
      LOG.warn("分区组 {} 已在本节点运行，幂等跳过", groupName);
      return CompletableFuture.completedFuture(null);
    }

    final var now = System.currentTimeMillis();
    final var meta = new NodePartitionMetadata(
      groupName, groupType, partitionCount, replicationFactor,
      List.of(), Map.of(), now, now);
    metadataStore.savePartitionGroup(meta);
    return startPartitionGroup(meta).toCompletableFuture().thenApply(v -> null);
  }

  @Override
  public CompletableFuture<Void> stopPartitionGroupRemote(final String groupName) {
    LOG.info("远程调度停止分区组: {}", groupName);
    if (!processes.containsKey(groupName)) {
      return CompletableFuture.completedFuture(null);
    }
    return stopPartitionGroup(groupName).toCompletableFuture().thenApply(v -> null);
  }

  @Override
  public CompletableFuture<Void> joinRemoteGroup(final String groupName) {
    LOG.info("远程调度加入分区组: {}", groupName);
    if (!contexts.containsKey(groupName)) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("分区组未启动: " + groupName));
    }
    final var meta = metadataStore.getPartitionGroup(groupName);
    if (meta.isEmpty()) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("分区组元数据缺失: " + groupName));
    }
    LOG.info("分区组 {} 已在本节点运行，元数据已确认", groupName);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> leaveRemoteGroup(final String groupName) {
    LOG.info("远程调度离开分区组: {}", groupName);
    if (processes.containsKey(groupName)) {
      return stopPartitionGroup(groupName).toCompletableFuture().thenApply(v -> {
        metadataStore.removePartitionGroup(groupName);
        return null;
      });
    }
    metadataStore.removePartitionGroup(groupName);
    return CompletableFuture.completedFuture(null);
  }

  // ===== 内部 =====

  /**
   * 组上下文占位元数据（成员等由工厂/配置后续提供；此处以 1 号分区标识组）
   */
  private static PartitionMetadata partitionMetadata(final NodePartitionMetadata meta) {
    return new PartitionMetadata(
      PartitionId.from(meta.groupName(), 1), Set.of(), Map.of(), 0, null);
  }

  private static <T> List<T> supplyList(final Supplier<List<T>> supplier) {
    final var list = supplier != null ? supplier.get() : null;
    return list != null ? list : List.of();
  }
}
