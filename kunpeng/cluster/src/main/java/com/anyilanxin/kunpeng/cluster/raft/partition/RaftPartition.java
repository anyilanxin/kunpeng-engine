/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import io.micrometer.core.instrument.MeterRegistry;
import com.anyilanxin.kunpeng.cluster.primitive.partition.Partition;
import com.anyilanxin.kunpeng.cluster.primitive.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.primitive.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.primitive.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.MoreObjects.toStringHelper;

/** Abstract partition. */
public class RaftPartition implements Partition, HealthMonitorable {

  private static final Logger LOG = LoggerFactory.getLogger(RaftPartition.class);
  private static final String PARTITION_NAME_FORMAT = "%s-partition-%d";
  private final PartitionId partitionId;
  private final RaftPartitionGroupConfig config;
  private final File dataDirectory;
  private final MeterRegistry meterRegistry;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private PartitionMetadata partitionMetadata;
  private RaftPartitionServer server;

  public RaftPartition(
      final PartitionId partitionId,
      final RaftPartitionGroupConfig config,
      final File dataDirectory,
      final MeterRegistry meterRegistry) {
    this.partitionId = partitionId;
    this.config = config;
    this.dataDirectory = dataDirectory;
    this.meterRegistry = meterRegistry;
  }

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    if (server == null) {
      deferredRoleChangeListeners.add(listener);
    } else {
      server.addRoleChangeListener(listener);
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

  /**
   * Takes a snapshot of the partition.
   *
   * @return a future to be completed once the snapshot is complete
   */
  public CompletableFuture<Void> snapshot() {
    final RaftPartitionServer server = this.server;
    if (server != null) {
      return server.snapshot();
    }
    return CompletableFuture.completedFuture(null);
  }

  /** Opens the partition. */
  CompletableFuture<Partition> open(
      final PartitionMetadata metadata, final PartitionManagementService managementService) {
    partitionMetadata = metadata;
    if (partitionMetadata
        .members()
        .contains(managementService.getMembershipService().getLocalMember().id())) {
      initServer(managementService);
      return server.start().thenApply(v -> null);
    }
    return CompletableFuture.completedFuture(this);
  }

  private void initServer(final PartitionManagementService managementService) {
    server = createServer(managementService);

    if (!deferredRoleChangeListeners.isEmpty()) {
      deferredRoleChangeListeners.forEach(server::addRoleChangeListener);
      deferredRoleChangeListeners.clear();
    }
  }

  /** Creates a Raft server. */
  protected RaftPartitionServer createServer(final PartitionManagementService managementService) {
    return new RaftPartitionServer(
        this,
        config,
        managementService.getMembershipService().getLocalMember().id(),
        managementService.getMembershipService(),
        managementService.getMessagingService(),
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
  public String getName() {
    return name();
  }

  @Override
  public HealthReport getHealthReport() {
    return server.getHealthReport();
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
  CompletableFuture<Void> close() {
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
    return partitionMetadata != null ? partitionMetadata.members() : Collections.emptyList();
  }

  public Role getRole() {
    return server != null ? server.getRole() : null;
  }

  public RaftPartitionServer getServer() {
    return server;
  }

  public CompletableFuture<Void> stepDown() {
    return server.stepDown();
  }

  /**
   * Tries to step down if the following conditions are met:
   *
   * <ul>
   *   <li>priority election is enabled
   *   <li>the partition distributor determined a primary node
   *   <li>this node is not the primary
   * </ul>
   */
  public CompletableFuture<Void> stepDownIfNotPrimary() {
    if (shouldStepDown()) {
      return stepDown();
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  private boolean shouldStepDown() {
    final var primary = partitionMetadata.getPrimary();
    final var partitionConfig = config.getPartitionConfig();
    return server != null
        && partitionConfig.isPriorityElectionEnabled()
        && primary.isPresent()
        && primary.get() != server.getMemberId();
  }

  public CompletableFuture<Void> goInactive() {
    return server.goInactive();
  }
}
