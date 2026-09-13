/*
 * Copyright 2015-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.raft.impl;

import static com.anyilanxin.kunpeng.cluster.utils.concurrent.Threads.namedThreads;
import static com.google.common.base.Preconditions.*;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.*;
import com.anyilanxin.kunpeng.cluster.raft.RaftException.ProtocolException;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.RaftClusterContext;
import com.anyilanxin.kunpeng.cluster.raft.journal.CheckedJournalException.FlushException;
import com.anyilanxin.kunpeng.cluster.raft.journal.SegmentInfo;
import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.metadata.BusinessMetaManager;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftReplicationMetrics;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRoleMetrics;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftServiceMetrics;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RebalanceMetrics;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftElectionConfig;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.protocol.*;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Builder;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.roles.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.StorageException;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.BusinessMetaEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.BusinessMetaStore;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.MetaStore;
import com.anyilanxin.kunpeng.cluster.raft.utils.StateUtil;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.utils.logging.ThrottledLogger;
import com.anyilanxin.kunpeng.utils.CheckedRunnable;
import com.anyilanxin.kunpeng.utils.exception.UnrecoverableException;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.agrona.CloseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Manages the volatile state and state transitions of a Raft server.
 *
 * <p>This class is the primary vehicle for managing the state of a server. All state that is shared
 * across roles (i.e. follower, candidate, leader) is stored in the cluster state. This includes
 * Raft-specific state like the current leader and term, the log, and the cluster configuration.
 */
public class RaftContext implements AutoCloseable, HealthMonitorable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RaftContext.class);

  /**
   * Configuration index returned when no configuration is available, i.e. configuration is null .
   */
  private static final long NO_CONFIGURATION_INDEX = -1L;

  private static final String RAFT_ROLE_KEY = "raft-role";

  protected final String name;
  protected final ThreadContext threadContext;
  protected final ClusterMembershipService membershipService;
  protected final RaftClusterContext cluster;
  protected final RaftServerProtocol protocol;
  protected final RaftStorage storage;
  private final RaftElectionConfig electionConfig;
  private final Set<RaftRoleChangeListener> roleChangeListeners = new CopyOnWriteArraySet<>();
  private final Set<Consumer<State>> stateChangeListeners = new CopyOnWriteArraySet<>();
  private final Set<Consumer<RaftMember>> electionListeners = new CopyOnWriteArraySet<>();
  private final Set<RaftCommitListener> commitListeners = new CopyOnWriteArraySet<>();
  private final Set<RaftApplicationEntryCommittedPositionListener> committedEntryListeners =
      new CopyOnWriteArraySet<>();
  private final Set<SnapshotReplicationListener> snapshotReplicationListeners =
      new CopyOnWriteArraySet<>();
  private final Set<FailureListener> failureListeners = new CopyOnWriteArraySet<>();

  /** 业务三态监听器到其聚合适配器的映射，注销时反查。 */
  private final Map<RaftRoleStateListener, RaftRoleStateAdapter> roleStateAdapters =
      new ConcurrentHashMap<>();

  /** 业务元数据变更监听器（状态实际推进时成对回调 onStarted/onCompleted）。 */
  private final Set<RaftBusinessMetaListener> businessMetaListeners = new CopyOnWriteArraySet<>();

  private final RaftRoleMetrics raftRoleMetrics;
  private final RebalanceMetrics rebalanceMetrics;
  private final RaftReplicationMetrics replicationMetrics;
  private final MetaStore meta;
  private final BusinessMetaStore businessMetaStore;
  private final BusinessMetaManager businessMetaManager;

  /** leader 同步拉取钩子（RaftPartitionServer 装配 BusinessMetaSync 后注册；volatile 支持运行期注入）。 */
  private volatile Runnable businessMetaSyncHook;

  private final RaftLog raftLog;
  private final RaftSnapshotStore persistedSnapshotStore;
  private final LogCompactor logCompactor;
  private volatile State state = State.ACTIVE;
  // Some fields are read by external threads. To ensure thread-safe access, we can use the lock for
  // synchronizing write and reads on such fields.
  private final Object externalAccessLock = new Object();
  private RaftRole role = new InactiveRole(this);
  private volatile MemberId leader;
  private volatile long term;
  private MemberId lastVotedFor;
  private long commitIndex;

  /** 合并窗口内已达 quorum 的最高提交目标；fsync 完成后才真正推进 commitIndex（仅 raft 线程访问）。 */
  private long pendingCommitIndex;

  /** 是否已有合并刷盘任务在 threadContext 队列中排队，防止重复调度。 */
  private boolean commitFlushPending;

  private long firstCommitIndex;
  private volatile boolean started;
  private EntryValidator entryValidator;
  private LeadershipTransferWriteBarrier leadershipTransferWriteBarrier =
      LeadershipTransferWriteBarrier.NONE;
  private LeadershipTransferCoordinatorCheck leadershipTransferCoordinatorCheck =
      LeadershipTransferCoordinatorCheck.NONE;
  // Used for randomizing election timeout
  private final Random random;
  private PersistedSnapshot currentSnapshot;
  private final int snapshotChunkSize;

  private boolean ongoingTransition = false;
  // Keeps track of snapshot replication to notify new listeners about missed events
  private MissedSnapshotReplicationEvents missedSnapshotReplicationEvents =
      MissedSnapshotReplicationEvents.NONE;

  @SuppressWarnings("java:S3077") // allow volatile here, health is immutable
  private volatile HealthReport health;

  private long lastHeartbeat;
  private final RaftPartitionConfig partitionConfig;
  private final PartitionId partitionId;
  private final MeterRegistry meterRegistry;

  /** 单次合并刷盘覆盖的提交条数（保序 group commit 效果观测；batch=1 即未合并）。 */
  private final DistributionSummary commitBatchSize;

  // after firstCommitIndex is set it will be null
  private AwaitingReadyCommitListener awaitingReadyCommitListener;

  public RaftContext(
      final String name,
      final PartitionId partitionId,
      final MemberId localMemberId,
      final ClusterMembershipService membershipService,
      final RaftServerProtocol protocol,
      final RaftStorage storage,
      final RaftThreadContextFactory threadContextFactory,
      final Supplier<Random> randomFactory,
      final RaftElectionConfig electionConfig,
      final RaftPartitionConfig partitionConfig,
      final MeterRegistry meterRegistry) {
    this.name = checkNotNull(name, "name cannot be null");
    this.membershipService = checkNotNull(membershipService, "membershipService cannot be null");
    this.protocol = checkNotNull(protocol, "protocol cannot be null");
    this.storage = checkNotNull(storage, "storage cannot be null");
    random = randomFactory.get();
    this.partitionId = partitionId;
    this.meterRegistry = checkNotNull(meterRegistry, "meterRegistry cannot be null");
    health = HealthReport.healthy(this);

    raftRoleMetrics = new RaftRoleMetrics(name, meterRegistry);
    rebalanceMetrics = new RebalanceMetrics(name, meterRegistry);
    // registry 为进程级共享，必须带 partition 标签，避免同组多分区 meter 相互覆盖
    commitBatchSize =
        DistributionSummary.builder("raft.commit.batch.size")
            .description("单次合并刷盘覆盖的提交条数（保序 group commit 效果观测）")
            .baseUnit("entries")
            .tag("partition", name)
            .register(meterRegistry);

    this.electionConfig = electionConfig;
    if (electionConfig.isPriorityElectionEnabled()) {
      LOGGER.debug(
          "Priority election is enabled with target priority {} and node priority {}",
          electionConfig.getInitialTargetPriority(),
          electionConfig.getNodePriority());
    }

    // Lock the storage directory.
    if (!storage.lock(localMemberId.id())) {
      throw new StorageException(
          "Failed to acquire storage lock; ensure each Raft server is configured with a distinct storage directory");
    }

    threadContext =
        createThreadContext("raft-server", partitionId, threadContextFactory, localMemberId.id());

    // Open the metadata store.
    meta = storage.openMetaStore();

    // Open the business meta store; projection is rebuilt on demand from the log.
    businessMetaStore = storage.openBusinessMetaStore();
    businessMetaManager = new BusinessMetaManager(businessMetaStore);
    businessMetaManager.setStateChangeCallback(this::notifyBusinessMetaChanged);
    businessMetaManager.loadFromStore();
    // 先于 initial setCommitIndex 注册：重启时补放 (0, 持久化 commitIndex]，覆盖「已提交但投影未落盘」窗口
    addCommitListener(this::applyBusinessMetaCommitted);

    // Load the current term and last vote from disk.
    term = meta.loadTerm();
    lastVotedFor = meta.loadVote();
    // Construct the core log, reader, writer, and compactor.
    raftLog =
        storage.openLog(
            meta,
            () ->
                createThreadContext(
                    "raft-log", partitionId, threadContextFactory, localMemberId.id()));

    // Open the snapshot store.
    persistedSnapshotStore = storage.getPersistedSnapshotStore();
    persistedSnapshotStore.addSnapshotListener(this::onNewPersistedSnapshot);
    // Update the current snapshot because the listener only notifies when a new snapshot is
    // created.
    persistedSnapshotStore
        .getLatestSnapshot()
        .ifPresent(persistedSnapshot -> currentSnapshot = persistedSnapshot);
    StateUtil.verifySnapshotLogConsistent(
        partitionId.id(),
        getCurrentSnapshotIndex(),
        raftLog.getFirstIndex(),
        raftLog.isEmpty(),
        raftLog::reset,
        LOGGER);

    logCompactor =
        new LogCompactor(
            threadContext,
            raftLog,
            partitionConfig.getPreferSnapshotReplicationThreshold(),
            new RaftServiceMetrics(name, meterRegistry));

    snapshotChunkSize = partitionConfig.getSnapshotChunkSize();

    this.partitionConfig = partitionConfig;
    cluster = new RaftClusterContext(localMemberId, this);

    replicationMetrics = new RaftReplicationMetrics(name, meterRegistry);
    replicationMetrics.setAppendIndex(raftLog.getLastIndex());
    lastHeartbeat = System.currentTimeMillis();

    // Register protocol listeners.
    registerHandlers(protocol);
    started = true;

    if (meta.hasCommitIndex()) {
      setCommitIndex(meta.commitIndex());
    }

    LOGGER.debug(
        "Server started with term={}, lastVotedFor={}, lastFlushedIndex={}, commitIndex={}",
        term,
        lastVotedFor,
        meta.loadLastFlushedIndex(),
        commitIndex);

    // initialize the listener after setCommitIndex has been called
    awaitingReadyCommitListener = new AwaitingReadyCommitListener();

    if (!raftLog.isEmpty() && term == 0) {
      // This will only happen when metastore is empty because the node has just restored from a
      // backup. Backup only contains the logs. Other case, where this can happen is when the meta
      // file was manually deleted to recover from an unexpected bug.
      // In both cases, we should not restart the term from 0 because the assumption in raft is that
      // the term always increase. After restore, it is safe to restart the term at the last log's
      // term. During the first election, the term will be incremented by 1.
      // In the second case, it is possible that the actual term is higher. But it is still safe to
      // set it to last log's term because the actual term will be set when this node gets the first
      // message from other healthy replicas.
      setTerm(raftLog.getLastEntry().term());
    }
  }

  private ThreadContext createThreadContext(
      final String name,
      final PartitionId partitionId,
      final RaftThreadContextFactory threadContextFactory,
      final String localMemberId) {
    final var context =
        threadContextFactory.createContext(
            namedThreads("%s-%s-%d".formatted(name, localMemberId, partitionId.id()), LOGGER),
            this::onUncaughtException);
    // in order to set the partition id once in the raft thread
    context.execute(
        () -> {
          MDC.put("partitionId", String.valueOf(partitionId.id()));
          // matches Actor.ACTOR_PROP_PHYSICAL_TENANT
          MDC.put("physicalTenant", partitionId.group());
          MDC.put("actor-name", name + "-" + partitionId.id());
          MDC.put("actor-scheduler", "Broker-" + localMemberId);
          MDC.put(RAFT_ROLE_KEY, Role.INACTIVE.name());
        });
    return context;
  }

  private void onNewPersistedSnapshot(final PersistedSnapshot persistedSnapshot) {
    // 日志压缩后检查 busimeta 缺口，必要时经钩子向 leader 拉取；
    // 切 raft 线程与提交重放串行，避免与 applyCommitted 交错
    threadContext.execute(this::syncBusinessMetaIfNeeded);
    threadContext.execute(this::updateCurrentSnapshot);
  }

  private void onUncaughtException(final Throwable error) {
    LOGGER.error("An uncaught exception occurred, transition to inactive role", error);
    try {
      // to prevent further operations submitted to the threadcontext to execute
      if (ThreadContext.currentContext() == threadContext) {
        transition(Role.INACTIVE);
      } else {
        // raft-log 等其他单线程上下文的异常：转换必须回到 raft 线程执行，
        // 否则 checkThread 抛错后在错误线程上走阻塞 close()
        threadContext.execute(() -> transition(Role.INACTIVE));
      }
    } catch (final Exception e) {
      LOGGER.error("An error occurred when transitioning to inactive, closing the raft context", e);
      close();
    }

    notifyFailureListeners(error);
  }

  private void notifyFailureListeners(final Throwable error) {
    try {
      if (error instanceof UnrecoverableException) {
        health = HealthReport.dead(this).withIssue(error, Instant.now());
        failureListeners.forEach((l) -> l.onUnrecoverableFailure(health));
      } else {
        health = HealthReport.unhealthy(this).withIssue(error, Instant.now());
        failureListeners.forEach((l) -> l.onFailure(health));
      }
    } catch (final Exception e) {
      LOGGER.error("Could not notify failure listeners", e);
    }
  }

  /** Registers server handlers on the configured protocol. */
  private void registerHandlers(final RaftServerProtocol protocol) {
    protocol.registerConfigureHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onConfigure(request), ConfigureResponse::builder));
    protocol.registerInstallHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onInstall(request), InstallResponse::builder));
    protocol.registerReconfigureHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onReconfigure(request), ReconfigureResponse::builder));
    protocol.registerForceConfigureHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onForceConfigure(request), ForceConfigureResponse::builder));
    protocol.registerJoinHandler(
        request ->
            handleRequestOnContext(request, () -> role.onJoin(request), JoinResponse::builder));
    protocol.registerLeaveHandler(
        request ->
            handleRequestOnContext(request, () -> role.onLeave(request), LeaveResponse::builder));
    protocol.registerTransferHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onTransfer(request), TransferResponse::builder));
    protocol.registerTimeoutNowHandler(
        request ->
            handleRequestOnContext(
                request, () -> role.onTimeoutNow(request), TimeoutNowResponse::builder));
    protocol.registerLeadershipTransferInitiateHandler(
        request ->
            handleRequestOnContext(
                request,
                () -> role.onLeadershipTransferInitiate(request),
                LeadershipTransferInitiateResponse::builder));
    protocol.registerAppendV1Handler(
        request ->
            handleRequestOnContext(
                request,
                () -> role.onAppend(ProtocolVersionHandler.transform(request)),
                AppendResponse::builder));
    protocol.registerAppendV2Handler(
        request ->
            handleRequestOnContext(
                request,
                () -> role.onAppend(ProtocolVersionHandler.transform(request)),
                AppendResponse::builder));
    protocol.registerPollHandler(
        request ->
            handleRequestOnContext(request, () -> role.onPoll(request), PollResponse::builder));
    protocol.registerVoteHandler(
        request ->
            handleRequestOnContext(request, () -> role.onVote(request), VoteResponse::builder));
  }

  private <T extends Builder<T, R>, R extends RaftResponse>
      CompletableFuture<R> handleRequestOnContext(
          final RaftRequest request,
          final Supplier<CompletableFuture<R>> function,
          final Supplier<RaftResponse.Builder<T, R>> responseBuilder) {

    final CompletableFuture<R> future = new CompletableFuture<>();
    threadContext.execute(
        () ->
            role.shouldAcceptRequest(request)
                .ifRightOrLeft(
                    ignore ->
                        function
                            .get()
                            .whenComplete(
                                (response, error) -> {
                                  if (error == null) {
                                    future.complete(response);
                                  } else {
                                    future.completeExceptionally(error);
                                  }
                                }),
                    error -> {
                      final R response =
                          responseBuilder.get().withStatus(Status.ERROR).withError(error).build();
                      LOGGER.trace("Sending {}", response);
                      future.complete(response);
                    }));

    return future;
  }

  public int getMaxAppendBatchSize() {
    return partitionConfig.getMaxAppendBatchSize();
  }

  public int getMaxAppendsPerFollower() {
    return partitionConfig.getMaxAppendsPerFollower();
  }

  /**
   * Adds a role change listener. If there isn't currently a transition ongoing the listener is
   * called immediately after adding the listener.
   *
   * @param listener The role change listener.
   */
  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    threadContext.execute(
        () -> {
          roleChangeListeners.add(listener);

          // When a transition is currently ongoing, then the given
          // listener will be called when the transition completes.
          if (!ongoingTransition) {
            // Otherwise, the listener will called directly for the last
            // completed transition.
            listener.onNewRole(getRole(), getTerm());
          }
        });
  }

  /**
   * Removes a role change listener.
   *
   * @param listener The role change listener.
   */
  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    roleChangeListeners.remove(listener);
  }

  /**
   * Adds a state change listener.
   *
   * @param listener The state change listener.
   */
  public void addStateChangeListener(final Consumer<State> listener) {
    listener.accept(state);
    stateChangeListeners.add(listener);
  }

  /**
   * Removes a state change listener.
   *
   * @param listener The state change listener.
   */
  public void removeStateChangeListener(final Consumer<State> listener) {
    stateChangeListeners.remove(listener);
  }

  /**
   * Adds a new commit listener, which will be notified whenever the commit position changes. Note
   * that it will be called on the Raft thread, and as such should not perform any heavy
   * computation.
   *
   * @param commitListener the listener to add
   */
  public void addCommitListener(final RaftCommitListener commitListener) {
    commitListeners.add(commitListener);
  }

  /**
   * Removes registered commit listener
   *
   * @param commitListener the listener to remove
   */
  public void removeCommitListener(final RaftCommitListener commitListener) {
    commitListeners.remove(commitListener);
  }

  /**
   * Adds a new committed entry listener, which will be notified when the Leader commits a new
   * entry. If RAFT runs currently in a Follower role this listeners are not called.
   *
   * <p>Note that it will be called on the Raft thread, and as such should not perform any heavy
   * computation.
   *
   * @param raftApplicationEntryCommittedPositionListener the listener to add
   */
  public void addCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener
          raftApplicationEntryCommittedPositionListener) {
    committedEntryListeners.add(raftApplicationEntryCommittedPositionListener);
  }

  /**
   * Removes registered committedEntryListener
   *
   * @param raftApplicationEntryCommittedPositionListener the listener to remove
   */
  public void removeCommittedEntryListener(
      final RaftApplicationEntryCommittedPositionListener
          raftApplicationEntryCommittedPositionListener) {
    committedEntryListeners.remove(raftApplicationEntryCommittedPositionListener);
  }

  /**
   * Notifies all listeners of the latest entry.
   *
   * @param lastCommitIndex index of the most recently committed entry
   */
  public void notifyCommitListeners(final long lastCommitIndex) {
    // 逐监听器隔离: 单个监听器抛错不应沿提交路径扩散成未捕获异常(会触发整分区 INACTIVE)
    for (final RaftCommitListener listener : commitListeners) {
      try {
        listener.onCommit(lastCommitIndex);
      } catch (final Exception e) {
        LOGGER.error(
            "Commit listener {} failed on commit index {}",
            listener.getClass().getName(),
            lastCommitIndex,
            e);
      }
    }
  }

  /** 提交推进时增量应用 busimeta 条目（raft 线程回调，IO 仅为小文件投影重写），并按需触发 leader 同步。 */
  private void applyBusinessMetaCommitted(final long commitIndex) {
    businessMetaManager.applyCommitted(raftLog, commitIndex);
    syncBusinessMetaIfNeeded();
  }

  /** 业务元数据管理器（含内存状态读取）。 */
  public BusinessMetaManager getBusinessMetaManager() {
    return businessMetaManager;
  }

  /**
   * 业务元数据追加入口（raft 内部处理，与配置变更同级，业务侧不直接触达日志追加）： 任意线程可调，切到 raft 线程后由 leader 角色追加 BusinessMetaEntry
   * 并等待多数派提交， future 以提交条目 index 完成；本机非 leader（含易主切换瞬间）以 {@link RaftException.NoLeader} 异常完成，
   * 由调用方决定转发或拒绝。
   */
  public CompletableFuture<Long> appendBusinessMeta(final Map<String, String> entries) {
    final CompletableFuture<Long> future = new CompletableFuture<>();
    threadContext.execute(
        () -> {
          if (role instanceof final LeaderRole leaderRole) {
            leaderRole
                .appendBusinessMetaEntry(new BusinessMetaEntry(entries))
                .whenComplete(
                    (index, error) -> {
                      if (error != null) {
                        future.completeExceptionally(error);
                      } else {
                        future.complete(index);
                      }
                    });
          } else {
            future.completeExceptionally(
                new RaftException.NoLeader("Local role is not the leader"));
          }
        });
    return future;
  }

  /** 非 leader 且日志压缩形成缺口（appliedIndex+1 &lt; firstIndex）时，经钩子向 leader 拉取全量状态（leader 自身投影启动时已恢复）。 */
  private void syncBusinessMetaIfNeeded() {
    if (isLeader()) {
      return;
    }
    if (businessMetaManager.needsLeaderSync(raftLog) && businessMetaSyncHook != null) {
      businessMetaSyncHook.run();
    }
  }

  /** 注册同步拉取钩子（RaftPartitionServer 装配后调用）。 */
  public void setBusinessMetaSyncHook(final Runnable hook) {
    businessMetaSyncHook = hook;
  }

  /**
   * busimeta 状态实际推进后通知变更监听器（仅业务视图，不改真实 raft 角色，leader/follower 一视同仁）： onStarted/onCompleted 在单次
   * raft 线程回调内成对触发（紧配对，之间无其他事件插入）， 携带回调时刻的真实角色；晚注册监听器经 {@link #addBusinessMetaListener} 注册时以当前状态补发。
   */
  private void notifyBusinessMetaChanged() {
    final Role currentRole = role.role();
    final long currentTerm = term;
    final Map<String, String> entries = businessMetaManager.current().entries();
    businessMetaListeners.forEach(
        listener -> {
          listener.onStarted(partitionId, entries, currentTerm, currentRole);
          listener.onCompleted(partitionId, entries, currentTerm, currentRole);
        });
  }

  /** 注册业务元数据变更监听器（Raft 线程执行；注册时以当前状态补发一次成对回调，重启后无需等待新状态推进）。 */
  public void addBusinessMetaListener(final RaftBusinessMetaListener listener) {
    threadContext.execute(
        () -> {
          final Map<String, String> entries = businessMetaManager.current().entries();
          listener.onStarted(partitionId, entries, term, role.role());
          listener.onCompleted(partitionId, entries, term, role.role());
          businessMetaListeners.add(listener);
        });
  }

  /** 注销业务元数据变更监听器。 */
  public void removeBusinessMetaListener(final RaftBusinessMetaListener listener) {
    threadContext.execute(() -> businessMetaListeners.remove(listener));
  }

  /**
   * Notifies all listeners of the latest entry.
   *
   * @param committedEntry the most recently committed entry
   */
  public void notifyApplicationEntryCommittedPositionListeners(final long committedEntry) {
    committedEntryListeners.forEach(listener -> listener.onCommit(committedEntry));
  }

  /**
   * Sets the commit index.
   *
   * @param commitIndex The commit index.
   * @return the previous commit index
   */
  public long setCommitIndex(long commitIndex) {
    checkArgument(commitIndex >= 0, "commitIndex must be positive");

    // Do not set the commitIndex to be greater than the events we persisted.
    // In case of a heartbeat, the leader sends its commitIndex which may be greater than what's
    // persisted in case we failed to persist a previous AppendEntries
    commitIndex = Math.min(commitIndex, raftLog.getLastIndex());

    final long previousCommitIndex = this.commitIndex;
    if (commitIndex > previousCommitIndex) {
      if (isLeader()) {
        // 保序 group commit：目标先累积，由合并任务统一 fsync 后推进。
        // commitIndex 推进 / storeCommitIndex / client future 完成全部晚于本地 fsync。
        pendingCommitIndex = Math.max(pendingCommitIndex, commitIndex);
        if (!commitFlushPending) {
          commitFlushPending = true;
          // 排到事件队列尾：先处理完已排队事件，它们的提交目标一并合并进同一次 fsync
          threadContext.execute(this::commitFlushAdvance);
        }
        return previousCommitIndex;
      }
      applyCommitAdvance(commitIndex, previousCommitIndex);
    }
    if (awaitingReadyCommitListener != null) {
      awaitingReadyCommitListener.onCommit(commitIndex);
    }
    return previousCommitIndex;
  }

  /**
   * 推进提交水位并落盘：日志提交位、内存 commitIndex、配置提交、meta 持久化、监听器通知。
   *
   * <p>leader 路径由 {@link #commitFlushAdvance()} 在本地 fsync 完成后调用；follower 路径保持与原实现一致的同步推进。
   *
   * @param commitIndex 本次推进到的提交索引（已按日志末端收窄）
   * @param previousCommitIndex 推进前的提交索引
   */
  private void applyCommitAdvance(final long commitIndex, final long previousCommitIndex) {
    raftLog.setCommitIndex(commitIndex);
    this.commitIndex = commitIndex;
    final var clusterConfig = cluster.getConfiguration();
    if (clusterConfig != null) {
      final long configurationIndex = clusterConfig.index();
      if (configurationIndex > previousCommitIndex && configurationIndex <= commitIndex) {
        cluster.commitCurrentConfiguration();
      }
    }
    // Persist the commit index only after the configuration it covers is persisted. This keeps
    // the invariant that a committed configuration entry at or below the stored commit index is
    // always recoverable from the meta store, so startup only needs to search the uncommitted
    // part of the log for configuration entries. The reverse order would allow a crash to leave
    // a stored commit index covering a configuration that is in neither the meta store nor,
    // after restart, in memory.
    meta.storeCommitIndex(commitIndex);
    replicationMetrics.setCommitIndex(commitIndex);
    notifyCommitListeners(commitIndex);
    if (awaitingReadyCommitListener != null) {
      awaitingReadyCommitListener.onCommit(commitIndex);
    }
  }

  /**
   * 合并刷盘任务：一次 fsync 覆盖窗口内全部已 append 记录，之后才推进 commitIndex（保序 group commit）。提交路径强制直刷：即使配置了
   * DelayedFlusher，也必须 fsync 完成后才视为已提交。
   */
  private void commitFlushAdvance() {
    commitFlushPending = false;
    final long previousCommitIndex = commitIndex;
    final long target = pendingCommitIndex;
    pendingCommitIndex = 0;
    if (target <= previousCommitIndex) {
      return;
    }
    if (!isLeader()) {
      // 刷盘排队期间已退位：丢弃 pending，由新 leader 心跳重建提交水位，防止旧 leader 无 quorum 授权推进
      LOGGER.debug("Discarded pending commit index {} after stepping down", target);
      return;
    }
    try {
      raftLog.forceFlush();
    } catch (final FlushException e) {
      LOGGER.warn("Failed to flush commit up to index %s, stepping down".formatted(target), e);
      // 退位触发 LeaderRole.stop() → appender.close()，在途 append future 由此失败
      transition(Role.FOLLOWER);
      return;
    }
    applyCommitAdvance(Math.min(target, raftLog.getLastIndex()), previousCommitIndex);
    commitBatchSize.record(Math.min(target, raftLog.getLastIndex()) - previousCommitIndex);
  }

  /**
   * Adds a new snapshot replication listener, which will be notified before and after a new
   * snapshot is received from a leader. Note that it will be called on the Raft thread, and hence
   * should not perform any heavy computation.
   *
   * @param snapshotReplicationListener the listener to add
   */
  public void addSnapshotReplicationListener(
      final SnapshotReplicationListener snapshotReplicationListener) {
    threadContext.execute(
        () -> {
          snapshotReplicationListeners.add(snapshotReplicationListener);
          // Notify listener immediately if it registered during an ongoing replication.
          // This is to prevent missing necessary state transitions.
          if (role.role() == Role.FOLLOWER) {
            switch (missedSnapshotReplicationEvents) {
              case STARTED -> snapshotReplicationListener.onSnapshotReplicationStarted();
              case COMPLETED -> {
                snapshotReplicationListener.onSnapshotReplicationStarted();
                snapshotReplicationListener.onSnapshotReplicationCompleted(term);
              }
              default -> {}
            }
          }
        });
  }

  /**
   * Removes registered snapshot replication listener
   *
   * @param snapshotReplicationListener the listener to remove
   */
  public void removeSnapshotReplicationListener(
      final SnapshotReplicationListener snapshotReplicationListener) {
    threadContext.execute(() -> snapshotReplicationListeners.remove(snapshotReplicationListener));
  }

  /**
   * 注册业务三态状态监听器（把角色变更与快照复制事件聚合为 LEADER/FOLLOWER/INACTIVE 视图）， 注册后在 Raft 线程上立即回调一次当前状态。
   *
   * @param roleStateListener 业务监听器
   */
  public void addRoleStateListener(final RaftRoleStateListener roleStateListener) {
    threadContext.execute(
        () -> {
          final RaftRoleStateAdapter adapter = new RaftRoleStateAdapter(roleStateListener);
          roleStateAdapters.put(roleStateListener, adapter);
          roleChangeListeners.add(adapter);
          snapshotReplicationListeners.add(adapter);
          // 先回调当前角色状态，再补发错过的快照复制周期，保证终止状态与真实进度一致
          adapter.onNewRole(role.role(), term);
          replayMissedReplicationEvents(adapter);
        });
  }

  /** 为晚注册的监听器补发错过的快照复制事件，映射与运行期完全一致（开始→INACTIVE，结束→FOLLOWER）。 */
  private void replayMissedReplicationEvents(final RaftRoleStateAdapter adapter) {
    if (role.role() != Role.FOLLOWER) {
      return;
    }
    switch (missedSnapshotReplicationEvents) {
      case STARTED -> adapter.onSnapshotReplicationStarted();
      case COMPLETED -> {
        adapter.onSnapshotReplicationStarted();
        adapter.onSnapshotReplicationCompleted(term);
      }
      default -> {
        // 无错过事件
      }
    }
  }

  /** 注销业务三态状态监听器。 */
  public void removeRoleStateListener(final RaftRoleStateListener roleStateListener) {
    threadContext.execute(
        () -> {
          final RaftRoleStateAdapter adapter = roleStateAdapters.remove(roleStateListener);
          if (adapter != null) {
            roleChangeListeners.remove(adapter);
            snapshotReplicationListeners.remove(adapter);
          }
        });
  }

  /** 把 Raft 角色变更与快照复制事件聚合为业务三态视图的适配器（包可见供测试）。 */
  static final class RaftRoleStateAdapter
      implements RaftRoleChangeListener, SnapshotReplicationListener {

    private final RaftRoleStateListener listener;
    private volatile long currentTerm;

    RaftRoleStateAdapter(final RaftRoleStateListener listener) {
      this.listener = listener;
    }

    @Override
    public void onNewRole(final Role newRole, final long term) {
      currentTerm = term;
      dispatch(newRole, term);
    }

    @Override
    public void onSnapshotReplicationStarted() {
      // 快照复制进行中：日志将被重置、消费者需全部关闭，业务视角确定为不可用（INACTIVE）
      listener.onInactive(currentTerm);
    }

    @Override
    public void onSnapshotReplicationCompleted(final long term) {
      currentTerm = term;
      // 快照复制结束：确定性地恢复为 FOLLOWER，不依赖当时的具体角色
      listener.onFollower(term);
    }

    private void dispatch(final Role role, final long term) {
      switch (role) {
        case LEADER -> listener.onLeader(term);
        case FOLLOWER -> listener.onFollower(term);
        default -> listener.onInactive(term);
      }
    }
  }

  public void notifySnapshotReplicationStarted() {
    threadContext.execute(this::snapshotReplicationStarted);
  }

  public void notifySnapshotReplicationCompleted() {
    threadContext.execute(this::snapshotReplicationCompleted);
  }

  /** 复制开始事件分发（须在 raft 线程）：错过的注册者补发标记 + 通知全部监听器。 */
  private void snapshotReplicationStarted() {
    missedSnapshotReplicationEvents = MissedSnapshotReplicationEvents.STARTED;
    snapshotReplicationListeners.forEach(
        SnapshotReplicationListener::onSnapshotReplicationStarted);
  }

  /** 复制完成事件分发（须在 raft 线程）：通知全部监听器 + 错过的注册者补发标记。 */
  private void snapshotReplicationCompleted() {
    snapshotReplicationListeners.forEach(l -> l.onSnapshotReplicationCompleted(term));
    missedSnapshotReplicationEvents = MissedSnapshotReplicationEvents.COMPLETED;
  }

  /**
   * 把存储内已落地的最新镜像安装为本节点状态（两阶段通知 + 日志对齐），在 raft 线程串行执行—— 阶段一通知复制开始（业务关闭日志消费者，三态视图
   * INACTIVE），随后把日志重置到 镜像 index+1（与 follower install 快照一致），对齐当前镜像引用，阶段二通知复制完成（业务可从镜像恢复）。
   *
   * <p>两类调用方：跨分区引导新分区（镜像已落地、raft 尚未 bootstrap，安装后单节点 bootstrap 当选 leader， 完成 onInactive
   * → onLeader 闭环）；follower 安装 leader 合并后的镜像（拉取落地后对齐本地状态）。
   *
   * @return 安装完成 future；无可用镜像时异常完成
   */
  public CompletableFuture<Void> installSnapshot() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    threadContext.execute(
        () -> {
          try {
            final long snapshotIndex = getCurrentSnapshotIndex();
            if (snapshotIndex <= 0) {
              future.completeExceptionally(
                  new SnapshotException(
                      "No snapshot to install for partition "
                          + partitionId.id()
                          + "; expected one landed in the snapshot store"));
              return;
            }
            LOGGER.info(
                "Installing snapshot at index {} for partition {}", snapshotIndex, partitionId.id());
            snapshotReplicationStarted();
            // 日志重置到镜像 index+1：新分区从此起点开始追加自己的条目
            raftLog.reset(snapshotIndex + 1);
            updateCurrentSnapshot();
            snapshotReplicationCompleted();
            future.complete(null);
          } catch (final Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  /**
   * Ensures everything written to the log until this point, is flushed to disk. If default raft
   * flush is enabled, then this will not flush because the logs are flushed when necessary to
   * achieve expected consistency guarantees.
   *
   * @return a future to be completed once the log is flushed to disk
   */
  public CompletableFuture<Void> flushLog() {
    // If flush operations are synchronous on the Raft thread, then the log is guaranteed to be
    // flushed by before committing. Hence, there is no need to flush them again here. This is an
    // optimization to ensure we are not unnecessarily blocking raft thread to do an i/o.
    if (raftLog.flushesDirectly()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(CheckedRunnable.toUnchecked(raftLog::flush), threadContext);
  }

  /**
   * Adds a leader election listener.
   *
   * @param listener The leader election listener.
   */
  public void addLeaderElectionListener(final Consumer<RaftMember> listener) {
    electionListeners.add(listener);
  }

  /**
   * Removes a leader election listener.
   *
   * @param listener The leader election listener.
   */
  public void removeLeaderElectionListener(final Consumer<RaftMember> listener) {
    electionListeners.remove(listener);
  }

  /**
   * Returns the cluster state.
   *
   * @return The cluster state.
   */
  public RaftClusterContext getCluster() {
    return cluster;
  }

  /**
   * Returns the state leader.
   *
   * @return The state leader.
   */
  public DefaultRaftMember getLeader() {
    // Store in a local variable to prevent race conditions and/or multiple volatile lookups.
    final MemberId leader = this.leader;
    return leader != null ? cluster.getMember(leader) : null;
  }

  /** Transition handler. */
  public void transition(final Role role) {
    checkThread();
    checkNotNull(role);

    if (this.role.role() == role) {
      return;
    }

    LOGGER.info(
        "Transitioning to {}: term={}, lastFlushedIdx={}, commitIdx={}",
        role,
        term,
        raftLog.getLastIndex(),
        raftLog.getCommitIndex());

    startTransition();

    // Close the old state.
    try {
      this.role.stop().get();
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException("failed to close Raft state", e);
    }

    MDC.put(RAFT_ROLE_KEY, role.name());

    // Force state transitions to occur synchronously in order to prevent race conditions.
    try {
      final RaftRole newRole = createRole(role);
      synchronized (externalAccessLock) {
        // role is accessed by external threads. To ensure thread-safe access, we need to
        // synchronize the udpate.
        this.role = newRole;
      }
      this.role.start().get();
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException("failed to initialize Raft state", e);
    }

    if (!this.role.role().active() && role.active()) {
      health = HealthReport.healthy(this);
      failureListeners.forEach(l -> l.onRecovered(health));
    }

    if (this.role.role() == role) {
      if (this.role.role() == Role.LEADER) {
        // It is safe to assume that transition to leader is only complete after the initial entries
        // are committed.
        final LeaderRole leaderRole = (LeaderRole) this.role;
        leaderRole.onInitialEntriesCommitted(
            () -> {
              if (this.role == leaderRole) { // ensure no other role change happened in between
                notifyRoleChangeListeners();
                // Transitioning to leader completes
                // once the initial entry gets committed
                completeTransition();
              }
            });
      } else {
        notifyRoleChangeListeners();
        completeTransition();
      }
    }
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  private void startTransition() {
    ongoingTransition = true;
  }

  private void completeTransition() {
    missedSnapshotReplicationEvents = MissedSnapshotReplicationEvents.NONE;
    ongoingTransition = false;
  }

  private void notifyRoleChangeListeners() {
    try {
      roleChangeListeners.forEach(l -> l.onNewRole(role.role(), getTerm()));
    } catch (final Exception exception) {
      LOGGER.error("Unexpected error on calling role change listeners.", exception);
    }
  }

  /** Checks that the current thread is the state context thread. */
  public void checkThread() {
    threadContext.checkThread();
  }

  /** Creates an internal state for the given state type. */
  private RaftRole createRole(final Role role) {
    raftRoleMetrics.setTerm(getTerm());
    return switch (role) {
      case INACTIVE -> {
        raftRoleMetrics.becomingInactive();
        yield new InactiveRole(this);
      }
      case PASSIVE -> new PassiveRole(this);
      case PROMOTABLE -> new PromotableRole(this);
      case FOLLOWER -> {
        raftRoleMetrics.becomingFollower();
        yield new FollowerRole(this, this::createElectionTimer);
      }
      case CANDIDATE -> {
        raftRoleMetrics.becomingCandidate();
        yield new CandidateRole(this);
      }
      case LEADER -> {
        raftRoleMetrics.becomingLeader();
        yield new LeaderRole(this);
      }
      default -> throw new AssertionError();
    };
  }

  private ElectionTimer createElectionTimer(final Runnable triggerElection, final Logger log) {
    if (electionConfig.isPriorityElectionEnabled()) {
      return new PriorityElectionTimer(
          partitionConfig.getElectionTimeout(),
          threadContext,
          triggerElection,
          log,
          electionConfig.getInitialTargetPriority(),
          electionConfig.getNodePriority(),
          partitionConfig.getPriorityDecayGap());
    } else {
      return new RandomizedElectionTimer(
          partitionConfig.getElectionTimeout(), threadContext, random, triggerElection, log);
    }
  }

  /** Transitions the server to the base state for the given member type. */
  public void transition(final Type type) {
    switch (type) {
      case ACTIVE:
        if (!(role instanceof ActiveRole)) {
          transition(Role.FOLLOWER);
        }
        break;
      case PROMOTABLE:
        if (role.role() != Role.PROMOTABLE) {
          transition(Role.PROMOTABLE);
        }
        break;
      case PASSIVE:
        if (role.role() != Role.PASSIVE) {
          transition(Role.PASSIVE);
        }
        break;
      default:
        if (role.role() != Role.INACTIVE) {
          transition(Role.INACTIVE);
        }
        break;
    }
  }

  @Override
  public void close() {
    LOGGER.debug(
        "Closing RaftContext: term={}, commitIdx={}, lastFlushedIdx={}",
        term,
        raftLog.getCommitIndex(),
        raftLog.getLastIndex());
    raftRoleMetrics.becomingInactive();
    started = false;
    // Unregister protocol listeners.
    unregisterHandlers(protocol);

    // Stop the current role before closing the log. A running role may have queued tasks on the
    // thread context that append to the log, for example a leader coming out of joint consensus.
    // Such tasks bail out once the role is no longer running, but if they run against a closed
    // journal, they write into unmapped memory and crash the JVM.
    try {
      role.stop().get();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error("Interrupted while stopping role {} on close", role.role(), e);
    } catch (final Exception e) {
      LOGGER.error("Failed to stop role {} on close", role.role(), e);
    }

    // Close the log.
    try {
      raftLog.close();
    } catch (final Exception e) {
      LOGGER.error("Failed to close raft log", e);
    }

    // Close the metastore.
    try {
      meta.close();
    } catch (final Exception e) {
      LOGGER.error("Failed to close metastore", e);
    }

    // Close the business meta store.
    CloseHelper.quietClose(businessMetaStore);

    LOGGER.debug("Raft context closed");
    // close thread contexts
    threadContext.close();
  }

  /** Unregisters server handlers on the configured protocol. */
  private void unregisterHandlers(final RaftServerProtocol protocol) {
    protocol.unregisterConfigureHandler();
    protocol.unregisterInstallHandler();
    protocol.unregisterReconfigureHandler();
    protocol.unregisterForceConfigureHandler();
    protocol.unregisterJoinHandler();
    protocol.unregisterLeaveHandler();
    protocol.unregisterTransferHandler();
    protocol.unregisterTimeoutNowHandler();
    protocol.unregisterLeadershipTransferInitiateHandler();
    protocol.unregisterAppendHandler();
    protocol.unregisterPollHandler();
    protocol.unregisterVoteHandler();
  }

  @Override
  public String toString() {
    return getClass().getCanonicalName();
  }

  /**
   * Returns the commit index.
   *
   * @return The commit index.
   */
  public long getCommitIndex() {
    return commitIndex;
  }

  /**
   * Returns the election timeout.
   *
   * @return The election timeout.
   */
  public Duration getElectionTimeout() {
    return partitionConfig.getElectionTimeout();
  }

  /**
   * Returns the configuration change timeout.
   *
   * @return The configuration change timeout.
   */
  public Duration getConfigurationChangeTimeout() {
    return partitionConfig.getConfigurationChangeTimeout();
  }

  /**
   * Returns the first commit index.
   *
   * @return The first commit index.
   */
  public long getFirstCommitIndex() {
    return firstCommitIndex;
  }

  /**
   * Sets the first commit index.
   *
   * @param firstCommitIndex The first commit index.
   */
  public void setFirstCommitIndex(final long firstCommitIndex, final long lastFlushedIndex) {
    if (this.firstCommitIndex == 0) {
      if (firstCommitIndex == 0) {
        return;
      }

      // To detect if the current leader has experienced data loss we need to check both its
      // commitIndex and the lastFlushedIndex:
      // the previous leader may have already committed lastFlushedIndex before crashing,
      // but failed to replicate the commit to the new leader.
      // In this situation, the commitIndex of the leader may be smaller than the one in this node,
      // but both nodes agree that events <= lastFlushedIndex have been persisted.
      if (firstCommitIndex < commitIndex && lastFlushedIndex < commitIndex) {
        final var errorMessage =
            String.format(
                """
              Expected to set first commit position to after restart, but firstCommitIndex(%d), lastflushedIndex(%d) < commitIndex(%d). \
              While commitIndex is the last committed index, persisted in the metadata file. \
              This means a majority of nodes has lost committed data: \
              This node will become inactive to avoid overwriting previously committed data, \
              but the leader have formed a quorum and will continue to commit new events, \
              creating an inconsistent timeline of events: \
              THE CLUSTER SHOULD BE STOPPED IMMEDIATELY to further prevent inconsistencies.""",
                firstCommitIndex, lastFlushedIndex, commitIndex);
        throw new IllegalStateException(errorMessage);
      }
      this.firstCommitIndex = firstCommitIndex;
      LOGGER.info(
          "Setting firstCommitIndex to {}. RaftServer is ready only after it has committed events up to this index",
          firstCommitIndex);
    }
  }

  /**
   * Returns the heartbeat interval.
   *
   * @return The heartbeat interval.
   */
  public Duration getHeartbeatInterval() {
    return partitionConfig.getHeartbeatInterval();
  }

  /**
   * Returns the entry validator to be called when an entry is appended.
   *
   * @return The entry validator.
   */
  public EntryValidator getEntryValidator() {
    return entryValidator;
  }

  /**
   * Sets the entry validator to be called when an entry is appended.
   *
   * @param validator The entry validator.
   */
  public void setEntryValidator(final EntryValidator validator) {
    entryValidator = validator;
  }

  /**
   * The broker-supplied barrier the leader uses to freeze/unfreeze the partition's writes during a
   * coordinated leadership transfer. Defaults to {@link LeadershipTransferWriteBarrier#NONE} when
   * no broker is attached. Must be read on the Raft thread.
   */
  public LeadershipTransferWriteBarrier getLeadershipTransferWriteBarrier() {
    return leadershipTransferWriteBarrier;
  }

  /**
   * Registers the barrier the broker attaches on its own thread. Applied on the Raft thread, so the
   * registration only takes effect once the Raft thread picks it up.
   */
  public void setLeadershipTransferWriteBarrier(final LeadershipTransferWriteBarrier barrier) {
    threadContext.execute(() -> leadershipTransferWriteBarrier = barrier);
  }

  /**
   * The broker-supplied check deciding whether the node requesting a transfer is the cluster's
   * rebalancing coordinator. Defaults to {@link LeadershipTransferCoordinatorCheck#NONE} when no
   * broker is attached. Must be read on the Raft thread.
   */
  public LeadershipTransferCoordinatorCheck getLeadershipTransferCoordinatorCheck() {
    return leadershipTransferCoordinatorCheck;
  }

  /**
   * Registers the check the broker attaches on its own thread. Applied on the Raft thread, so the
   * registration only takes effect once the Raft thread picks it up; the returned future completes
   * once it has, so callers that must not proceed until the check is live can await it.
   */
  public CompletableFuture<Void> setLeadershipTransferCoordinatorCheck(
      final LeadershipTransferCoordinatorCheck check) {
    final var applied = new CompletableFuture<Void>();
    threadContext.execute(
        () -> {
          leadershipTransferCoordinatorCheck = check;
          applied.complete(null);
        });
    return applied;
  }

  /**
   * Returns the state last voted for candidate.
   *
   * @return The state last voted for candidate.
   */
  public MemberId getLastVotedFor() {
    return lastVotedFor;
  }

  /**
   * Sets the state last voted for candidate.
   *
   * @param candidate The candidate that was voted for.
   */
  public void setLastVotedFor(final MemberId candidate) {
    // If we've already voted for another candidate in this term then the last voted for candidate
    // cannot be overridden.
    checkState(!(lastVotedFor != null && candidate != null), "Already voted for another candidate");
    lastVotedFor = candidate;
    meta.storeVote(lastVotedFor);

    if (candidate != null) {
      LOGGER.debug("Voted for {}", candidate);
    } else {
      LOGGER.trace("Reset last voted for");
    }
  }

  /**
   * Returns the server log.
   *
   * @return The server log.
   */
  public RaftLog getLog() {
    return raftLog;
  }

  /**
   * Returns the cluster service.
   *
   * @return the cluster service
   */
  public ClusterMembershipService getMembershipService() {
    return membershipService;
  }

  /**
   * Returns the server metadata store.
   *
   * @return The server metadata store.
   */
  public MetaStore getMetaStore() {
    return meta;
  }

  /**
   * Returns the server name.
   *
   * @return The server name.
   */
  public String getName() {
    return name;
  }

  @Override
  public String componentName() {
    return name;
  }

  @Override
  public HealthReport getHealthReport() {
    return health;
  }

  /** Adds a failure listener which will be invoked when an uncaught exception occurs */
  @Override
  public void addFailureListener(final FailureListener listener) {
    failureListeners.add(listener);
  }

  /** Remove a failure listener */
  @Override
  public void removeFailureListener(final FailureListener listener) {
    failureListeners.remove(listener);
  }

  /**
   * Returns the server protocol.
   *
   * @return The server protocol.
   */
  public RaftServerProtocol getProtocol() {
    return protocol;
  }

  /**
   * Returns the current server state.
   *
   * @return The current server state.
   */
  public RaftRole getRaftRole() {
    // This method is accessed by external threads. To ensure thread-safe access, we need to
    // synchronize access to role.
    synchronized (externalAccessLock) {
      return role;
    }
  }

  public RaftRoleMetrics getRaftRoleMetrics() {
    return raftRoleMetrics;
  }

  public RebalanceMetrics getRebalanceMetrics() {
    return rebalanceMetrics;
  }

  /**
   * Returns the current server role.
   *
   * @return The current server role.
   */
  public Role getRole() {
    return getRaftRole().role();
  }

  /**
   * Returns the log compactor.
   *
   * @return The log compactor.
   */
  public LogCompactor getLogCompactor() {
    return logCompactor;
  }

  /**
   * Returns the server snapshot store.
   *
   * @return The server snapshot store.
   */
  public RaftSnapshotStore getPersistedSnapshotStore() {
    return persistedSnapshotStore;
  }

  /**
   * Returns the current server state.
   *
   * @return the current server state
   */
  public State getState() {
    return state;
  }

  /**
   * Returns the server storage.
   *
   * @return The server storage.
   */
  public RaftStorage getStorage() {
    return storage;
  }

  /**
   * Returns the state term.
   *
   * @return The state term.
   */
  public long getTerm() {
    return term;
  }

  /**
   * Sets the state term.
   *
   * @param term The state term.
   */
  public void setTerm(final long term) {
    if (term > this.term) {
      this.term = term;
      leader = null;
      lastVotedFor = null;
      meta.storeTerm(this.term);
      meta.storeVote(lastVotedFor);
      LOGGER.debug("Set term {}", term);
    }
  }

  /**
   * Returns the execution context.
   *
   * @return The execution context.
   */
  public ThreadContext getThreadContext() {
    return threadContext;
  }

  /**
   * Returns a boolean indicating whether this server is the current leader.
   *
   * @return Indicates whether this server is the leader.
   */
  public boolean isLeader() {
    final MemberId leader = this.leader;
    return leader != null && leader.equals(cluster.getLocalMember().memberId());
  }

  /**
   * Sets the state leader.
   *
   * @param leader The state leader.
   */
  public void setLeader(final MemberId leader) {
    if (!Objects.equals(this.leader, leader)) {
      if (leader == null) {
        this.leader = null;
      } else {
        // If a valid leader ID was specified, it must be a member that's currently a member of the
        // ACTIVE members configuration. Note that we don't throw exceptions for unknown members.
        // It's possible that a failure following a configuration change could result in an unknown
        // leader sending AppendRequest to this server. Simply configure the leader if it's known.
        final DefaultRaftMember member = cluster.getMember(leader);
        if (member != null) {
          this.leader = leader;
          LOGGER.info("Found leader {}", member.memberId());
          electionListeners.forEach(l -> l.accept(member));
        }
      }

      LOGGER.trace("Set leader {}", this.leader);
    }
  }

  public PersistedSnapshot getCurrentSnapshot() {
    return currentSnapshot;
  }

  public void updateCurrentSnapshot() {
    checkThread();
    // Get the latest snapshot from snapshot store because it might have been updated already before
    // this listener is executed
    currentSnapshot = persistedSnapshotStore.getLatestSnapshot().orElse(null);
    LOGGER.trace("Set currentSnapshot to {}", currentSnapshot);
    logCompactor.compactFromSnapshots(persistedSnapshotStore);
  }

  public long getCurrentSnapshotIndex() {
    return currentSnapshot != null ? currentSnapshot.getIndex() : 0L;
  }

  /**
   * @return the current configuration index or -1 if there is no configuration yet.
   */
  public long getCurrentConfigurationIndex() {
    final var configuration = cluster.getConfiguration();
    return configuration != null ? configuration.index() : NO_CONFIGURATION_INDEX;
  }

  public boolean isRunning() {
    return started;
  }

  public RaftReplicationMetrics getReplicationMetrics() {
    return replicationMetrics;
  }

  public Random getRandom() {
    return random;
  }

  public long getLastHeartbeat() {
    return lastHeartbeat;
  }

  public void setLastHeartbeat(final long lastHeartbeat) {
    this.lastHeartbeat = lastHeartbeat;
  }

  public void resetLastHeartbeat() {
    setLastHeartbeat(System.currentTimeMillis());
  }

  public int getMinStepDownFailureCount() {
    return partitionConfig.getMinStepDownFailureCount();
  }

  /**
   * The settings bounding a coordinated leadership transfer as configured on this member. A
   * coordinator may override them per transfer, so a transfer in flight uses whatever it resolved
   * on acceptance rather than reading these again.
   */
  public RebalanceConfiguration getRebalanceConfiguration() {
    return new RebalanceConfiguration(
        partitionConfig.getRebalanceReplicationLagThreshold(),
        partitionConfig.getRebalanceReplicationTimeout(),
        partitionConfig.getRebalanceMaxTransferAttempts());
  }

  public Duration getMaxQuorumResponseTimeout() {
    return partitionConfig.getMaxQuorumResponseTimeout();
  }

  public int getPreferSnapshotReplicationThreshold() {
    return partitionConfig.getPreferSnapshotReplicationThreshold();
  }

  public void setPreferSnapshotReplicationThreshold(final int snapshotReplicationThreshold) {
    partitionConfig.setPreferSnapshotReplicationThreshold(snapshotReplicationThreshold);
  }

  public CompletableFuture<Void> reconfigurePriority(final int newPriority) {
    final CompletableFuture<Void> configureFuture = new CompletableFuture<>();
    threadContext.execute(
        () -> {
          electionConfig.setNodePriority(newPriority);
          if (role instanceof final FollowerRole followerRole
              && followerRole.getElectionTimer()
                  instanceof final PriorityElectionTimer priorityElectionTimer) {
            priorityElectionTimer.setNodePriority(newPriority);
          }
          configureFuture.complete(null);
        });
    return configureFuture;
  }

  /**
   * Transfers leadership to the given member (jraft's {@code transferLeadershipTo} equivalent),
   * reusing the coordinated transfer machinery: the leader pauses writes, catches the desired
   * leader up to the frozen log head and promotes it with TimeoutNow. The future completes once the
   * target has been observed as leader, and fails if another member is elected instead or the
   * transfer is rejected.
   *
   * <pre>{@code
   * Caller                Local Node (leader)              Target Member
   *    |                        |                                |
   *    | transferLeadership(t)  |                                |
   *    |----------------------->|                                |
   *    |                        | register leader-election listener
   *    |                        | onLeadershipTransferInitiate   |
   *    |                        | (admission: single in-flight,  |
   *    |                        |  no config change, reachable)  |
   *    |                        | freeze log head (pause writes) |
   *    |                        |-- AppendRequest (catch-up) --->|
   *    |                        |<-- AppendResponse (match=head)-|
   *    |                        |-- TimeoutNowRequest ---------->|
   *    |                        |                                | start election immediately
   *    |                        |<-- VoteRequest ----------------|
   *    |                        |--- vote for target ------------>|
   *    |                        |          (target becomes leader, listener fires)
   *    |<--- complete OK -------|                                |
   * }</pre>
   *
   * <p>When the local node is not the leader, the initiate request is forwarded to the known leader
   * instead; the completion path (election listener) is identical.
   *
   * @param targetMember the member that should take over leadership
   */
  public CompletableFuture<Void> transferLeadership(final MemberId targetMember) {
    final CompletableFuture<Void> result = new CompletableFuture<>();
    threadContext.execute(() -> startLocalLeadershipTransfer(targetMember, result));
    return result;
  }

  private void startLocalLeadershipTransfer(
      final MemberId targetMember, final CompletableFuture<Void> result) {
    final var localMemberId = getCluster().getLocalMember().memberId();

    final Consumer<RaftMember> electionListener =
        new Consumer<>() {
          @Override
          public void accept(final RaftMember electedLeader) {
            if (targetMember.equals(electedLeader.memberId())) {
              result.complete(null);
            } else {
              result.completeExceptionally(
                  new ProtocolException(
                      "Leadership transfer to %s failed: %s was elected instead"
                          .formatted(targetMember, electedLeader.memberId())));
            }
            removeLeaderElectionListener(this);
          }
        };
    addLeaderElectionListener(electionListener);
    result.whenComplete((ignored, error) -> removeLeaderElectionListener(electionListener));

    final var request =
        LeadershipTransferInitiateRequest.builder()
            .withDesiredLeader(targetMember)
            .withCoordinator(localMemberId)
            .withCoordinatorConfigVersion(
                Optional.ofNullable(getCluster().getConfiguration())
                    .map(Configuration::index)
                    .orElse(0L))
            .build();

    final java.util.function.BiConsumer<LeadershipTransferInitiateResponse, Throwable> onResponse =
        (response, error) -> {
          if (error != null) {
            result.completeExceptionally(error);
          } else if (response.status() != Status.OK) {
            result.completeExceptionally(response.error().createException());
          } else if (!response.accepted()) {
            result.completeExceptionally(
                new ProtocolException(
                    "Leadership transfer to %s was rejected: %s"
                        .formatted(targetMember, response.rejectionReason())));
          }
        };

    if (getRole() == Role.LEADER) {
      role.onLeadershipTransferInitiate(request).whenCompleteAsync(onResponse, threadContext);
      return;
    }

    final var leader = getLeader();
    if (leader == null) {
      result.completeExceptionally(
          new RaftError(RaftError.Type.NO_LEADER, "Cannot transfer leadership without a leader")
              .createException());
      return;
    }
    getProtocol()
        .leadershipTransferInitiate(leader.memberId(), request)
        .whenCompleteAsync(onResponse, threadContext);
  }

  public int getPartitionId() {
    return partitionId.id();
  }

  public void updateState(final State newState) {
    if (state != newState) {
      state = newState;
      stateChangeListeners.forEach(l -> l.accept(state));
    }
  }

  public int getSnapshotChunkSize() {
    return snapshotChunkSize;
  }

  public CompletableFuture<SegmentInfo> getTailSegments(final long index) {
    final var fut = new CompletableFuture<SegmentInfo>();
    threadContext.execute(
        () -> {
          final var segments = raftLog.getTailSegments(index);
          fut.complete(segments);
        });
    return fut;
  }

  /** Raft server state. */
  public enum State {
    ACTIVE,
    READY,
    LEFT,
  }

  /**
   * Keeps track of potentially missed snapshot replication events to properly notify newly
   * registered listeners.
   */
  private enum MissedSnapshotReplicationEvents {
    NONE,
    STARTED,
    COMPLETED
  }

  /** Commit listener is active only until the server is ready */
  final class AwaitingReadyCommitListener implements RaftCommitListener {
    private final Logger throttledLogger = new ThrottledLogger(LOGGER, Duration.ofSeconds(30));

    @Override
    public void onCommit(final long index) {
      // On start up, set the state to READY after the follower has caught up with the leader
      // https://github.com/zeebe-io/zeebe/issues/4877
      if (index >= firstCommitIndex) {
        LOGGER.info("Commit index is {}. RaftServer is ready", index);
        updateState(State.READY);
        awaitingReadyCommitListener = null;
      } else {
        throttledLogger.info(
            "Commit index is {}. RaftServer is ready only after it has committed events up to index {}",
            commitIndex,
            firstCommitIndex);
      }
    }
  }
}
