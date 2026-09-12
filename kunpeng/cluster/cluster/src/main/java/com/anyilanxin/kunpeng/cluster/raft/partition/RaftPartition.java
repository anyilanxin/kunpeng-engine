/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleStateListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaUpdateResponse;
import com.anyilanxin.kunpeng.cluster.raft.metadata.PartitionBusinessMeta;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.SnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultRaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract partition. */
public final class RaftPartition implements Partition, HealthMonitorable {
  public static final String PARTITION_NAME_FORMAT = "%s-partition-%d";
  private static final Logger LOG = LoggerFactory.getLogger(RaftPartition.class);
  private static final String PARTITION_COMPONENT_NAME_FORMAT = "RaftPartition-%d";

  private final PartitionId partitionId;
  private final RaftPartitionConfig config;
  private final Path partitionDirectory;
  private final Path runtimeDirectory;
  private final MeterRegistry meterRegistry;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private final Set<RaftRoleStateListener> deferredRoleStateListeners = new CopyOnWriteArraySet<>();
  private final Set<RaftBusinessMetaListener> deferredBusinessMetaListeners =
      new CopyOnWriteArraySet<>();
  private final PartitionMetadata partitionMetadata;

  /** 分区管理服务（成员发现与集群通信），构造时注入。 */
  private final PartitionManagementService managementService;

  /** 快照 actor 调度服务，构造时注入。 */
  private final ActorSchedulingService actorSchedulingService;

  /** 快照内容拍摄 SPI，构造时注入（仅 raft 类型使用）。 */
  private final SnapshotProvider snapshotProvider;

  /** 本分区的快照存储，启动时内部构建。 */
  private RaftSnapshotStore snapshotStore;

  /** 本分区的快照 actor，承载 store 状态串行与周期拍摄。 */
  private Actor snapshotActor;

  private volatile RaftPartitionServer server;
  private final SnapshotFileInfoProvider snapshotFileInfoProvider;

  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final Path partitionDirectory,
      final Path runtimeDirectory,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final ActorSchedulingService actorSchedulingService,
      final SnapshotProvider snapshotProvider,
      final SnapshotFileInfoProvider snapshotFileInfoProvider) {
    partitionId = partitionMetadata.id();
    this.partitionMetadata = partitionMetadata;
    this.config = config;
    this.partitionDirectory = partitionDirectory;
    this.runtimeDirectory = runtimeDirectory;
    this.meterRegistry = meterRegistry;
    this.managementService = managementService;
    this.actorSchedulingService = actorSchedulingService;
    this.snapshotProvider = snapshotProvider;
    this.snapshotFileInfoProvider = snapshotFileInfoProvider;
  }

  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final Path partitionDirectory,
      final Path runtimeDirectory,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final ActorSchedulingService actorSchedulingService,
      final SnapshotProvider snapshotProvider) {
    this(
        partitionMetadata,
        config,
        partitionDirectory,
        runtimeDirectory,
        meterRegistry,
        managementService,
        actorSchedulingService,
        snapshotProvider,
        new DefaultSnapshotFileInfoProvider());
  }

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    if (server == null) {
      deferredRoleChangeListeners.add(listener);
    } else {
      server.addRoleChangeListener(listener);
    }
  }

  /**
   * 注册业务三态状态监听器（角色变更与快照复制事件聚合为 LEADER/FOLLOWER/INACTIVE 视图）， 注册后立即回调一次当前状态；分区服务器尚未创建时延迟到初始化完成时注册。
   *
   * @param listener 业务状态监听器
   */
  public void addRoleStateListener(final RaftRoleStateListener listener) {
    if (server == null) {
      deferredRoleStateListeners.add(listener);
    } else {
      server.addRoleStateListener(listener);
    }
  }

  /** 注销业务三态状态监听器。 */
  public void removeRoleStateListener(final RaftRoleStateListener listener) {
    deferredRoleStateListeners.remove(listener);
    if (server != null) {
      server.removeRoleStateListener(listener);
    }
  }

  /** 注册业务元数据变更监听器（onStarted/onCompleted 成对触发，携带当时角色与 term）； 分区服务器尚未创建时延迟到初始化完成时注册。 */
  public void addBusinessMetaListener(final RaftBusinessMetaListener listener) {
    if (server == null) {
      deferredBusinessMetaListeners.add(listener);
    } else {
      server.addBusinessMetaListener(listener);
    }
  }

  /** 注销业务元数据变更监听器。 */
  public void removeBusinessMetaListener(final RaftBusinessMetaListener listener) {
    deferredBusinessMetaListeners.remove(listener);
    if (server != null) {
      server.removeBusinessMetaListener(listener);
    }
  }

  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    deferredRoleChangeListeners.remove(listener);
    if (server != null) {
      server.removeRoleChangeListener(listener);
    }
  }

  /**
   * Returns the partition data directory.
   *
   * @return the partition data directory
   */
  public Path rootDirectory() {
    return partitionDirectory;
  }

  public Path runtimeDirectory() {
    return runtimeDirectory;
  }

  /** 引导分区（本节点是初始成员时创建 Raft 服务并引导集群）。 */
  public CompletableFuture<RaftPartition> bootstrap() {
    if (partitionMetadata
        .members()
        .contains(managementService.getMembershipService().getLocalMember().id())) {
      initServer();
      return startServer(false);
    }
    return CompletableFuture.completedFuture(this);
  }

  /** 以加入者身份加入分区集群。 */
  public CompletableFuture<RaftPartition> join() {
    initServer();
    return startServer(true);
  }

  /** 启动：先启动快照存储（磁盘加载），再构建并启动 raft；失败时回收 server，防止重试构建第二个 server 共写同一目录。 */
  private CompletableFuture<RaftPartition> startServer(final boolean join) {
    return startSnapshotStore()
        .thenCompose(
            v -> {
              server = createServer();
              final var roleListeners = List.copyOf(deferredRoleChangeListeners);
              final var stateListeners = List.copyOf(deferredRoleStateListeners);
              final var metaListeners = List.copyOf(deferredBusinessMetaListeners);
              roleListeners.forEach(server::addRoleChangeListener);
              stateListeners.forEach(server::addRoleStateListener);
              metaListeners.forEach(server::addBusinessMetaListener);
              return (join ? server.join() : server.bootstrap())
                  .thenApply(
                      r -> {
                        deferredRoleChangeListeners.removeAll(roleListeners);
                        deferredRoleStateListeners.removeAll(stateListeners);
                        deferredBusinessMetaListeners.removeAll(metaListeners);
                        return this;
                      });
            })
        .whenComplete(
            (result, error) -> {
              if (error != null && server != null) {
                cleanupFailedServer();
              }
            });
  }

  /** 启动失败回收：监听器留在延迟队列等待重试，停止 raft 并关闭快照 actor。 */
  private void cleanupFailedServer() {
    final var failedServer = server;
    server = null;
    LOG.warn("Partition {} raft startup failed, cleaning up the failed server", partitionId);
    failedServer
        .stop()
        .exceptionally(
            stopError -> {
              LOG.warn(
                  "Failed to stop raft server of partition {} on startup failure",
                  partitionId,
                  stopError);
              return null;
            })
        .thenCompose(v -> closeSnapshotActor())
        .exceptionally(
            closeError -> {
              LOG.warn(
                  "Failed to close snapshot actor of partition {} on startup failure",
                  partitionId,
                  closeError);
              return null;
            });
  }

  public CompletableFuture<RaftPartition> leave() {
    final var current = server;
    if (current == null) {
      return CompletableFuture.completedFuture(this);
    }
    return current.leave().thenApply(v -> this);
  }

  private void initServer() {
    initSnapshotStore();
  }

  /** 构建快照 actor 与 raft store（仅创建对象，磁盘加载在启动时经 {@link #startSnapshotStore()} 执行）。 */
  private void initSnapshotStore() {
    final String nodeId = managementService.getMembershipService().getLocalMember().id().id();
    snapshotActor = Actor.newActor().name(name() + "-snapshot").build();
    snapshotStore =
        new DefaultRaftSnapshotStore(
            nodeId,
            partitionDirectory.resolve("snapshots").resolve(SnapshotType.RAFT.directoryName()),
            config.getMaxSnapshotCount(),
            new DefaultSimpleFileVerificationStore(),
            snapshotFileInfoProvider,
            snapshotProvider,
            snapshotActor);
    snapshotProvider.setSnapshotStore(snapshotStore);
    actorSchedulingService
        .submitActor(snapshotActor)
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                LOG.error("Failed to initialize snapshot", throwable);
              } else {
                snapshotActor.run(
                    () ->
                        snapshotActor
                            .getControl()
                            .runAtFixedRate(
                                config.getSnapshotInterval(),
                                RaftPartition.this::takeSnapshotScheduled));
              }
            });
  }

  /** 启动快照存储：在快照 actor 上加载磁盘既有镜像，必须先于 raft 启动完成。 */
  private CompletableFuture<Void> startSnapshotStore() {
    final java.util.concurrent.Callable<Void> load =
        () -> {
          snapshotStore.start();
          return null;
        };
    return snapshotActor.call(load).toCompletableFuture();
  }

  /** 无参手动拍摄镜像，供外部使用；分区未启动时 future 异常完成。 */
  public CompletableFuture<Void> takeSnapshot() {
    return takeSnapshotInternal();
  }

  private CompletableFuture<Void> takeSnapshotInternal() {
    final RaftPartitionServer current = server;
    if (current == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("partition " + partitionId + " is not started"));
    }
    final long index = current.getCommitIndex();
    final long term = current.getTerm();
    if (index <= 0 || index <= snapshotStore.getCurrentSnapshotIndex()) {
      return CompletableFuture.completedFuture(null);
    }
    return snapshotStore
        .newTransientSnapshot(index, term)
        .toCompletableFuture()
        // 相同 id 前置跳过时 pending 为 null（本次拍摄已被 store 跳过），直接视为完成
        .thenCompose(
            pending -> {
              if (pending == null) {
                return CompletableFuture.<Void>completedFuture(null);
              }
              return pending.persist().toCompletableFuture().thenApply(ignored -> null);
            });
  }

  /** 周期拍摄入口：运行在快照 actor 线程，失败仅记录日志。 */
  private void takeSnapshotScheduled() {
    if (server == null) {
      // 分区尚未启动（或启动失败）时周期拍摄直接跳过，避免刷错误日志
      return;
    }
    takeSnapshotInternal()
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn("Periodic snapshot failed for partition {}", partitionId, error);
              }
            });
  }

  /** Creates a Raft server. */
  private RaftPartitionServer createServer() {
    return new RaftPartitionServer(
        this,
        config,
        managementService.getMembershipService().getLocalMember().id(),
        managementService.getMembershipService(),
        managementService.getCommunicationService(),
        snapshotStore,
        partitionMetadata,
        meterRegistry);
  }

  /**
   * Returns the partition name.
   *
   * @return the partition name
   */
  public String name() {
    return String.format(PARTITION_NAME_FORMAT, partitionId.group(), partitionId.id());
  }

  @Override
  public String componentName() {
    return String.format(PARTITION_COMPONENT_NAME_FORMAT, partitionId.id());
  }

  @Override
  public HealthReport getHealthReport() {
    final var current = server;
    if (current == null) {
      return HealthReport.unhealthy(this).withName(componentName());
    }
    return current.getHealthReport().withName(componentName());
  }

  @Override
  public void addFailureListener(final FailureListener failureListener) {
    server.addFailureListener(failureListener);
  }

  @Override
  public void removeFailureListener(final FailureListener failureListener) {
    server.removeFailureListener(failureListener);
  }

  /** 强制关闭分区：关闭前先拍一次快照（best-effort），再停 raft、关快照 actor。 */
  public CompletableFuture<Void> close() {
    return snapshotBeforeClose()
        .thenCompose(v -> closeSnapshotProvider())
        .thenCompose(v -> closeServer())
        .thenCompose(v -> closeSnapshotActor())
        .exceptionally(
            error -> {
              LOG.error("Error on shutdown partition: {}.", partitionId, error);
              return null;
            });
  }

  /** 关闭前拍摄一次快照（best-effort）：失败仅记录日志，不阻断关闭。 */
  private CompletableFuture<Void> snapshotBeforeClose() {
    if (server == null) {
      return CompletableFuture.completedFuture(null);
    }
    return takeSnapshotInternal()
        .handle(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn("Failed to snapshot before closing partition {}", partitionId, error);
              }
              return null;
            });
  }

  private CompletableFuture<Void> closeSnapshotProvider() {
    if (snapshotProvider != null) {
      snapshotProvider.close();
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<Void> closeSnapshotActor() {
    if (snapshotActor != null) {
      return snapshotActor.closeAsync().toCompletableFuture();
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<Void> closeServer() {
    if (server != null) {
      return server.stop();
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Deletes the partition.
   *
   * @return future to be completed once the partition has been deleted
   */
  public CompletableFuture<Void> delete() {
    final var current = server;
    if (current == null) {
      return CompletableFuture.completedFuture(null);
    }
    return current.stop().thenRun(current::delete);
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("partitionId", id()).toString();
  }

  @Override
  public PartitionId id() {
    return partitionId;
  }

  @Override
  public long term() {
    return server != null ? server.getTerm() : 0;
  }

  @Override
  public Collection<MemberId> members() {
    final var membersFromServer = server != null ? server.getMembers() : null;
    if (membersFromServer != null) {
      // Use members from server if available. This will reflect changes when members leave or join.
      return membersFromServer.stream().map(RaftMember::memberId).collect(Collectors.toSet());
    } else {
      // Fall back to static partition metadata so that we can still get the members of a partition
      // that hasn't been started yet. This is necessary for bootstrap.
      return partitionMetadata != null ? partitionMetadata.members() : Collections.emptyList();
    }
  }

  @Override
  public PartitionMetadata partitionMetadata() {
    return partitionMetadata;
  }

  public Role getRole() {
    return server != null ? server.getRole() : null;
  }

  public RaftPartitionServer getServer() {
    return server;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public CompletableFuture<Void> stepDown() {
    final var current = server;
    if (current == null) {
      return CompletableFuture.completedFuture(null);
    }
    return current.stepDown();
  }

  /**
   * Step down for leader balancing. Only steps down if priority election is enabled.
   *
   * @return a future that completes when the step down is complete, or completes immediately if
   *     priority election is not enabled or server is not available
   */
  public CompletableFuture<Void> stepDownForLeaderBalancing() {
    if (server != null && config.isPriorityElectionEnabled()) {
      return server.stepDown();
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * 修改业务元数据（业务端入口）：entries 为全量快照，整体覆盖生效（未携带的 key 即删除）。 本机 leader 直接追加、非 leader 自动转发、无 leader 返回
   * NO_LEADER；成功（多数派落盘提交）后 future 以提交条目 index 完成。分区尚未启动时 future 异常完成。
   */
  public CompletableFuture<BusinessMetaUpdateResponse> updateBusinessMeta(
      final Map<String, String> entries) {
    final RaftPartitionServer current = server;
    if (current == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("partition " + partitionId + " is not started"));
    }
    return current.updateBusinessMeta(entries);
  }

  /**
   * 读取当前已提交业务元数据（内存只读视图，线程安全；含 appliedIndex/appliedTerm 水位）。 分区尚未启动时抛 {@link
   * IllegalStateException}——此时投影尚未从磁盘加载，返回空视图会掩盖既有元数据。
   */
  public PartitionBusinessMeta businessMeta() {
    final RaftPartitionServer current = server;
    if (current == null) {
      throw new IllegalStateException("partition " + partitionId + " is not started");
    }
    return current.businessMeta();
  }

  public CompletableFuture<Void> stop() {
    final var current = server;
    if (current == null) {
      return CompletableFuture.completedFuture(null);
    }
    return current.stop();
  }

  public RaftPartitionConfig getPartitionConfig() {
    return config;
  }
}
