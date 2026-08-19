/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.primitive.partition.impl;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.primitive.partition.ManagedPartitionGroup;
import com.anyilanxin.kunpeng.cluster.primitive.partition.ManagedPartitionService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Default partition service. */
public class DefaultPartitionService implements ManagedPartitionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPartitionService.class);

  private final ClusterMembershipService clusterMembershipService;
  private final ClusterCommunicationService communicationService;
  private volatile PartitionManagementService partitionManagementService;
  private final ManagedPartitionGroup group;
  private final AtomicBoolean started = new AtomicBoolean();

  public DefaultPartitionService(
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService messagingService,
      final ManagedPartitionGroup group) {
    clusterMembershipService = membershipService;
    communicationService = messagingService;
    this.group = group;
  }

  @Override
  public ManagedPartitionGroup getPartitionGroup() {
    return group;
  }

  @Override
  public CompletableFuture<PartitionService> start() {
    if (started.compareAndSet(false, true)) {

      partitionManagementService =
          new DefaultPartitionManagementService(clusterMembershipService, communicationService);

      final var startStepFuture =
          group != null
              ? group.join(partitionManagementService)
              : CompletableFuture.completedFuture(null);

      return startStepFuture.thenApply(
          v -> {
            LOGGER.debug("Started {}", getClass());
            started.set(true);
            return this;
          });
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<Void> stop() {
    final var stopStepFuture =
        group != null ? group.close() : CompletableFuture.completedFuture(null);

    return stopStepFuture
        .exceptionally(
            throwable -> {
              LOGGER.error("Failed closing partition group(s)", throwable);
              return null;
            })
        .thenRun(
            () -> {
              LOGGER.info("Stopped");
              started.set(false);
            });
  }
}
