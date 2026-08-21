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
import com.anyilanxin.kunpeng.cluster.raft.partition.Partition;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleStateListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotProvider;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
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
  private final File dataDirectory;
  private final MeterRegistry meterRegistry;
  /** 业务快照拍摄 SPI，构造时由业务组装层传入；为 null 时不支持业务快照拍摄。 */
  private final SnapshotProvider snapshotProvider;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private final Set<RaftRoleStateListener> deferredRoleStateListeners =
      new CopyOnWriteArraySet<>();
  private final PartitionMetadata partitionMetadata;
  private RaftPartitionServer server;

  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final File dataDirectory,
      final MeterRegistry meterRegistry) {
    this(partitionMetadata, config, dataDirectory, meterRegistry, null);
  }

  public RaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftPartitionConfig config,
      final File dataDirectory,
      final MeterRegistry meterRegistry,
      final SnapshotProvider snapshotProvider) {
    partitionId = partitionMetadata.id();
    this.partitionMetadata = partitionMetadata;
    this.config = config;
    this.dataDirectory = dataDirectory;
    this.meterRegistry = meterRegistry;
    this.snapshotProvider =
        snapshotProvider != null ? snapshotProvider : new SnapshotProvider.NoopSnapshotProvider();
  }

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    if (server == null) {
      deferredRoleChangeListeners.add(listener);
    } else {
      server.addRoleChangeListener(listener);
    }
  }

  /** 拍摄本分区当前已提交位点的业务快照（创建服务时需注入 SnapshotProvider）。 */
  public CompletableFuture<PersistedSnapshot> takeSnapshot() {
    if (server == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Partition server is not initialized; cannot take snapshot"));
    }
    return server.takeSnapshot();
  }

  /**
   * 注册业务三态状态监听器（角色变更与快照复制事件聚合为 LEADER/FOLLOWER/INACTIVE 视图），
   * 注册后立即回调一次当前状态；分区服务器尚未创建时延迟到初始化完成时注册。
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

  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    deferredRoleChangeListeners.remove(listener);
    server.removeRoleChangeListener(listener);
  }

  /**
   * Returns the partition data directory.
   *
   * @return the partition data directory
   */
  public File dataDirectory() {
    return dataDirectory;
  }

  /** Bootstraps a partition. */
  public CompletableFuture<RaftPartition> bootstrap(
      final PartitionManagementService managementService,
      final ReceivableSnapshotStore snapshotStore) {
    if (partitionMetadata
        .members()
        .contains(managementService.getMembershipService().getLocalMember().id())) {
      initServer(managementService, snapshotStore);
      return server.bootstrap().thenApply(v -> this);
    }
    return CompletableFuture.completedFuture(this);
  }

  public CompletableFuture<RaftPartition> join(
      final PartitionManagementService managementService,
      final ReceivableSnapshotStore snapshotStore) {
    initServer(managementService, snapshotStore);
    return server.join().thenApply(v -> this);
  }

  public CompletableFuture<RaftPartition> leave() {
    return server.leave().thenApply(v -> this);
  }

  private void initServer(
      final PartitionManagementService managementService,
      final ReceivableSnapshotStore snapshotStore) {
    server = createServer(managementService, snapshotStore);

    if (!deferredRoleChangeListeners.isEmpty()) {
      deferredRoleChangeListeners.forEach(server::addRoleChangeListener);
      deferredRoleChangeListeners.clear();
    }
    if (!deferredRoleStateListeners.isEmpty()) {
      deferredRoleStateListeners.forEach(server::addRoleStateListener);
      deferredRoleStateListeners.clear();
    }
  }

  /** Creates a Raft server. */
  private RaftPartitionServer createServer(
      final PartitionManagementService managementService,
      final ReceivableSnapshotStore snapshotStore) {
    return new RaftPartitionServer(
        this,
        config,
        managementService.getMembershipService().getLocalMember().id(),
        managementService.getMembershipService(),
        managementService.getMessagingService(),
        snapshotStore,
        partitionMetadata,
        meterRegistry,
        snapshotProvider);
  }

  /**
   * Returns the partition name.
   *
   * @return the partition name
   */
  public String name() {
    return String.format(PARTITION_NAME_FORMAT, partitionId.group(), partitionId.number());
  }

  @Override
  public String componentName() {
    return String.format(PARTITION_COMPONENT_NAME_FORMAT, partitionId.number());
  }

  @Override
  public HealthReport getHealthReport() {
    // name must be overridden otherwise it equals to name()
    return server.getHealthReport().withName(componentName());
  }

  @Override
  public void addFailureListener(final FailureListener failureListener) {
    server.addFailureListener(failureListener);
  }

  @Override
  public void removeFailureListener(final FailureListener failureListener) {
    server.removeFailureListener(failureListener);
  }

  /** Closes the partition. */
  public CompletableFuture<Void> close() {
    return closeServer()
        .exceptionally(
            error -> {
              LOG.error("Error on shutdown partition: {}.", partitionId, error);
              return null;
            });
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
    return server
        .stop()
        .thenRun(
            () -> {
              if (server != null) {
                server.delete();
              }
            });
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
    return server.stepDown();
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

  public CompletableFuture<Void> stop() {
    return server.stop();
  }

  public RaftPartitionConfig getPartitionConfig() {
    return config;
  }
}
