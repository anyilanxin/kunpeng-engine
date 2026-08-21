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
package io.atomix.raft.partition.impl;

import static io.atomix.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;

import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.PartitionId;
import io.atomix.cluster.messaging.ClusterCommunicationService;
import io.atomix.primitive.partition.Partition;
import io.atomix.primitive.partition.PartitionMetadata;
import io.atomix.raft.LeadershipTransferCoordinatorCheck;
import io.atomix.raft.LeadershipTransferWriteBarrier;
import io.atomix.raft.RaftApplicationEntryCommittedPositionListener;
import io.atomix.raft.RaftCommitListener;
import io.atomix.raft.RaftRoleChangeListener;
import io.atomix.raft.RaftRoleStateListener;
import io.atomix.raft.RaftServer;
import io.atomix.raft.RaftServer.Role;
import io.atomix.raft.SnapshotReplicationListener;
import io.atomix.raft.cluster.RaftMember;
import io.atomix.raft.cluster.RaftMember.Type;
import io.atomix.raft.metrics.RaftRequestMetrics;
import io.atomix.raft.metrics.RaftStartupMetrics;
import io.atomix.raft.partition.RaftElectionConfig;
import io.atomix.raft.partition.RaftPartition;
import io.atomix.raft.partition.RaftPartitionConfig;
import io.atomix.raft.partition.RaftPartitionTopology;
import io.atomix.raft.partition.RaftStorageConfig;
import io.atomix.raft.roles.RaftRole;
import io.atomix.raft.storage.RaftStorage;
import io.atomix.raft.storage.log.RaftLogReader;
import io.atomix.raft.zeebe.ZeebeLogAppender;
import io.atomix.utils.serializer.Serializer;
import io.atomix.cluster.PhysicalTenantIds;
import io.atomix.raft.journal.SegmentInfo;
import io.atomix.raft.snapshot.PersistedSnapshot;
import io.atomix.raft.snapshot.PersistedSnapshotStore;
import io.atomix.raft.snapshot.ReceivableSnapshotStore;
import io.atomix.raft.snapshot.SnapshotException;
import io.atomix.raft.snapshot.SnapshotProvider;
import io.atomix.raft.snapshot.SnapshotType;
import io.atomix.raft.snapshot.transfer.SnapshotTransferClient;
import io.atomix.raft.snapshot.transfer.SnapshotTransferServer;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.atomix.utils.VisibleForTesting;
import io.atomix.utils.health.FailureListener;
import io.atomix.utils.health.HealthMonitorable;
import io.atomix.utils.health.HealthReport;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link Partition} server. */
public class RaftPartitionServer implements HealthMonitorable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RaftPartitionServer.class);

  private final MemberId localMemberId;
  private final RaftPartition partition;
  private final RaftPartitionConfig config;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService clusterCommunicator;
  private final PartitionMetadata partitionMetadata;
  private final Duration requestTimeout;
  private final Duration snapshotRequestTimeout;
  private final Duration configurationChangeTimeout;

  private final ReceivableSnapshotStore persistedSnapshotStore;
  private final RaftServer server;
  private final MeterRegistry meterRegistry;
  private final SnapshotTransferServer snapshotTransferServer;
  /** 业务快照拍摄 SPI，由分区构造链传入；为 null 时不支持业务快照拍摄。 */
  private final SnapshotProvider snapshotProvider;

  public RaftPartitionServer(
      final RaftPartition partition,
      final RaftPartitionConfig config,
      final MemberId localMemberId,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService clusterCommunicator,
      final ReceivableSnapshotStore persistedSnapshotStore,
      final PartitionMetadata partitionMetadata,
      final MeterRegistry meterRegistry,
      final SnapshotProvider snapshotProvider) {
    this.partition = partition;
    this.config = config;
    this.localMemberId = localMemberId;
    this.membershipService = membershipService;
    this.clusterCommunicator = clusterCommunicator;
    this.meterRegistry = meterRegistry;
    this.persistedSnapshotStore = persistedSnapshotStore;
    this.partitionMetadata = partitionMetadata;
    this.snapshotProvider =
        snapshotProvider != null ? snapshotProvider : new SnapshotProvider.NoopSnapshotProvider();
    requestTimeout = config.getRequestTimeout();
    snapshotRequestTimeout = config.getSnapshotRequestTimeout();
    configurationChangeTimeout = config.getConfigurationChangeTimeout();
    server = buildServer(meterRegistry);
    snapshotTransferServer =
        new SnapshotTransferServer(clusterCommunicator, partition.name(), persistedSnapshotStore);
    // 角色变更时：把分区角色写入本节点成员属性广播集群；成为 leader 才注册快照传输服务，离开即卸载
    server.addRoleChangeListener(this::onPartitionRoleChanged);
  }

  private void onPartitionRoleChanged(final RaftServer.Role newRole, final long term) {
    publishPartitionRole(newRole);
    if (newRole == RaftServer.Role.LEADER) {
      snapshotTransferServer.register();
      LOGGER.info("Leader registered snapshot transfer handler for partition {}", partition.id());
    } else {
      snapshotTransferServer.unregister();
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
    return server != null ? server.shutdown() : CompletableFuture.completedFuture(null);
  }

  /**
   * 从源分区的当前 leader 节点拉取指定类型的最新快照（引导/合并快照跨分区传输入口），接收并提交到
   * 本分区的快照存储。目标节点经集群分区拓扑（成员属性广播）自动解析，调用方只需给出源分区 ID。
   */
  public CompletableFuture<PersistedSnapshot> pullSnapshot(
      final PartitionId sourcePartitionId, final SnapshotType type, final int preferredChunkSize) {
    final var topology = new RaftPartitionTopology(membershipService);
    final var leader = topology.leaderOf(sourcePartitionId);
    if (leader.isEmpty()) {
      return CompletableFuture.failedFuture(
          new SnapshotException(
              "No known leader for partition " + sourcePartitionId + "; cannot pull snapshot"));
    }
    final var client = new SnapshotTransferClient(clusterCommunicator, snapshotRequestTimeout);
    return client.pull(
        RaftPartitionTopology.partitionNameOf(sourcePartitionId),
        leader.get().id(),
        type,
        preferredChunkSize,
        persistedSnapshotStore);
  }

  /**
   * 拍摄本节点当前已提交位点的常规快照：内容生成委托给业务注入的 {@link SnapshotProvider}，
   * 快照模块负责临时目录、manifest（含业务元数据）、逐文件校验与原子提交。
   */
  public CompletableFuture<PersistedSnapshot> takeSnapshot() {
    final var provider = snapshotProvider;
    final var context = server.getContext();

    final long commitIndex = context.getCommitIndex();
    final long term = context.getTerm();
    final var transientSnapshot =
        persistedSnapshotStore
            .newTransientSnapshot(
                commitIndex,
                term,
                localMemberId.id(),
                1,
                SnapshotType.REGULAR,
                provider.snapshotVersion(),
                provider.businessInfo())
            .orElse(null);
    if (transientSnapshot == null) {
      return CompletableFuture.failedFuture(
          new SnapshotException(
              "A newer snapshot already exists in partition "
                  + partition.id()
                  + "; skipped taking snapshot at index "
                  + commitIndex));
    }

    return transientSnapshot
        .take(
            directory -> {
              try {
                provider.takeSnapshot(directory);
              } catch (final Exception e) {
                throw new CompletionException(e);
              }
            })
        .thenCompose(ignored -> transientSnapshot.commit());
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
   * @see io.atomix.raft.impl.RaftContext#addCommitListener(RaftCommitListener)
   */
  public void addCommitListener(final RaftCommitListener commitListener) {
    server.getContext().addCommitListener(commitListener);
  }

  /**
   * @see io.atomix.raft.impl.RaftContext#removeCommitListener(RaftCommitListener)
   */
  public void removeCommitListener(final RaftCommitListener commitListener) {
    server.getContext().removeCommitListener(commitListener);
  }

  /**
   * @see
   *     io.atomix.raft.impl.RaftContext#addCommittedEntryListener(RaftApplicationEntryCommittedPositionListener)
   */
  public void addCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener commitListener) {
    server.getContext().addCommittedEntryListener(commitListener);
  }

  /**
   * @see
   *     io.atomix.raft.impl.RaftContext#removeCommittedEntryListener(RaftApplicationEntryCommittedPositionListener)
   */
  public void removeCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener commitListener) {
    server.getContext().removeCommittedEntryListener(commitListener);
  }

  /**
   * @see
   *     io.atomix.raft.impl.RaftContext#addSnapshotReplicationListener(SnapshotReplicationListener)
   */
  public void addSnapshotReplicationListener(final SnapshotReplicationListener listener) {
    server.getContext().addSnapshotReplicationListener(listener);
  }

  /**
   * @see
   *     io.atomix.raft.impl.RaftContext#removeSnapshotReplicationListener(SnapshotReplicationListener)
   */
  public void removeSnapshotReplicationListener(final SnapshotReplicationListener listener) {
    server.getContext().removeSnapshotReplicationListener(listener);
  }

  public PersistedSnapshotStore getPersistedSnapshotStore() {
    return persistedSnapshotStore;
  }

  /** Deletes the server. */
  public void delete() {
    try {
      FileUtil.deleteTreeIfExists(partition.dataDirectory().toPath());
    } catch (final IOException e) {
      LOGGER.error("Failed to delete partition: {}", partition, e);
    }
  }

  public Optional<ZeebeLogAppender> getAppender() {
    final RaftRole role = server.getContext().getRaftRole();
    if (role instanceof ZeebeLogAppender) {
      return Optional.of((ZeebeLogAppender) role);
    }

    return Optional.empty();
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

  public MemberId getMemberId() {
    return localMemberId;
  }

  private RaftStorage createRaftStorage() {
    final RaftStorageConfig storageConfig = config.getStorageConfig();
    return RaftStorage.builder(meterRegistry)
        .withPrefix(partition.name())
        .withPartitionId(partition.id().number())
        .withDirectory(partition.dataDirectory())
        .withMaxSegmentSize((int) storageConfig.getSegmentSize())
        .withFlusherFactory(storageConfig.flusherFactory())
        .withFreeDiskSpace(storageConfig.getFreeDiskSpace())
        .withSnapshotStore(persistedSnapshotStore)
        .withJournalIndexDensity(storageConfig.getJournalIndexDensity())
        .withSegmentAllocator(storageConfig.getSegmentAllocator())
        .build();
  }

  private RaftServerCommunicator createServerProtocol() {
    final var partitionId = partition.id().number();
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
