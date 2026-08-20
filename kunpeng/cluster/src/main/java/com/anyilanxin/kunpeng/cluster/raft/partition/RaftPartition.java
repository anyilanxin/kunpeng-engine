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
import com.anyilanxin.kunpeng.cluster.raft.RaftCommitListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.raft.orchestrator.ManagedPartitionTopologyService;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotVault;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduled;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
  private final File dataDirectory;
  private final MeterRegistry meterRegistry;
  private final SnapshotVault snapshotVault;
  private final com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler snapshotHandler;
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicationService;
  private final ManagedPartitionTopologyService topologyService;
  private final Set<RaftRoleChangeListener> deferredRoleChangeListeners =
      new CopyOnWriteArraySet<>();
  private final Set<FailureListener> deferredFailureListeners = new CopyOnWriteArraySet<>();
  private final Set<RaftCommitListener> deferredCommitListeners =
      new CopyOnWriteArraySet<>();
  private volatile RaftPartitionServer server;
  private volatile Scheduled snapshotTimer;
  private volatile boolean snapshotScheduleEnabled;
  /** 进行中的快照流程（同一分区同时只允许一个；并发调用复用在途 future） */
  private final AtomicReference<CompletableFuture<Void>> inFlightSnapshot =
      new AtomicReference<>();

  public RaftPartition(
      final PartitionMetadata metadata,
      final RaftPartitionConfig config,
      final File dataDirectory,
      final MeterRegistry meterRegistry,
      final SnapshotHandler snapshotHandler,
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicationService,
      final ManagedPartitionTopologyService topologyService) {
    this.metadata = metadata;
    this.config = config;
    this.dataDirectory = dataDirectory;
    this.meterRegistry = meterRegistry;
    this.snapshotHandler = snapshotHandler;
    snapshotVault =
      new SnapshotVault(dataDirectory.toPath(), snapshotHandler, meterRegistry);
    this.membershipService = membershipService;
    this.communicationService = communicationService;
    this.topologyService = topologyService;
    // 注册分区拓扑服务：角色/故障变化时携带本分区元数据上报并广播
    if (topologyService != null) {
      deferredRoleChangeListeners.add(topologyService);
      deferredFailureListeners.add(topologyService);
    }
  }

  /** 快照操作处理器（由业务层实现） */
  public com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler snapshotHandler() {
    return snapshotHandler;
  }

  // ===== 生命周期 =====

  /**
   * 首次创建集群：本节点与已知成员形成新 Raft 集群。
   *
   * <p>仅当本节点是分区成员时启动 raft server；传入<b>全量</b>成员列表
   * （不可过滤本机——重启时需与持久化配置在 ensureConfigurationIsConsistent
   * 中比对成员数与成员一致性）。
   */
  public CompletableFuture<RaftPartition> bootstrap() {
    if (!metadata.members().contains(getLocalMemberId())) {
      return CompletableFuture.completedFuture(this);
    }
    LOG.info("Bootstrap partition: {} (members={})", name(), metadata.members());
    final var srv = getOrCreateServer();
    return srv.bootstrap(metadata.members())
        .thenApply(v -> {
          // server 就绪后自动启动周期快照
          startSnapshotSchedule();
          return this;
        });
  }

  /**
   * 首次创建集群并从指定分区拉取引导快照。
   *
   * <p>执行流程：
   * <ol>
   *   <li>正常 bootstrap——本节点与已知成员形成 Raft 集群</li>
   *   <li>bootstrap 完成后——从 {@code sourcePartitionId} 指定的分区拉取引导快照：
   *       <ul>
   *         <li>源分区节点调用其 {@code SnapshotHandler.onTakeBootstrapSnapshot} 产生数据</li>
   *         <li>快照数据传输到本节点的 bootstrap 快照区</li>
   *       </ul>
   *   </li>
   *   <li>快照接收完成——本分区调用 {@code SnapshotHandler.onRecoverFromSnapshot} 恢复业务状态</li>
   * </ol>
   *
   * @param sourcePartitionId 引导快照的来源分区（可以是同组或跨组的分区）
   * @param sourceNode 源分区所在节点
   * @return 引导快照恢复完成后返回本分区
   */
  public CompletableFuture<RaftPartition> bootstrap(
      final PartitionId sourcePartitionId,
      final com.anyilanxin.kunpeng.cluster.cluster.MemberId sourceNode) {
    LOG.info("Bootstrap partition {} with bootstrap snapshot from {} (node={})",
        name(), sourcePartitionId, sourceNode);
    return bootstrap()
        .thenCompose(v -> pullBootstrapSnapshot(sourceNode))
        .thenCompose(archivedSnapshot -> {
          if (archivedSnapshot != null) {
            LOG.info("Bootstrap snapshot received from {}, recovering", sourcePartitionId);
            return recoverFromSnapshot(archivedSnapshot).thenApply(ignored -> this);
          }
          LOG.warn("No bootstrap snapshot available from {}", sourcePartitionId);
          return CompletableFuture.completedFuture(this);
        });
  }

  /**
   * 启动引导快照服务（在源分区的 leader 节点调用）：
   * 监听引导请求 → 若无引导快照则创建 → 逐块返回 SnapshotBlock。
   *
   * <p>由 orchestrator 在分区就绪后调用。
   */
  public CompletableFuture<Void> startBootstrapServing() {
    final var service = getOrCreateBootstrapTransferService();
    return service.startServing();
  }

  /** 停止引导快照服务 */
  public void stopBootstrapServing() {
    final var srv = bootstrapTransferService;
    if (srv != null) {
      srv.stopServing();
    }
  }

  private com.anyilanxin.kunpeng.cluster.raft.snapshot.BootstrapTransferService bootstrapTransferService;

  private com.anyilanxin.kunpeng.cluster.raft.snapshot.BootstrapTransferService
      getOrCreateBootstrapTransferService() {
    var svc = bootstrapTransferService;
    if (svc == null) {
      svc = new com.anyilanxin.kunpeng.cluster.raft.snapshot.BootstrapTransferService(
          communicationService, snapshotVault, snapshotHandler, metadata.id());
      bootstrapTransferService = svc;
    }
    return svc;
  }

  /**
   * 从指定分区的指定节点拉取引导快照：init → 逐块拉取 → 写入本地 vault → 落档。
   *
   * <p>源分区节点需已调用 {@link #startBootstrapServing()}。
   *
   * @param sourceNode 源分区所在节点
   * @return 接收落档后的快照；不可用时返回 null
   */
  public CompletableFuture<com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot>
      pullBootstrapSnapshot(final com.anyilanxin.kunpeng.cluster.cluster.MemberId sourceNode) {
    final var service = getOrCreateBootstrapTransferService();
    return service.pullBootstrapSnapshot(sourceNode);
  }

  /**
   * 加入已有集群：以 PASSIVE 启动 → 向已知成员发 ReconfigureRequest →
   * leader 接受后将本节点写入配置 → 通过日志复制收到新配置 → 提升为 ACTIVE。
   *
   * <p>仅当本节点是分区成员时启动 raft server；参数为<b>非本机</b>的成员列表
   * （join 的联系对象，不含本机）。
   */
  public CompletableFuture<RaftPartition> join() {
    if (!metadata.members().contains(getLocalMemberId())) {
      return CompletableFuture.completedFuture(this);
    }
    final var contactMembers = metadata.members().stream()
        .filter(id -> !id.equals(getLocalMemberId()))
        .toList();
    LOG.info("Join partition: {} (contacts={})", name(), contactMembers);
    final var srv = getOrCreateServer();
    return srv.join(contactMembers)
        .thenApply(v -> {
          // server 就绪后自动启动周期快照
          startSnapshotSchedule();
          return this;
        });
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

  /**
   * 关闭前阶段：主动拍摄一次终局快照 → 关闭业务资源（{@code SnapshotHandler.onClose}）。
   *
   * <p>必须在 {@link #close()} 之前调用——业务需在 raft server 停止前完成
   * 终局快照写入与资源释放。
   */
  public CompletableFuture<Void> closeHandler() {
    return takeCloseSnapshot().thenCompose(v -> closeSnapshotHandler());
  }

  /**
   * 启动周期快照（bootstrap/join 完成后自动调用，幂等）：到达
   * {@code RaftPartitionConfig.snapshotInterval} 周期时自动经
   * {@code SnapshotHandler.onTakeSnapshot} 拍摄（非正数周期表示关闭）。
   */
  private void startSnapshotSchedule() {
    if (snapshotScheduleEnabled) {
      return;
    }
    final var interval = config.getSnapshotInterval();
    if (interval == null || !interval.isPositive()) {
      return;
    }
    final var srv = server;
    if (srv == null) {
      return;
    }
    final ThreadContext threadContext = srv.getThreadContext();
    if (threadContext == null) {
      return;
    }
    snapshotScheduleEnabled = true;
    scheduleNextSnapshot(threadContext, interval);
  }

  /** 停止周期快照 */
  public void stopSnapshotSchedule() {
    snapshotScheduleEnabled = false;
    final var timer = snapshotTimer;
    if (timer != null) {
      timer.cancel();
      snapshotTimer = null;
    }
  }

  private void scheduleNextSnapshot(final ThreadContext threadContext, final Duration interval) {
    snapshotTimer = threadContext.schedule(interval, () -> {
      if (!snapshotScheduleEnabled) {
        return;
      }
      takePeriodicSnapshot();
      scheduleNextSnapshot(threadContext, interval);
    });
  }

  /** 周期快照（best-effort）：上一轮仍在进行则跳过本轮 */
  private void takePeriodicSnapshot() {
    if (inFlightSnapshot.get() != null) {
      LOG.debug("Skip periodic snapshot for {}, previous snapshot still in flight", name());
      return;
    }
    LOG.info("Take periodic snapshot for {}", name());
    takeSnapshot(true).exceptionally(error -> {
      LOG.warn("Periodic snapshot failed for {}", name(), error);
      return null;
    });
  }

  /** 关闭分区（不离开集群，仅停止本节点的 Raft 服务） */
  public CompletableFuture<Void> close() {
    stopSnapshotSchedule();
    final var srv = server;
    if (srv != null) {
      return srv.stop().exceptionally(error -> {
        LOG.error("Error on shutdown partition: {}", metadata.id(), error);
        return null;
      });
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * 终局快照：关闭前主动拍摄一次（失败不阻断关闭）
   */
  private CompletableFuture<Void> takeCloseSnapshot() {
    if (snapshotHandler == null) {
      return CompletableFuture.completedFuture(null);
    }
    LOG.info("Take final snapshot on close for {}", name());
    return takeSnapshot(true).exceptionally(error -> {
      LOG.warn("Final snapshot on close failed for {}, continuing shutdown", name(), error);
      return null;
    });
  }

  /**
   * 关闭业务系统资源
   */
  private CompletableFuture<Void> closeSnapshotHandler() {
    if (snapshotHandler == null) {
      return CompletableFuture.completedFuture(null);
    }
    return snapshotHandler.onClose().toCompletableFuture().exceptionally(error -> {
      LOG.error("Error on closing snapshot handler for {}", metadata.id(), error);
      return null;
    });
  }

  /** 删除分区（停止 + 清理数据） */
  public CompletableFuture<Void> delete() {
    stopSnapshotSchedule();
    final var srv = server;
    if (srv != null) {
      return srv.stop().thenRun(srv::delete);
    }
    return CompletableFuture.completedFuture(null);
  }

  // ===== 快照操作（外部触发 → 内部编排 handler 回调） =====

  /**
   * 主动拍摄快照（用户调用）：立即拍摄一次；
   * 已有快照流程进行中时复用其 future，不会并发拍摄。
   *
   * @return 完成即快照流程结束（失败以异常完成，由调用方处理）
   */
  public CompletableFuture<Void> takeSnapshotNow() {
    LOG.info("Take snapshot now for {}", name());
    return takeSnapshot(true);
  }

  /**
   * 拍摄常规快照（外部触发）：vault 暂存 → {@code handler.onTakeSnapshot(dir)} 写入业务数据
   * → vault 提交落档 → 可选复制到 bootstrap/merge 副本区。
   *
   * <p>之后可由 Leader 通过 Install 协议发送给落后节点，或由本节点日志压缩时使用。
   *
   * @param force 是否强制拍摄（忽略"已有更新快照"检查）
   * @return 完成即快照已落档；已有快照流程进行中时复用其 future
   */
  public CompletableFuture<Void> takeSnapshot(final boolean force) {
    return exclusiveSnapshot(() -> {
      final long index = getCurrentIndex();
      final long term = getCurrentTerm();
      LOG.info("Take snapshot for {} (index={}, term={})", name(), index, term);
      return snapshotVault
          .stage(index, term, getLocalMemberId().id(), force)
          .thenCompose(staged ->
              snapshotVault.capture(staged, dir ->
                  snapshotHandler.onTakeSnapshot(dir).toCompletableFuture()));
    });
  }

  /**
   * 拍摄引导快照（外部触发，源节点）：vault 暂存 →
   * {@code handler.onTakeBootstrapSnapshot(dir)} 写入业务数据 → vault 提交落档 →
   * 复制到 bootstrap 副本区 → 由 BootstrapReplicaSender 传输到目标节点。
   *
   * <p>目标节点接收后调用 {@link #recoverFromSnapshot} 恢复。
   *
   * @return 完成即快照已落档并复制到 bootstrap 区；已有快照流程进行中时复用其 future
   */
  public CompletableFuture<Void> takeBootstrapSnapshot() {
    return exclusiveSnapshot(() -> {
      final long index = getCurrentIndex();
      final long term = getCurrentTerm();
      LOG.info("Take bootstrap snapshot for {} (index={}, term={})", name(), index, term);
      return snapshotVault
          .stage(index, term, getLocalMemberId().id(), true)
          .thenCompose(staged ->
              snapshotVault.capture(staged, dir ->
                  snapshotHandler.onTakeBootstrapSnapshot(dir).toCompletableFuture()))
          .thenCompose(v -> {
            final var latest = snapshotVault.getLatestSnapshot();
            if (latest.isPresent()) {
              return snapshotVault.copyForBootstrap(latest.get().ref().toString());
            }
            return CompletableFuture.completedFuture(null);
          });
    });
  }

  /**
   * 从快照恢复（外部触发，目标节点）：调用 {@code handler.onRecoverFromSnapshot} 恢复业务状态。
   *
   * <p>两种调用场景：
   * <ol>
   *   <li>初始引导——新节点从引导快照恢复</li>
   *   <li>Leader 发送——本节点落后过多，Leader Install 协议传输后恢复</li>
   * </ol>
   *
   * @param archivedSnapshot 已落档的快照
   * @return 恢复完成
   */
  public CompletableFuture<Void> recoverFromSnapshot(
      final com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot archivedSnapshot) {
    LOG.info("Recover from snapshot {} for {}", archivedSnapshot.ref(), name());
    return snapshotHandler.onRecoverFromSnapshot(archivedSnapshot).toCompletableFuture();
  }

  /**
   * 拍摄迁移快照（外部触发，源分区）：vault 暂存 →
   * {@code handler.onTakeMergeSnapshot(dir)} 写入业务数据 → vault 提交 →
   * 复制到 merge 副本区 → 由 MergeReplicaSender 传输到目标分区。
   *
   * @return 完成即快照已落档并复制到 merge 区；已有快照流程进行中时复用其 future
   */
  public CompletableFuture<Void> takeMergeSnapshot() {
    return exclusiveSnapshot(() -> {
      final long index = getCurrentIndex();
      final long term = getCurrentTerm();
      LOG.info("Take merge snapshot for {} (index={}, term={})", name(), index, term);
      return snapshotVault
          .stage(index, term, getLocalMemberId().id(), true)
          .thenCompose(staged ->
              snapshotVault.capture(staged, dir ->
                  snapshotHandler.onTakeMergeSnapshot(dir).toCompletableFuture()))
          .thenCompose(v -> {
            final var latest = snapshotVault.getLatestSnapshot();
            if (latest.isPresent()) {
              return snapshotVault.copyForMerge(latest.get().ref().toString());
            }
            return CompletableFuture.completedFuture(null);
          });
    });
  }

  /**
   * 合并迁移快照（外部触发，目标分区）：调用 {@code handler.onMergeSnapshot} 合并远端状态。
   *
   * @param archivedSnapshot 已接收落档的迁移快照
   * @return 合并完成
   */
  public CompletableFuture<Void> mergeSnapshot(
      final com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot archivedSnapshot) {
    LOG.info("Merge snapshot {} for {}", archivedSnapshot.ref(), name());
    return snapshotHandler.onMergeSnapshot(archivedSnapshot).toCompletableFuture();
  }

  /**
   * 快照互斥执行：同一分区同时只允许一个快照流程。
   *
   * <p>已有流程进行中时，后续调用直接返回该在途 future（不重复拍摄）。
   */
  private CompletableFuture<Void> exclusiveSnapshot(
      final Supplier<CompletableFuture<Void>> flow) {
    if (inFlightSnapshot.get() != null) {
      return inFlightSnapshot.get();
    }
    final var created = new CompletableFuture<Void>();
    if (inFlightSnapshot.compareAndSet(null, created)) {
      flow.get().whenComplete((v, error) -> {
        inFlightSnapshot.compareAndSet(created, null);
        if (error == null) {
          created.complete(null);
        } else {
          created.completeExceptionally(error);
        }
      });
      return created;
    }
    return inFlightSnapshot.get();
  }

  /** 快照索引取当前 raft 提交索引（server 未创建时回退最近快照索引） */
  private long getCurrentIndex() {
    final var srv = server;
    if (srv != null) {
      return srv.getCommitIndex();
    }
    return snapshotVault.getCurrentSnapshotIndex();
  }

  private long getCurrentTerm() {
    final var srv = server;
    return srv != null ? srv.getTerm() : 0;
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
    return new RaftPartitionServer(
        this,
        config,
        getLocalMemberId(),
        membershipService,
        communicationService,
        metadata,
        meterRegistry,
        snapshotVault);
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
    if (!deferredCommitListeners.isEmpty()) {
      deferredCommitListeners.forEach(srv::addCommitListener);
      deferredCommitListeners.clear();
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

  public void addCommitListener(final RaftCommitListener listener) {
    final var srv = server;
    if (srv != null) {
      srv.addCommitListener(listener);
    } else {
      deferredCommitListeners.add(listener);
    }
  }

  public void removeCommitListener(final RaftCommitListener listener) {
    deferredCommitListeners.remove(listener);
    final var srv = server;
    if (srv != null) {
      srv.removeCommitListener(listener);
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
