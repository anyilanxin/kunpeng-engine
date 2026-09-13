/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition.impl;

import static com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PhysicalTenantIds;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.*;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.journal.SegmentInfo;
import com.anyilanxin.kunpeng.cluster.raft.logentry.LogAppender;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaServer;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaSync;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaTransfer;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaUpdateResponse;
import com.anyilanxin.kunpeng.cluster.raft.metadata.PartitionBusinessMeta;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRequestMetrics;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftStartupMetrics;
import com.anyilanxin.kunpeng.cluster.raft.partition.*;
import com.anyilanxin.kunpeng.cluster.raft.roles.RaftRole;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap.BootstrapSnapshotServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap.BootstrapSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer.SnapshotPushServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer.SnapshotTransferServer;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogReader;
import com.anyilanxin.kunpeng.cluster.utils.VisibleForTesting;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link Partition} server. */
public class RaftPartitionServer implements HealthMonitorable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RaftPartitionServer.class);

  /** follower 安装通知主题前缀：{@value #SNAPSHOT_INSTALL_NOTIFY_SUBJECT_PREFIX}{分区名}，leader 合并后通知 follower 拉取安装。 */
  public static final String SNAPSHOT_INSTALL_NOTIFY_SUBJECT_PREFIX =
      "snapshot-install-notify-";

  private final MemberId localMemberId;
  private final RaftPartition partition;
  private final RaftPartitionConfig config;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService clusterCommunicator;
  private final PartitionMetadata partitionMetadata;
  private final Duration requestTimeout;
  private final Duration snapshotRequestTimeout;
  private final Duration configurationChangeTimeout;

  private final RaftSnapshotStore persistedSnapshotStore;
  private final RaftServer server;
  private final MeterRegistry meterRegistry;
  private final SnapshotTransferServer snapshotTransferServer;

  /**
   * 合并快照接收端（可空）：配置了 TransferSnapshotProvider 的分区才有——目标分区 leader 角色时注册，
   * 接收源分区 leader 推来的合并镜像并触发合并流。
   */
  private final SnapshotPushServer mergePushServer;

  /**
   * 引导镜像拍摄端（可空）：配置了 TransferSnapshotProvider 的分区才有——leader 角色时注册， 接收新分区引导节点的跨分区引导请求。
   */
  private final BootstrapSnapshotServer bootstrapSnapshotServer;

  /** 业务元数据修改请求接收端：server 存续期间常驻注册（非 leader 负责转发/拒绝）。 */
  private final BusinessMetaServer businessMetaServer;

  /** leader 同步拉取端：follower 缺口时向 leader 拉取全量状态（经 RaftContext 钩子触发）。 */
  private final BusinessMetaSync businessMetaSync;

  public RaftPartitionServer(
      final RaftPartition partition,
      final RaftPartitionConfig config,
      final MemberId localMemberId,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService clusterCommunicator,
      final RaftSnapshotStore persistedSnapshotStore,
      final PartitionMetadata partitionMetadata,
      final MeterRegistry meterRegistry) {
    this(
        partition,
        config,
        localMemberId,
        membershipService,
        clusterCommunicator,
        persistedSnapshotStore,
        partitionMetadata,
        meterRegistry,
        null,
        null);
  }

  public RaftPartitionServer(
      final RaftPartition partition,
      final RaftPartitionConfig config,
      final MemberId localMemberId,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService clusterCommunicator,
      final RaftSnapshotStore persistedSnapshotStore,
      final PartitionMetadata partitionMetadata,
      final MeterRegistry meterRegistry,
      final BootstrapSnapshotStore bootstrapSnapshotStore,
      final RaftSnapshotStore mergeSnapshotStore) {
    this.partition = partition;
    this.config = config;
    this.localMemberId = localMemberId;
    this.membershipService = membershipService;
    this.clusterCommunicator = clusterCommunicator;
    this.meterRegistry = meterRegistry;
    this.persistedSnapshotStore = persistedSnapshotStore;
    this.partitionMetadata = partitionMetadata;
    requestTimeout = config.getRequestTimeout();
    snapshotRequestTimeout = config.getSnapshotRequestTimeout();
    configurationChangeTimeout = config.getConfigurationChangeTimeout();
    server = buildServer(meterRegistry);
    mergePushServer =
        mergeSnapshotStore == null
            ? null
            : new SnapshotPushServer(
                clusterCommunicator,
                partition.name(),
                mergeSnapshotStore,
                partition::mergeReceivedSnapshot);
    snapshotTransferServer =
        new SnapshotTransferServer(
            clusterCommunicator,
            partition.name(),
            persistedSnapshotStore,
            config.getSnapshotTransferMaxBatchSize());
    bootstrapSnapshotServer =
        bootstrapSnapshotStore == null
            ? null
            : new BootstrapSnapshotServer(
                clusterCommunicator,
                partition.name(),
                bootstrapSnapshotStore,
                this::getCommitIndex,
                this::getTerm,
                config.getSnapshotTransferMaxBatchSize());
    businessMetaServer = new BusinessMetaServer(clusterCommunicator, this, partition.name());
    businessMetaServer.register();
    businessMetaSync =
        new BusinessMetaSync(clusterCommunicator, this, partition.name(), requestTimeout);
    server.getContext().setBusinessMetaSyncHook(businessMetaSync::requestSyncFromLeader);
    // follower 安装通知：server 存续期间常驻注册（仅 leader 触发），收到后从 leader 拉取最新镜像并两阶段安装
    clusterCommunicator.consume(
        SNAPSHOT_INSTALL_NOTIFY_SUBJECT_PREFIX + partition.name(),
        Function.identity(),
        (sender, payload) -> partition.requestLeaderSnapshotInstall(),
        Runnable::run);
    // 角色变更时：把分区角色写入本节点成员属性广播集群；成为 leader 才注册快照传输服务，离开即卸载
    server.addRoleChangeListener(this::onPartitionRoleChanged);
  }

  private void onPartitionRoleChanged(final RaftServer.Role newRole, final long term) {
    publishPartitionRole(newRole);
    if (newRole == RaftServer.Role.LEADER) {
      snapshotTransferServer.register();
      if (mergePushServer != null) {
        mergePushServer.register();
      }
      if (bootstrapSnapshotServer != null) {
        bootstrapSnapshotServer.register();
      }
      LOGGER.info("Leader registered snapshot transfer handler for partition {}", partition.id());
    } else {
      snapshotTransferServer.unregister();
      if (mergePushServer != null) {
        mergePushServer.unregister();
      }
      if (bootstrapSnapshotServer != null) {
        bootstrapSnapshotServer.unregister();
      }
    }
  }

  /** 把本分区的最新角色写入本地成员属性，经成员元数据传播机制广播到集群。 */
  private void publishPartitionRole(final RaftServer.Role role) {
    membershipService
        .getLocalMember()
        .properties()
        .setProperty(RaftPartitionTopology.rolePropertyKey(partition.name()), role.name());
  }

  public CompletableFuture<RaftPartitionServer> bootstrap() {
    final RaftStartupMetrics raftStartupMetrics =
        new RaftStartupMetrics(partition.name(), meterRegistry);
    LOGGER.info("Server bootstrapping partition {}", partition.id());
    final long bootstrapStartTime = System.currentTimeMillis();
    return server
        .bootstrap(partition.members())
        .whenComplete(
            (r, e) -> {
              if (e == null) {
                final long endTime = System.currentTimeMillis();
                raftStartupMetrics.observeBootstrapDuration(endTime - bootstrapStartTime);
                LOGGER.info(
                    "Server successfully bootstrapped partition {} in {}ms",
                    partition.id(),
                    endTime - bootstrapStartTime);
              } else {
                LOGGER.warn("Server bootstrap failed for partition {}", partition.id(), e);
              }
            })
        .thenApply(v -> this);
  }

  public CompletableFuture<RaftPartitionServer> join() {
    final var metrics = new RaftStartupMetrics(partition.name(), meterRegistry);
    final long joinStartTime = System.currentTimeMillis();
    LOGGER.info("Server joining partition {}", partition.id());
    return server
        .join(partitionMetadata.members())
        .whenComplete(
            (r, e) -> {
              if (e == null) {
                final long endTime = System.currentTimeMillis();
                metrics.observeJoinDuration(endTime - joinStartTime);
                LOGGER.info(
                    "Server successfully joined partition {} in {}ms",
                    partition.id(),
                    endTime - joinStartTime);
              } else {
                LOGGER.warn("Server join failed for partition {}", partition.id(), e);
              }
            })
        .thenApply(v -> this);
  }

  public CompletableFuture<RaftPartitionServer> leave() {
    return server.leave().thenApply(v -> this);
  }

  public CompletableFuture<RaftPartitionServer> forceReconfigure(
      final Map<MemberId, Type> members) {
    return server.forceConfigure(members).thenApply(v -> this);
  }

  public CompletableFuture<Void> stop() {
    snapshotTransferServer.unregister();
    if (mergePushServer != null) {
      mergePushServer.unregister();
    }
    if (bootstrapSnapshotServer != null) {
      bootstrapSnapshotServer.unregister();
    }
    businessMetaServer.unregister();
    clusterCommunicator.unsubscribe(SNAPSHOT_INSTALL_NOTIFY_SUBJECT_PREFIX + partition.name());
    return server != null ? server.shutdown() : CompletableFuture.completedFuture(null);
  }

  /**
   * 把存储内已落地的最新镜像安装为本节点状态（两阶段复制通知 + 日志对齐），详见 {@link
   * RaftContext#installSnapshot()}；引导新分区与 follower 安装合并镜像共用。
   */
  public CompletableFuture<Void> installSnapshot() {
    return server.getContext().installSnapshot();
  }

  public CompletableFuture<Void> reconfigurePriority(final int newPriority) {
    return server.reconfigurePriority(newPriority);
  }

  private RaftServer buildServer(final MeterRegistry meterRegistry) {
    final var electionConfig =
        config.isPriorityElectionEnabled()
            ? RaftElectionConfig.ofPriorityElection(
                partitionMetadata.getTargetPriority(), partitionMetadata.getPriority(localMemberId))
            : RaftElectionConfig.ofDefaultElection();

    return RaftServer.builder(localMemberId)
        .withName(partition.name())
        .withPartitionId(partition.id())
        .withMembershipService(membershipService)
        .withProtocol(createServerProtocol())
        .withPartitionConfig(config)
        .withStorage(createRaftStorage())
        .withEntryValidator(config.getEntryValidator())
        .withElectionConfig(electionConfig)
        .withMeterRegistry(meterRegistry)
        .build();
  }

  public CompletableFuture<Void> flushLog() {
    return server.flushLog();
  }

  public RaftLogReader openReader() {
    return server.getContext().getLog().openCommittedReader();
  }

  /** 底层 raft 上下文（业务元数据修改接收端/同步拉取端复用）。 */
  public RaftContext getContext() {
    return server.getContext();
  }

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    server.addRoleChangeListener(listener);
  }

  /** 注册业务三态状态监听器（LEADER/FOLLOWER/INACTIVE 聚合视图），注册后立即回调当前状态。 */
  public void addRoleStateListener(final RaftRoleStateListener listener) {
    server.addRoleStateListener(listener);
  }

  /** 注销业务三态状态监听器。 */
  public void removeRoleStateListener(final RaftRoleStateListener listener) {
    server.removeRoleStateListener(listener);
  }

  /** 注册业务元数据变更监听器（onStarted/onCompleted 成对触发，携带当时角色与 term）。 */
  public void addBusinessMetaListener(final RaftBusinessMetaListener listener) {
    server.addBusinessMetaListener(listener);
  }

  /** 注销业务元数据变更监听器。 */
  public void removeBusinessMetaListener(final RaftBusinessMetaListener listener) {
    server.removeBusinessMetaListener(listener);
  }

  @Override
  public String componentName() {
    return getClass().getSimpleName();
  }

  @Override
  public HealthReport getHealthReport() {
    return server.getContext().getHealthReport();
  }

  @Override
  public void addFailureListener(final FailureListener listener) {
    server.addFailureListener(listener);
  }

  @Override
  public void removeFailureListener(final FailureListener listener) {
    server.removeFailureListener(listener);
  }

  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    server.removeRoleChangeListener(listener);
  }

  /**
   * @see com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#addCommitListener(RaftCommitListener)
   */
  public void addCommitListener(final RaftCommitListener commitListener) {
    server.getContext().addCommitListener(commitListener);
  }

  /**
   * @see
   *     com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#removeCommitListener(RaftCommitListener)
   */
  public void removeCommitListener(final RaftCommitListener commitListener) {
    server.getContext().removeCommitListener(commitListener);
  }

  /**
   * @see
   *     com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#addCommittedEntryListener(RaftApplicationEntryCommittedPositionListener)
   */
  public void addCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener commitListener) {
    server.getContext().addCommittedEntryListener(commitListener);
  }

  /**
   * @see
   *     com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#removeCommittedEntryListener(RaftApplicationEntryCommittedPositionListener)
   */
  public void removeCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener commitListener) {
    server.getContext().removeCommittedEntryListener(commitListener);
  }

  /**
   * @see
   *     com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#addSnapshotReplicationListener(SnapshotReplicationListener)
   */
  public void addSnapshotReplicationListener(final SnapshotReplicationListener listener) {
    server.getContext().addSnapshotReplicationListener(listener);
  }

  /**
   * @see
   *     com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#removeSnapshotReplicationListener(SnapshotReplicationListener)
   */
  public void removeSnapshotReplicationListener(final SnapshotReplicationListener listener) {
    server.getContext().removeSnapshotReplicationListener(listener);
  }

  public RaftSnapshotStore getPersistedSnapshotStore() {
    return persistedSnapshotStore;
  }

  /** Deletes the server. */
  public void delete() {
    try {
      FileUtil.deleteTreeIfExists(partition.rootDirectory());
    } catch (final IOException e) {
      LOGGER.error("Failed to delete partition: {}", partition, e);
    }
  }

  public Optional<LogAppender> getAppender() {
    final RaftRole role = server.getContext().getRaftRole();
    if (role instanceof LogAppender) {
      return Optional.of((LogAppender) role);
    }

    return Optional.empty();
  }

  /** 当前已提交业务元数据（内存只读视图）。 */
  public PartitionBusinessMeta businessMeta() {
    return server.getContext().getBusinessMetaManager().current();
  }

  /**
   * 修改业务元数据入口（任意角色可调用）：本机 leader 直接追加；非 leader 且已知 leader 转发； 无 leader 返回 NO_LEADER。entries
   * 为全量快照（整体覆盖语义，未携带的 key 即删除）；成功（多数派落盘提交）后返回提交条目 index。
   */
  public CompletableFuture<BusinessMetaUpdateResponse> updateBusinessMeta(
      final Map<String, String> entries) {
    if (server.getContext().isLeader()) {
      return appendBusinessMeta(entries);
    }
    final var leader = server.getContext().getLeader();
    if (leader == null) {
      return CompletableFuture.completedFuture(BusinessMetaUpdateResponse.noLeader());
    }
    return forwardBusinessMetaTo(leader.memberId(), entries);
  }

  /** leader 路径：经 raft 内部入口追加 BusinessMetaEntry，多数派提交后完成 future；易主切换瞬间失主按无主拒绝。 */
  public CompletableFuture<BusinessMetaUpdateResponse> appendBusinessMeta(
      final Map<String, String> entries) {
    return server
        .getContext()
        .appendBusinessMeta(entries)
        .thenApply(BusinessMetaUpdateResponse::ok)
        .exceptionally(
            error ->
                error instanceof RaftException.NoLeader
                    ? BusinessMetaUpdateResponse.noLeader()
                    : BusinessMetaUpdateResponse.error("append failed: " + error.getMessage()));
  }

  /** 非 leader 路径：把更新转发到 leader 所在成员（forwarded=true 防环）。 */
  public CompletableFuture<BusinessMetaUpdateResponse> forwardBusinessMetaTo(
      final MemberId leaderId, final Map<String, String> entries) {
    return clusterCommunicator.send(
        BusinessMetaServer.subjectOf(partition.name()),
        BusinessMetaTransfer.encodeRequest(entries, true),
        Function.identity(),
        BusinessMetaTransfer::decodeResponse,
        leaderId,
        requestTimeout);
  }

  /**
   * Registers the broker-supplied barrier the leader uses to freeze/unfreeze the partition's writes
   * during a coordinated leadership transfer. Safe to call from the broker's thread; the barrier
   * takes effect once the Raft thread picks the registration up.
   */
  public void setLeadershipTransferWriteBarrier(final LeadershipTransferWriteBarrier barrier) {
    server.getContext().setLeadershipTransferWriteBarrier(barrier);
  }

  /**
   * Registers the broker-supplied check the leader uses to tell the cluster's rebalancing
   * coordinator from any other node asking it to transfer leadership. The returned future completes
   * once the check has taken effect on the Raft thread.
   */
  public CompletableFuture<Void> setLeadershipTransferCoordinatorCheck(
      final LeadershipTransferCoordinatorCheck check) {
    return server.getContext().setLeadershipTransferCoordinatorCheck(check);
  }

  public Role getRole() {
    return server.getRole();
  }

  public long getTerm() {
    return server.getTerm();
  }

  /** 当前已提交索引（快照拍摄位点来源）。 */
  public long getCommitIndex() {
    return server.getContext().getCommitIndex();
  }

  public MemberId getMemberId() {
    return localMemberId;
  }

  private RaftStorage createRaftStorage() {
    final RaftStorageConfig storageConfig = config.getStorageConfig();
    return RaftStorage.builder(meterRegistry)
        .withPrefix(partition.name())
        .withPartitionId(partition.id().id())
        .withDirectory(partition.rootDirectory().toFile())
        .withMaxSegmentSize((int) storageConfig.getSegmentSize())
        .withFlusherFactory(storageConfig.flusherFactory())
        .withFreeDiskSpace(storageConfig.getFreeDiskSpace())
        .withSnapshotStore(persistedSnapshotStore)
        .withJournalIndexDensity(storageConfig.getJournalIndexDensity())
        .withSegmentAllocator(storageConfig.getSegmentAllocator())
        .build();
  }

  private RaftServerCommunicator createServerProtocol() {
    final var partitionId = partition.id().id();
    final var partitionGroup = partition.id().group();

    final var sendingSubject = PARTITION_NAME_FORMAT.formatted(partitionGroup, partitionId);
    final var sendingContext = new RaftMessageContext(sendingSubject);

    final var receivingSubjects =
        partitionGroup.equals(PhysicalTenantIds.DEFAULT_PHYSICAL_TENANT_ID)
                && config.isReceiveOnLegacySubject()
            ? List.of(
                PARTITION_NAME_FORMAT.formatted("raft-partition", partitionId),
                PARTITION_NAME_FORMAT.formatted(partitionGroup, partitionId))
            : List.of(PARTITION_NAME_FORMAT.formatted(partitionGroup, partitionId));
    final var receivingContext = receivingSubjects.stream().map(RaftMessageContext::new).toList();

    return new RaftServerCommunicator(
        sendingContext,
        receivingContext,
        Serializer.using(RaftNamespaces.RAFT_PROTOCOL),
        clusterCommunicator,
        requestTimeout,
        snapshotRequestTimeout,
        configurationChangeTimeout,
        new RaftRequestMetrics(partition.name(), meterRegistry));
  }

  public CompletableFuture<Void> stepDown() {
    return server.stepDown();
  }

  public CompletableFuture<RaftServer> promote() {
    return server.promote();
  }

  public Collection<RaftMember> getMembers() {
    return server.cluster().getMembers();
  }

  public CompletableFuture<SegmentInfo> getTailSegments(final long index) {
    return server.getContext().getTailSegments(index);
  }

  @VisibleForTesting
  public RaftServer getServer() {
    return server;
  }
}
