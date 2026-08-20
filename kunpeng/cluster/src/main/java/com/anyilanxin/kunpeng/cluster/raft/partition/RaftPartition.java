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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChecksumProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChecksumProviderFactory;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raft 分区：自包含的生命周期管理单元。
 *
 * <p>{@link PartitionMetadata} 已包含分区组信息（组名在 {@code PartitionId.group()}），
 * 因此无需外层的 RaftPartitionGroup——分区组的成员分配与元数据由
 * {@code RaftOrchestrationService} 持有和管理，本类只关注单个分区的 Raft 生命周期。
 *
 * <p>生命周期方法：
 * <ul>
 *   <li>{@link #bootstrap()}——首次创建集群（本节点 + 已知成员形成新集群）</li>
 *   <li>{@link #join()}——加入已有集群（以 PASSIVE 启动，由 leader 提升为 ACTIVE）</li>
 *   <li>{@link #leave()}——离开集群（通知 leader 移除自身 → 完整 shutdown）</li>
 * </ul>
 */
public class RaftPartition implements Partition, HealthMonitorable {

  private static final Logger LOG = LoggerFactory.getLogger(RaftPartition.class);
  private static final String PARTITION_NAME_FORMAT = "%s-partition-%d";

  private final PartitionMetadata metadata;
  private final RaftPartitionConfig config;
  private final RaftStorageConfig storageConfig;
  private final File dataDirectory;
  private final MeterRegistry meterRegistry;
  private final SnapshotChecksumProviderFactory checksumProviderFactory;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicationService;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private final Set<FailureListener> deferredFailureListeners = new CopyOnWriteArraySet<>();
  private volatile RaftPartitionServer server;

  public RaftPartition(
      final PartitionMetadata metadata,
      final RaftPartitionConfig config,
      final RaftStorageConfig storageConfig,
      final File dataDirectory,
      final MeterRegistry meterRegistry,
      final SnapshotChecksumProviderFactory checksumProviderFactory,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicationService) {
    this.metadata = metadata;
    this.config = config;
    this.storageConfig = storageConfig;
    this.dataDirectory = dataDirectory;
    this.meterRegistry = meterRegistry;
    this.checksumProviderFactory = checksumProviderFactory;
    this.membershipService = membershipService;
    this.communicationService = communicationService;
  }

  // ===== 生命周期 =====

  /**
   * 首次创建集群：本节点与已知成员形成新 Raft 集群。
   *
   * <p>仅适用于尚无集群的场景（首次部署或数据目录为空）。
   */
  public CompletableFuture<RaftPartition> bootstrap() {
    LOG.info("Bootstrap partition: {} (members={})", name(), metadata.members());
    final var srv = getOrCreateServer();
    final var clusterMembers = metadata.members().stream()
        .filter(id -> !id.equals(getLocalMemberId()))
        .toList();
    return srv.bootstrap(clusterMembers).thenApply(v -> this);
  }

  /**
   * 加入已有集群：以 PASSIVE 启动 → 向已知成员发 ReconfigureRequest →
   * leader 接受后将本节点写入配置 → 通过日志复制收到新配置 → 提升为 ACTIVE。
   */
  public CompletableFuture<RaftPartition> join() {
    LOG.info("Join partition: {} (members={})", name(), metadata.members());
    final var srv = getOrCreateServer();
    final var clusterMembers = metadata.members().stream()
        .filter(id -> !id.equals(getLocalMemberId()))
        .toList();
    return srv.join(clusterMembers).thenApply(v -> this);
  }

  /**
   * 离开集群：向 leader 宣告离开（best-effort）→ 完整 shutdown（停定时器 →
   * INACTIVE → 关闭日志/存储/快照/网络）。
   */
  public CompletableFuture<RaftPartition> leave() {
    LOG.info("Leave partition: {}", name());
    final var srv = server;
    if (srv == null) {
      return CompletableFuture.completedFuture(this);
    }
    return srv.leave().thenApply(v -> this);
  }

  /** 关闭分区（不离开集群，仅停止本节点的 Raft 服务） */
  public CompletableFuture<Void> close() {
    final var srv = server;
    if (srv != null) {
      return srv.stop().exceptionally(error -> {
        LOG.error("Error on shutdown partition: {}", metadata.id(), error);
        return null;
      });
    }
    return CompletableFuture.completedFuture(null);
  }

  /** 删除分区（停止 + 清理数据） */
  public CompletableFuture<Void> delete() {
    final var srv = server;
    if (srv != null) {
      return srv.stop().thenRun(() -> srv.delete());
    }
    return CompletableFuture.completedFuture(null);
  }

  // ===== 内部 =====

  private MemberId getLocalMemberId() {
    return membershipService.getLocalMember().id();
  }

  private RaftPartitionServer getOrCreateServer() {
    var srv = server;
    if (srv == null) {
      synchronized (this) {
        srv = server;
        if (srv == null) {
          srv = createServer();
          server = srv;
          flushDeferredListeners(srv);
        }
      }
    }
    return srv;
  }

  private RaftPartitionServer createServer() {
    final SnapshotChecksumProvider checksumProvider =
        checksumProviderFactory != null
            ? checksumProviderFactory.create(metadata.id().id(), dataDirectory.toPath())
            : null;
    return new RaftPartitionServer(
        this,
        config,
        storageConfig,
        getLocalMemberId(),
        membershipService,
        communicationService,
        metadata,
        meterRegistry,
        checksumProvider);
  }

  private void flushDeferredListeners(final RaftPartitionServer srv) {
    if (!deferredRoleChangeListeners.isEmpty()) {
      deferredRoleChangeListeners.forEach(srv::addRoleChangeListener);
      deferredRoleChangeListeners.clear();
    }
    if (!deferredFailureListeners.isEmpty()) {
      deferredFailureListeners.forEach(srv::addFailureListener);
      deferredFailureListeners.clear();
    }
  }

  // ===== 查询 =====

  /** 分区名（组名-分区号） */
  public String name() {
    return String.format(PARTITION_NAME_FORMAT, metadata.id().group(), metadata.id().id());
  }

  /** 分区元数据（含组名、成员、优先级、primary） */
  public PartitionMetadata metadata() {
    return metadata;
  }

  /** 分区数据目录 */
  public File dataDirectory() {
    return dataDirectory;
  }

  /** 分区配置 */
  public RaftPartitionConfig config() {
    return config;
  }

  /** 拍快照 */
  public CompletableFuture<Void> snapshot() {
    final var srv = server;
    return srv != null ? srv.snapshot() : CompletableFuture.completedFuture(null);
  }

  /** 步退（Leader → Follower） */
  public CompletableFuture<Void> stepDown() {
    final var srv = server;
    return srv != null ? srv.stepDown() : CompletableFuture.completedFuture(null);
  }

  /** 转入 INACTIVE（不离开集群） */
  public CompletableFuture<Void> goInactive() {
    final var srv = server;
    return srv != null ? srv.goInactive() : CompletableFuture.completedFuture(null);
  }

  // ===== Partition 接口 =====

  @Override
  public PartitionId id() {
    return metadata.id();
  }

  @Override
  public String getName() {
    return name();
  }

  @Override
  public long term() {
    final var srv = server;
    return srv != null ? srv.getTerm() : 0;
  }

  @Override
  public Collection<MemberId> members() {
    return metadata.members();
  }

  // ===== HealthMonitorable 接口 =====

  @Override
  public HealthReport getHealthReport() {
    final var srv = server;
    return srv != null ? srv.getHealthReport() : HealthReport.healthy(this);
  }

  @Override
  public void addFailureListener(final FailureListener listener) {
    final var srv = server;
    if (srv != null) {
      srv.addFailureListener(listener);
    } else {
      deferredFailureListeners.add(listener);
    }
  }

  @Override
  public void removeFailureListener(final FailureListener listener) {
    deferredFailureListeners.remove(listener);
    final var srv = server;
    if (srv != null) {
      srv.removeFailureListener(listener);
    }
  }

  // ===== 事件 =====

  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    final var srv = server;
    if (srv != null) {
      srv.addRoleChangeListener(listener);
    } else {
      deferredRoleChangeListeners.add(listener);
    }
  }

  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    deferredRoleChangeListeners.remove(listener);
    final var srv = server;
    if (srv != null) {
      srv.removeRoleChangeListener(listener);
    }
  }

  public Role getRole() {
    final var srv = server;
    return srv != null ? srv.getRole() : null;
  }

  public RaftPartitionServer getServer() {
    return server;
  }

  @Override
  public String toString() {
    return "RaftPartition{id=" + metadata.id() + ", members=" + metadata.members() + "}";
  }
}
