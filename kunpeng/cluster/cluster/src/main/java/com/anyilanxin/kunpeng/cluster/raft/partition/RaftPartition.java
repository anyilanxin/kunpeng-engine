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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap.BootstrapSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultRaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer.DefaultSnapshotTransfer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer.SnapshotPushServer;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract partition. */
public final class RaftPartition implements Partition, HealthMonitorable {
  public static final String PARTITION_NAME_FORMAT = "%s-partition-%d";
  private static final Logger LOG = LoggerFactory.getLogger(RaftPartition.class);
  private static final String PARTITION_COMPONENT_NAME_FORMAT = "RaftPartition-%d";

  /** 合并镜像目录内最多保留 1 个：一次只进行一次转移，完成后即删除。 */
  private static final int MAX_MERGE_SNAPSHOT_COUNT = 1;

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

  /** 快照内容拍摄/合并 SPI，构造时注入（仅 raft 类型使用）。 */
  private final RaftSnapshotProvider snapshotProvider;

  /**
   * 跨分区转移镜像拍摄 SPI（可空），构造时注入：非空时本分区同时具备 引导拍摄端（leader 时响应 BOOTSTRAP 请求，见 {@link
   * #bootstrap(PartitionId, Address)} 的对端角色）与合并拍摄端（{@link #transferData(PartitionId,
   * Address)}）能力；为空则两者皆不可用。
   */
  private final TransferSnapshotProvider transferSnapshotProvider;

  /** 本分区的快照存储，启动时内部构建。 */
  private RaftSnapshotStore snapshotStore;

  /** 引导镜像拍摄端存储（transferSnapshotProvider 为空时不创建）。 */
  private BootstrapSnapshotStore bootstrapSnapshotStore;

  /**
   * 合并镜像存储（{@code snapshots/merge}，transferSnapshotProvider 为空时不创建）： 源分区侧经 {@link
   * #transferData} 拍摄合并镜像，目标分区侧接收源分区推送的合并镜像，两端共用同一类型目录。
   */
  private DefaultRaftSnapshotStore mergeSnapshotStore;

  /** 本分区的快照 actor，承载 store 状态串行与周期拍摄。 */
  private Actor snapshotActor;

  /**
   * 条数触发快照的水位判定（快照 actor 加载磁盘镜像后种子，raft 线程经提交监听器访问；
   * 阈值 0 时 tryFire 恒 false，即禁用条数触发）。
   */
  private SnapshotEntryTrigger snapshotEntryTrigger;

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
      final RaftSnapshotProvider snapshotProvider,
      final SnapshotFileInfoProvider snapshotFileInfoProvider) {
    this(
        partitionMetadata,
        config,
        partitionDirectory,
        runtimeDirectory,
        meterRegistry,
        managementService,
        actorSchedulingService,
        snapshotProvider,
        snapshotFileInfoProvider,
        null);
  }

  /**
   * 完整构造：额外携带跨分区转移镜像拍摄 SPI（可空）——非空时本分区 leader 可为其他新分区提供跨分区 引导镜像（见 {@link
   * #bootstrap(PartitionId, Address)} 的对端角色），本分区也可作为合并源执行 {@link
   * #transferData(PartitionId, Address)}。
   */
  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final Path partitionDirectory,
      final Path runtimeDirectory,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final ActorSchedulingService actorSchedulingService,
      final RaftSnapshotProvider snapshotProvider,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final TransferSnapshotProvider transferSnapshotProvider) {
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
    this.transferSnapshotProvider = transferSnapshotProvider;
  }

  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final Path partitionDirectory,
      final Path runtimeDirectory,
      final MeterRegistry meterRegistry,
      final PartitionManagementService managementService,
      final ActorSchedulingService actorSchedulingService,
      final RaftSnapshotProvider snapshotProvider) {
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

  /**
   * 跨分区引导新分区：从源分区拉取引导镜像落地为本分区首个快照，再触发两阶段镜像安装并 单节点 bootstrap。
   *
   * <p>流程：请求源分区指定成员（其 leader）拍摄引导镜像 → 逐批拉取落地到本分区快照存储
   * （{@code snapshots/snapshot}，与 follower 接收镜像同路）→ 两阶段安装（复制开始通知 → 日志对齐 镜像 index →
   * 复制完成通知）→ 单节点 bootstrap 当选 leader。成功返回后由调用方再逐个 join 其余副本。
   *
   * <p>约束：仅用于新分区首个节点（本节点须是分区成员且本地无既有状态）；引导节点收到/放弃镜像后 都会通知拍摄端释放
   * transferId 引用（引用归零时拍摄端删除引导镜像）。中途失败重试本方法：已落地的镜像 以 {@link
   * SnapshotAlreadyExistsException} 暴露，视为已落地直接进入安装。
   *
   * @param sourcePartitionId 引导镜像的源分区
   * @param sourceAddress 源分区 leader 所在成员地址
   * @return 引导完成 future（本节点成为该分区 leader），失败时回收半启动的 server
   */
  public CompletableFuture<RaftPartition> bootstrap(
      final PartitionId sourcePartitionId, final Address sourceAddress) {
    if (!partitionMetadata
        .members()
        .contains(managementService.getMembershipService().getLocalMember().id())) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "Local member is not part of partition " + partitionId + "; cannot bootstrap"));
    }
    initServer();
    return startSnapshotStore()
        .thenCompose(v -> pullBootstrapSnapshot(sourcePartitionId, sourceAddress))
        .thenCompose(v -> startBootstrappedServer())
        .whenComplete(
            (result, error) -> {
              if (error != null) {
                cleanupFailedServer();
              }
            });
  }

  /** 拉取引导镜像落地到本分区快照存储：崩溃重试时已落地（already-exists）直接沿用。 */
  private CompletableFuture<Void> pullBootstrapSnapshot(
      final PartitionId sourcePartitionId, final Address sourceAddress) {
    final var sourceMember = resolveRemoteMember(sourceAddress);
    if (sourceMember.isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "Cannot resolve member at address " + sourceAddress + " for bootstrap"));
    }
    final var transfer =
        new DefaultSnapshotTransfer(
            managementService.getMembershipService(),
            managementService.getCommunicationService(),
            snapshotStore,
            config.getSnapshotRequestTimeout(),
            config.getSnapshotTransferMaxBatchSize());
    actorSchedulingService.submitActor(transfer);
    return transfer
        .getBootstrapSnapshot(sourcePartitionId, sourceMember.get())
        .toCompletableFuture()
        .handle(
            (persisted, error) -> {
              if (error != null) {
                final var cause = error.getCause() != null ? error.getCause() : error;
                if (!(cause instanceof SnapshotAlreadyExistsException)) {
                  throw new IllegalStateException(
                      "Failed to pull bootstrap snapshot from partition "
                          + sourcePartitionId
                          + " via "
                          + sourceAddress,
                      error);
                }
                // 上次引导已落地（崩溃重试）：沿用既有镜像进入安装
                LOG.info(
                    "Bootstrap snapshot for partition {} already landed, continue installing",
                    partitionId);
              }
              return (Void) null;
            })
        .whenComplete(
            (v, error) ->
                transfer
                    .closeAsync()
                    .toCompletableFuture()
                    .exceptionally(
                        closeError -> {
                          LOG.warn(
                              "Failed to close bootstrap transfer of partition {}",
                              partitionId,
                              closeError);
                          return null;
                        }));
  }

  /** 镜像落地后的启动：注册监听器 → 两阶段安装 → 单节点 bootstrap 当选 leader。 */
  private CompletableFuture<RaftPartition> startBootstrappedServer() {
    server = createServer();
    server.addCommitListener(this::onCommitAdvanced);
    final var roleListeners = List.copyOf(deferredRoleChangeListeners);
    final var stateListeners = List.copyOf(deferredRoleStateListeners);
    final var metaListeners = List.copyOf(deferredBusinessMetaListeners);
    roleListeners.forEach(server::addRoleChangeListener);
    stateListeners.forEach(server::addRoleStateListener);
    metaListeners.forEach(server::addBusinessMetaListener);
    return server
        .installSnapshot()
        .thenCompose(v -> server.bootstrap())
        .thenApply(
            r -> {
              deferredRoleChangeListeners.removeAll(roleListeners);
              deferredRoleStateListeners.removeAll(stateListeners);
              deferredBusinessMetaListeners.removeAll(metaListeners);
              return this;
            });
  }

  /**
   * 分区删除前的数据迁移（合并快照源端）：把本分区全部数据经合并镜像转移到目标分区， 目标分区确认合并完成后 future
   * 完成，调用方随后执行 leave()/delete() 关停并删除本分区。
   *
   * <p>流程：本分区（须为 leader 且已配置 TransferSnapshotProvider）以当前 commit 位点拍摄合并镜像
   * （{@code snapshots/merge}，经 {@link TransferSnapshotProvider#takeMergeSnapshot}）→ 逐批推送到目标分区
   * leader → 发送合并完成等待请求，目标分区完成整个合并流（接收→两阶段安装合并→重拍 raft 镜像→通知 其 follower
   * 安装）后应答 → 删除本地合并镜像。失败可重试：同位点重试复用上次拍摄残留，位点推进则重拍（保留策略自动清旧）。
   *
   * @param targetPartitionId 数据迁入的目标分区
   * @param targetAddress 目标分区 leader 所在成员地址
   * @return 迁移完成 future（目标分区已确认合并完成），完成后调用方再执行 leave()/delete()； 失败时由调用方决定重试或放弃迁移
   */
  public CompletableFuture<Void> transferData(
      final PartitionId targetPartitionId, final Address targetAddress) {
    final RaftPartitionServer current = server;
    if (current == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("partition " + partitionId + " is not started"));
    }
    if (mergeSnapshotStore == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "partition " + partitionId + " has no TransferSnapshotProvider; cannot transfer"));
    }
    if (getRole() != Role.LEADER) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "partition " + partitionId + " is not led by this member; transfer from leader only"));
    }
    final var targetMember = resolveRemoteMember(targetAddress);
    if (targetMember.isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "Cannot resolve member at address " + targetAddress + " for data transfer"));
    }
    return takeMergeSnapshot(current)
        .thenCompose(
            snapshot ->
                pushMergeSnapshot(snapshot, targetPartitionId, targetMember.get())
                    .thenCompose(
                        v -> awaitMergeCompletion(snapshot, targetPartitionId, targetMember.get()))
                    .whenComplete(
                        (v, error) ->
                            mergeSnapshotStore
                                .deleteAllSnapshots()
                                .toCompletableFuture()
                                .exceptionally(
                                    deleteError -> {
                                      LOG.warn(
                                          "Failed to delete local merge snapshot of partition {}",
                                          partitionId,
                                          deleteError);
                                      return null;
                                    })));
  }

  /** 拍摄合并镜像：以当前 commit 位点在 merge 存储强制（同位点重试覆盖）拍摄并提交。 */
  private CompletableFuture<PersistedSnapshot> takeMergeSnapshot(
      final RaftPartitionServer current) {
    final long index = current.getCommitIndex();
    final long term = current.getTerm();
    if (index <= 0) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "partition " + partitionId + " has no committed data to transfer"));
    }
    return mergeSnapshotStore
        .newTransientSnapshot(index, term)
        .toCompletableFuture()
        .thenCompose(
            pending -> {
              if (pending == null) {
                // 同位点已拍（上次尝试残留）：直接取现有镜像推送
                final var latest = mergeSnapshotStore.getLatestSnapshot();
                if (latest.isPresent()) {
                  return CompletableFuture.completedFuture(latest.get());
                }
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Merge snapshot disappeared before persist"));
              }
              return pending.persist().toCompletableFuture();
            });
  }

  /** 推送合并镜像到目标分区 leader（目标端接收进其 snapshots/merge 后启动合并流）。 */
  private CompletableFuture<Void> pushMergeSnapshot(
      final PersistedSnapshot snapshot,
      final PartitionId targetPartitionId,
      final MemberId targetMember) {
    final var transfer =
        new DefaultSnapshotTransfer(
            managementService.getMembershipService(),
            managementService.getCommunicationService(),
            snapshotStore,
            config.getSnapshotRequestTimeout(),
            config.getSnapshotTransferMaxBatchSize());
    actorSchedulingService.submitActor(transfer);
    return transfer
        .pushSnapshot(snapshot, targetPartitionId, targetMember)
        .toCompletableFuture()
        .whenComplete(
            (v, error) ->
                transfer
                    .closeAsync()
                    .toCompletableFuture()
                    .exceptionally(
                        closeError -> {
                          LOG.warn(
                              "Failed to close merge push transfer of partition {}",
                              partitionId,
                              closeError);
                          return null;
                        }));
  }

  /** 等待目标分区确认合并完成：一问一答，payload 为镜像 id，目标端完成合并流后应答。 */
  private CompletableFuture<Void> awaitMergeCompletion(
      final PersistedSnapshot snapshot,
      final PartitionId targetPartitionId,
      final MemberId targetMember) {
    return managementService
        .getCommunicationService()
        .send(
            SnapshotPushServer.awaitSubjectOf(
                RaftPartitionTopology.partitionNameOf(targetPartitionId)),
            snapshot.snapshotId().asString().getBytes(StandardCharsets.UTF_8),
            Function.identity(),
            Function.identity(),
            targetMember,
            config.getSnapshotMergeAwaitTimeout())
        .thenApply(ignored -> null);
  }

  /**
   * 目标端合并流（目标分区 leader 收完合并镜像后触发，见 {@code SnapshotPushServer}）： 两阶段镜像安装包裹业务合并——
   * 开始安装通知（业务关闭消费者）→ {@link RaftSnapshotProvider#mergeSnapshot} 合并 → 完成安装通知（业务恢复）→ 强制
   * 重拍 raft 镜像（承载合并后状态）→ 通知所有 follower 安装 leader 合并后的镜像。
   *
   * @param received 接收完成的合并镜像（位于本分区 snapshots/merge）
   * @return 合并流完成 future（异常完成即本次合并失败，源分区会整体重推）
   */
  public CompletableFuture<Void> mergeReceivedSnapshot(final PersistedSnapshot received) {
    final RaftPartitionServer current = server;
    if (current == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("partition " + partitionId + " is not started"));
    }
    final var context = current.getContext();
    // 阶段一：安装开始（快照复制开始通知，业务关闭日志消费者）
    context.notifySnapshotReplicationStarted();
    return snapshotProvider
        .mergeSnapshot(received.getPath())
        .toCompletableFuture()
        .thenRun(context::notifySnapshotReplicationCompleted)
        // 阶段二完成后：强制重拍 raft 镜像，把合并后的业务状态固化为可复制/可压缩的 raft 镜像
        .thenCompose(v -> takeSnapshotInternal(true))
        // 通知所有 follower 安装 leader 合并后的最新镜像（各自拉取 + 两阶段安装）
        .thenRun(this::notifyFollowersToInstallSnapshot);
  }

  /**
   * follower 侧安装入口（收到 leader 的安装通知后触发，见 RaftPartitionServer）： 从本分区 leader 拉取最新 raft 镜像落地到
   * {@code snapshots/snapshot}，再走两阶段安装对齐本地状态。 已有同水位镜像（SnapshotAlreadyExists）时直接安装现有镜像。
   */
  public void requestLeaderSnapshotInstall() {
    final RaftPartitionServer current = server;
    if (current == null) {
      return;
    }
    final var transfer =
        new DefaultSnapshotTransfer(
            managementService.getMembershipService(),
            managementService.getCommunicationService(),
            snapshotStore,
            config.getSnapshotRequestTimeout(),
            config.getSnapshotTransferMaxBatchSize());
    actorSchedulingService.submitActor(transfer);
    transfer
        .getLatestSnapshot(partitionId)
        .toCompletableFuture()
        .handle(
            (persisted, error) -> {
              if (error != null) {
                final var cause = error.getCause() != null ? error.getCause() : error;
                if (!(cause instanceof SnapshotAlreadyExistsException)) {
                  LOG.warn(
                      "Failed to pull merged snapshot of partition {} from leader", partitionId, error);
                  transfer
                      .closeAsync()
                      .toCompletableFuture()
                      .exceptionally(ignore -> null);
                  return null;
                }
                // 已具备同水位镜像（可能为本机周期拍摄）：直接进入安装
                LOG.info(
                    "Snapshot of partition {} at same watermark already exists, installing it",
                    partitionId);
              }
              current
                  .installSnapshot()
                  .whenComplete(
                      (v, installError) -> {
                        if (installError != null) {
                          LOG.warn(
                              "Failed to install merged snapshot of partition {}", partitionId, installError);
                        }
                      });
              transfer.closeAsync().toCompletableFuture().exceptionally(ignore -> null);
              return null;
            });
  }

  /** 通知本分区所有 follower（除本节点）从 leader 拉取并安装最新镜像（合并流收尾，fire-and-forget）。 */
  private void notifyFollowersToInstallSnapshot() {
    final RaftPartitionServer current = server;
    if (current == null) {
      return;
    }
    final var localMemberId = managementService.getMembershipService().getLocalMember().id();
    final var communicator = managementService.getCommunicationService();
    current.getMembers().stream()
        .map(RaftMember::memberId)
        .filter(memberId -> !memberId.equals(localMemberId))
        .forEach(
            follower ->
                communicator.unicast(
                    RaftPartitionServer.SNAPSHOT_INSTALL_NOTIFY_SUBJECT_PREFIX + name(),
                    new byte[0],
                    Function.identity(),
                    follower,
                    true));
  }

  /** 按地址解析远程成员（排除本节点）。 */
  private Optional<MemberId> resolveRemoteMember(final Address address) {
    final var localMemberId = managementService.getMembershipService().getLocalMember().id();
    return managementService.getMembershipService().getMembers().stream()
        .filter(member -> address.equals(member.address()))
        .map(member -> member.id())
        .filter(memberId -> !memberId.equals(localMemberId))
        .findFirst();
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
              server.addCommitListener(this::onCommitAdvanced);
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
    if (transferSnapshotProvider != null) {
      bootstrapSnapshotStore =
          new BootstrapSnapshotStore(
              nodeId,
              partitionDirectory,
              transferSnapshotProvider,
              snapshotFileInfoProvider,
              snapshotActor);
      // 合并镜像存储：源端拍摄（直接经内容写入器调 takeMergeSnapshot）+ 目标端接收共用同一 merge 目录
      mergeSnapshotStore =
          new DefaultRaftSnapshotStore(
              nodeId,
              partitionDirectory.resolve("snapshots").resolve(SnapshotType.MERGE.directoryName()),
              MAX_MERGE_SNAPSHOT_COUNT,
              new DefaultSimpleFileVerificationStore(),
              snapshotFileInfoProvider,
              this::takeMergeSnapshotContent,
              snapshotActor);
    }
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

  /**
   * 启动快照存储：在快照 actor 上加载磁盘既有镜像，必须先于 raft 启动完成；引导/合并镜像存储（如有） 同步启动并清空上次残留——两者的
   * 跨节点会话状态只在内存（transferId 引用/合并 pending），重启后无人推进清理，残留只会泄漏磁盘。
   */
  private CompletableFuture<Void> startSnapshotStore() {
    final java.util.concurrent.Callable<Void> load =
        () -> {
          snapshotStore.start();
          if (bootstrapSnapshotStore != null) {
            bootstrapSnapshotStore.start();
          }
          if (mergeSnapshotStore != null) {
            mergeSnapshotStore.start();
            mergeSnapshotStore.deleteAllSnapshots();
          }
          // 以既有镜像水位做种子：重启后增量从镜像 index 起算，避免启动即触发
          snapshotEntryTrigger =
              new SnapshotEntryTrigger(
                  config.getSnapshotEntryTriggerThreshold(),
                  snapshotStore.getCurrentSnapshotIndex());
          return null;
        };
    return snapshotActor.call(load).toCompletableFuture();
  }

  /** 无参手动拍摄镜像，供外部使用；分区未启动时 future 异常完成。 */
  public CompletableFuture<Void> takeSnapshot() {
    return takeSnapshotInternal(false);
  }

  /**
   * 拍摄 raft 镜像（可在同水位强制重拍）：{@code force=true} 时即使与当前最新镜像同 id 也重拍覆盖 —— 合并流在
   * {@link RaftSnapshotProvider#mergeSnapshot} 后调用，保证合并后的业务状态进入 raft 镜像（水位未推进时 常规拍摄会被同
   * id 跳过）。
   */
  private CompletableFuture<Void> takeSnapshotInternal(final boolean force) {
    final RaftPartitionServer current = server;
    if (current == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("partition " + partitionId + " is not started"));
    }
    final long index = current.getCommitIndex();
    final long term = current.getTerm();
    if (!force && (index <= 0 || index <= snapshotStore.getCurrentSnapshotIndex())) {
      return CompletableFuture.completedFuture(null);
    }
    return ((DefaultRaftSnapshotStore) snapshotStore)
        .newTransientSnapshot(index, term, force)
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
    takeSnapshotInternal(false)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn("Periodic snapshot failed for partition {}", partitionId, error);
              }
            });
  }

  /**
   * 条数触发入口（raft 线程回调）：commit 增量达阈值时转投快照 actor 拍摄。失败仅记录日志，
   * 由周期触发兜底；无需在途去重——触发本身即按水位重武装，重入频率被阈值约束。
   */
  private void onCommitAdvanced(final long commitIndex) {
    if (snapshotEntryTrigger.tryFire(commitIndex)) {
      LOG.debug(
          "Partition {} commit index {} reached snapshot entry threshold, taking snapshot",
          partitionId,
          commitIndex);
      snapshotActor.run(
          () ->
              takeSnapshotInternal(false)
                  .whenComplete(
                      (ignored, error) -> {
                        if (error != null) {
                          LOG.warn(
                              "Entry-threshold snapshot failed for partition {}",
                              partitionId,
                              error);
                        }
                      }));
    }
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
        meterRegistry,
        bootstrapSnapshotStore,
        mergeSnapshotStore);
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

  /** 强制关闭分区：关闭前先拍一次快照（best-effort）、删除引导/合并镜像（best-effort），再停 raft、关快照 actor。 */
  public CompletableFuture<Void> close() {
    return snapshotBeforeClose()
        .thenCompose(v -> deleteBootstrapSnapshotsBeforeClose())
        .thenCompose(v -> deleteMergeSnapshotsBeforeClose())
        .thenCompose(v -> closeSnapshotProvider())
        .thenCompose(v -> closeServer())
        .thenCompose(v -> closeSnapshotActor())
        .exceptionally(
            error -> {
              LOG.error("Error on shutdown partition: {}.", partitionId, error);
              return null;
            });
  }

  /**
   * 关闭前主动删除引导镜像（best-effort）：正常流程镜像在最后一个引用 RELEASE 时已删除， 这里兜底清理因引导方中途死亡而残留的镜像；
   * 失败仅记录日志，不阻断关闭。
   */
  private CompletableFuture<Void> deleteBootstrapSnapshotsBeforeClose() {
    if (bootstrapSnapshotStore == null) {
      return CompletableFuture.completedFuture(null);
    }
    return bootstrapSnapshotStore
        .deleteBootstrapSnapshots()
        .toCompletableFuture()
        .handle(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn(
                    "Failed to delete bootstrap snapshots of partition {} before closing",
                    partitionId,
                    error);
              }
              return null;
            });
  }

  /** 关闭前删除合并镜像（best-effort）：转移已完成/中断的残留一并清理，失败不阻断关闭。 */
  private CompletableFuture<Void> deleteMergeSnapshotsBeforeClose() {
    if (mergeSnapshotStore == null) {
      return CompletableFuture.completedFuture(null);
    }
    return mergeSnapshotStore
        .deleteAllSnapshots()
        .toCompletableFuture()
        .handle(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn(
                    "Failed to delete merge snapshots of partition {} before closing",
                    partitionId,
                    error);
              }
              return null;
            });
  }

  /** 关闭前拍摄一次快照（best-effort）：失败仅记录日志，不阻断关闭。 */
  private CompletableFuture<Void> snapshotBeforeClose() {
    if (server == null) {
      return CompletableFuture.completedFuture(null);
    }
    return takeSnapshotInternal(false)
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
    if (transferSnapshotProvider != null) {
      transferSnapshotProvider.close();
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

  /** 合并镜像内容直写：委托 {@link TransferSnapshotProvider#takeMergeSnapshot}，业务信息清单随镜像持久化。 */
  private Map<String, Object> takeMergeSnapshotContent(final Path snapshotDirectory) {
    return transferSnapshotProvider.takeMergeSnapshot(snapshotDirectory);
  }
}
