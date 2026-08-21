/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.*;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftStartupMetrics;
import com.anyilanxin.kunpeng.cluster.raft.partition.*;
import com.anyilanxin.kunpeng.cluster.raft.roles.RaftRole;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotVault;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.StorageException;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogReader;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.ZeebeLogAppender;
import com.anyilanxin.kunpeng.cluster.utils.Managed;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Futures;
import com.anyilanxin.kunpeng.cluster.utils.logging.ContextualLoggerFactory;
import com.anyilanxin.kunpeng.cluster.utils.logging.LoggerContext;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/** {@link Partition} server. */
public class RaftPartitionServer implements Managed<RaftPartitionServer>, HealthMonitorable {

  @Override
  public String getName() {
    return partition.name();
  }

  private final Logger log;

  private final MemberId localMemberId;
  private final RaftPartition partition;
  private final RaftPartitionConfig partitionConfig;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService clusterCommunicator;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private final Set<FailureListener> deferredFailureListeners = new CopyOnWriteArraySet<>();
  private final Set<RaftRoleStateListener> deferredRoleStateListeners =
      new CopyOnWriteArraySet<>();
  private final PartitionMetadata partitionMetadata;
  private final Duration requestTimeout;
  private final MeterRegistry meterRegistry;
  private final com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotVault snapshotVault;

  private RaftServer server;
  private ReceivableSnapshotStore persistedSnapshotStore;

  public RaftPartitionServer(
      final RaftPartition partition,
      final RaftPartitionConfig partitionConfig,
      final MemberId localMemberId,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService clusterCommunicator,
      final PartitionMetadata partitionMetadata,
      final MeterRegistry meterRegistry,
      final SnapshotVault snapshotVault) {
    this.partition = partition;
    this.partitionConfig = partitionConfig;
    this.localMemberId = localMemberId;
    this.membershipService = membershipService;
    this.clusterCommunicator = clusterCommunicator;
    log =
        ContextualLoggerFactory.getLogger(
            getClass(),
            LoggerContext.builder(RaftPartitionServer.class).addValue(partition.name()).build());
    this.partitionMetadata = partitionMetadata;
    this.meterRegistry = meterRegistry;
    this.snapshotVault = snapshotVault;
    requestTimeout = partitionConfig.getRequestTimeout();
  }

  @Override
  public CompletableFuture<RaftPartitionServer> start() {
    final RaftStartupMetrics raftStartupMetrics = new RaftStartupMetrics(partition.name(), meterRegistry);
    final long bootstrapStartTime;
    log.info("Starting server for partition {}", partition.id());
    final long startTime = System.currentTimeMillis();
    final CompletableFuture<RaftServer> serverOpenFuture;
    if (partition.members().contains(localMemberId)) {
      if (server != null && server.isRunning()) {
        return CompletableFuture.completedFuture(null);
      }
      synchronized (this) {
        try {
          initServer();

        } catch (final StorageException e) {
          return Futures.exceptionalFuture(e);
        }
      }
      bootstrapStartTime = System.currentTimeMillis();
      serverOpenFuture = server.bootstrap(partition.members());
    } else {
      bootstrapStartTime = System.currentTimeMillis();
      serverOpenFuture = CompletableFuture.completedFuture(null);
    }
    return serverOpenFuture
        .whenComplete(
            (r, e) -> {
              if (e == null) {
                final long endTime = System.currentTimeMillis();
                final long startDuration = endTime - startTime;
                raftStartupMetrics.observeBootstrapDuration(endTime - bootstrapStartTime);
                raftStartupMetrics.observeStartupDuration(startDuration);
                log.info(
                    "Successfully started server for partition {} in {}ms",
                    partition.id(),
                    startDuration);
              } else {
                log.warn("Failed to start server for partition {}", partition.id(), e);
              }
            })
        .thenApply(v -> this);
  }

  @Override
  public boolean isRunning() {
    return server != null && server.isRunning();
  }

  /** Raft 线程上下文（server 未创建时为 null） */
  public com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext getThreadContext() {
    return server != null ? server.getContext().getThreadContext() : null;
  }

  /** 当前已提交日志索引（server 未创建时为 0） */
  public long getCommitIndex() {
    return server != null ? server.getContext().getCommitIndex() : 0;
  }

  /** 首次创建集群（本节点与已知成员形成新 Raft 集群） */
  public CompletableFuture<RaftServer> bootstrap(final java.util.Collection<MemberId> clusterMembers) {
    ensureServer();
    return server.bootstrap(clusterMembers);
  }

  /** 加入已有集群（以 PASSIVE 启动 → 由 leader 提升为 ACTIVE） */
  public CompletableFuture<RaftServer> join(final java.util.Collection<MemberId> clusterMembers) {
    ensureServer();
    return server.join(clusterMembers);
  }

  /** 离开集群（通知 leader 移除自身 → 完整 shutdown） */
  public CompletableFuture<RaftServer> leave() {
    if (server == null) {
      return CompletableFuture.completedFuture(null);
    }
    return server.leave();
  }

  private void ensureServer() {
    if (server == null) {
      synchronized (this) {
        if (server == null) {
          try {
            initServer();
          } catch (final StorageException e) {
            throw new RuntimeException("Failed to initialize Raft server for partition " + partition.id(), e);
          }
        }
      }
    }
  }

  @Override
  public CompletableFuture<Void> stop() {
    return server != null ? server.shutdown() : CompletableFuture.completedFuture(null);
  }

  private void initServer() {
    server = buildServer();

    deferredRoleChangeListeners.forEach(server::addRoleChangeListener);
    deferredRoleChangeListeners.clear();

    deferredFailureListeners.forEach(server::addFailureListener);
    deferredFailureListeners.clear();

    deferredRoleStateListeners.forEach(this::registerRoleStateListener);
    deferredRoleStateListeners.clear();
  }

  private void registerRoleStateListener(final RaftRoleStateListener listener) {
    server.getContext().addRoleStateListener(listener);
  }

  private void unregisterRoleStateListener(final RaftRoleStateListener listener) {
    server.getContext().removeRoleStateListener(listener);
  }

  /** 添加业务角色状态监听器（server 未创建时延迟注册） */
  public void addRoleStateListener(final RaftRoleStateListener listener) {
    if (server == null) {
      deferredRoleStateListeners.add(listener);
    } else {
      registerRoleStateListener(listener);
    }
  }

  /** 移除业务角色状态监听器 */
  public void removeRoleStateListener(final RaftRoleStateListener listener) {
    deferredRoleStateListeners.remove(listener);
    if (server != null) {
      unregisterRoleStateListener(listener);
    }
  }

  private RaftServer buildServer() {
    final var partitionId = partition.id().id();
    persistedSnapshotStore = new com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultSnapshotStore(snapshotVault);

    // partitionConfig 已在构造器中注入

    final var electionConfig =
        partitionConfig.isPriorityElectionEnabled()
            ? RaftElectionConfig.ofPriorityElection(
                partitionMetadata.getTargetPriority(), partitionMetadata.getPriority(localMemberId))
            : RaftElectionConfig.ofDefaultElection();

    return RaftServer.builder(localMemberId)
        .withName(partition.name())
        .withPartitionId(partitionId)
      .withPartitionMetadata(partitionMetadata)
        .withMembershipService(membershipService)
        .withProtocol(createServerProtocol())
        .withPartitionConfig(partitionConfig)
        .withStorage(createRaftStorage())
        .withMeterRegistry(meterRegistry)
        .withEntryValidator(null) // TODO: entryValidator 由工厂体系提供
        .withElectionConfig(electionConfig)
        .build();
  }

  public CompletableFuture<Void> goInactive() {
    return server.goInactive();
  }

  /**
   * Takes a snapshot of the partition server.
   *
   * @return a future to be completed once the snapshot has been taken
   */
  public CompletableFuture<Void> snapshot() {
    return server.compact();
  }

  public void setCompactableIndex(final long index) {
    server.getContext().getLogCompactor().setCompactableIndex(index);
  }

  public RaftLogReader openReader() {
    return server.getContext().getLog().openCommittedReader();
  }

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    if (server == null) {
      deferredRoleChangeListeners.add(listener);
    } else {
      server.addRoleChangeListener(listener);
    }
  }

  @Override
  public HealthReport getHealthReport() {
    return server.getContext().getHealthReport();
  }

  @Override
  public void addFailureListener(final FailureListener listener) {
    if (server == null) {
      deferredFailureListeners.add(listener);
    } else {
      server.addFailureListener(listener);
    }
  }

  @Override
  public void removeFailureListener(final FailureListener listener) {
    deferredFailureListeners.remove(listener);
    server.removeFailureListener(listener);
  }

  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    deferredRoleChangeListeners.remove(listener);
    server.removeRoleChangeListener(listener);
  }

  /**
   * @see com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#addCommitListener(RaftCommitListener)
   */
  public void addCommitListener(final RaftCommitListener commitListener) {
    server.getContext().addCommitListener(commitListener);
  }

  /**
   * @see com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#removeCommitListener(RaftCommitListener)
   */
  public void removeCommitListener(final RaftCommitListener commitListener) {
    server.getContext().removeCommitListener(commitListener);
  }

  /**
   * @see com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#addCommittedEntryListener(RaftCommittedEntryListener)
   */
  public void addCommittedEntryListener(final RaftCommittedEntryListener commitListener) {
    server.getContext().addCommittedEntryListener(commitListener);
  }

  /**
   * @see com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext#removeCommittedEntryListener(RaftCommittedEntryListener)
   */
  public void removeCommittedEntryListener(final RaftCommittedEntryListener commitListener) {
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

  public PersistedSnapshotStore getPersistedSnapshotStore() {
    return persistedSnapshotStore;
  }

  /** Deletes the server. */
  public void delete() {
    try {
      Files.walkFileTree(
          partition.dataDirectory().toPath(),
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (final IOException e) {
      log.error("Failed to delete partition: {}", partition, e);
    }
  }

  public Optional<ZeebeLogAppender> getAppender() {
    final RaftRole role = server.getContext().getRaftRole();
    if (role instanceof ZeebeLogAppender) {
      return Optional.of((ZeebeLogAppender) role);
    }

    return Optional.empty();
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
    // storageConfig 已在构造器中注入
    return RaftStorage.builder()
        .withPrefix(partition.name())
        .withDirectory(partition.dataDirectory())
      .withMaxSegmentSize(partitionConfig.getSegmentSize())
        .withFlushExplicitly(partitionConfig.shouldFlushExplicitly())
        .withFreeDiskSpace(partitionConfig.getFreeDiskSpace())
        .withSnapshotStore(persistedSnapshotStore)
        .withJournalIndexDensity(partitionConfig.getJournalIndexDensity())
        .withMeterRegistry(meterRegistry)
        .build();
  }

  private RaftServerCommunicator createServerProtocol() {
    return new RaftServerCommunicator(
        partition.name(),
        meterRegistry,
        Serializer.using(RaftNamespaces.RAFT_PROTOCOL),
        clusterCommunicator,
        requestTimeout);
  }

  public CompletableFuture<Void> stepDown() {
    return server.stepDown();
  }

  public CompletableFuture<RaftServer> promote() {
    return server.promote();
  }
}
