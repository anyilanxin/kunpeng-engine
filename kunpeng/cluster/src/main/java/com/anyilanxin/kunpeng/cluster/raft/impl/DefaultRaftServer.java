/*
 * Copyright 2015-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.raft.impl;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.RaftThreadContextFactory;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftCluster;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferResult;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.roles.LeaderRole;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import java.util.ArrayList;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.AtomixFuture;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Futures;
import com.anyilanxin.kunpeng.cluster.utils.logging.ContextualLoggerFactory;
import com.anyilanxin.kunpeng.cluster.utils.logging.LoggerContext;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a standalone implementation of the <a href="http://raft.github.io/">Raft consensus
 * algorithm</a>.
 *
 * @see RaftStorage
 */
public class DefaultRaftServer implements RaftServer {

  protected final RaftContext context;
  private final Logger log;
  private final AtomicReference<CompletableFuture<RaftServer>> openFutureRef =
      new AtomicReference<>();
  private volatile boolean started;
  private volatile boolean stopped = false;

  public DefaultRaftServer(final RaftContext context) {
    this.context = checkNotNull(context, "context cannot be null");
    log =
        ContextualLoggerFactory.getLogger(
            getClass(),
            LoggerContext.builder(RaftServer.class).addValue(context.getName()).build());
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("name", name()).toString();
  }

  @Override
  public String name() {
    return context.getName();
  }

  @Override
  public RaftCluster cluster() {
    return context.getCluster();
  }

  @Override
  public void addRoleChangeListener(final RaftRoleChangeListener listener) {
    context.addRoleChangeListener(listener);
  }

  @Override
  public void removeRoleChangeListener(final RaftRoleChangeListener listener) {
    context.removeRoleChangeListener(listener);
  }

  @Override
  public void addFailureListener(final FailureListener listener) {
    context.addFailureListener(listener);
  }

  @Override
  public void removeFailureListener(final FailureListener listener) {
    context.removeFailureListener(listener);
  }

  @Override
  public CompletableFuture<RaftServer> bootstrap(final Collection<MemberId> cluster) {
    return start(() -> cluster().bootstrap(cluster));
  }

  @Override
  public CompletableFuture<RaftServer> join(final Collection<MemberId> cluster) {
    return start(() -> cluster().join(cluster));
  }

  /**
   * 离开集群：向 leader 宣告本节点离开（best-effort）→ 集群移除后执行完整 shutdown
   * （停定时器 → INACTIVE → 关闭日志/存储/快照/网络）→ 上层 RaftPartition 随后调 close() 和 delete()。
   */
  @Override
  public CompletableFuture<RaftServer> leave() {
    final CompletableFuture<RaftServer> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              final var leader = context.getLeader();
              if (leader != null
                  && !leader.equals(context.getCluster().getLocalMember().memberId())) {
                // 有 leader：发送降级请求让集群将本节点从配置中移除
                final var demoteReq =
                    ReconfigureRequest.builder()
                        .withIndex(context.getCluster().getConfiguration().index())
                        .withTerm(context.getTerm())
                        .withMember(context.getCluster().getLocalMember())
                        .build();
                context
                    .getProtocol()
                    .reconfigure(leader.memberId(), demoteReq)
                    .whenComplete(
                        (resp, err) ->
                            // 无论 leader 是否应答（best-effort），本节点均执行完整 shutdown
                            performShutdown(future));
              } else {
                // 自身为 leader 或无 leader：直接 shutdown
                performShutdown(future);
              }
            });
    return future;
  }

  /** 执行完整 shutdown 链：转 INACTIVE → 关闭 context 全部资源 */
  private void performShutdown(final CompletableFuture<RaftServer> future) {
    shutdown().whenComplete(
        (result, error) -> {
          if (error == null) {
            future.complete(this);
          } else {
            future.completeExceptionally(error);
          }
        });
  }

  @Override
  public CompletableFuture<RaftServer> promote() {
    return context.anoint().thenApply(v -> this);
  }

  @Override
  public CompletableFuture<RaftServer> forceConfigure(
      final Map<MemberId, RaftMember.Type> membersToRetain) {
    final CompletableFuture<RaftServer> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              try {
                // 紧急操作：直接将新配置写入 MetaStore 并应用到集群上下文（绕过共识）
                final var oldConfig = context.getCluster().getConfiguration();
                final var newMembers = new ArrayList< RaftMember >();
                for (final var entry : membersToRetain.entrySet()) {
                  final var existing = context.getCluster().getMember(entry.getKey());
                  if (existing != null) {
                    // 类型变更经 promote/demote 流程或直接配置写入; 此处保留原类型
                    newMembers.add(existing);
                  }
                }
                final var newConfig = new Configuration(
                    oldConfig.index() + 1,
                    oldConfig.term(),
                    System.currentTimeMillis(),
                    newMembers);
                context.getMetaStore().storeConfiguration(newConfig);
                context.getCluster().configure(newConfig);
                future.complete(this);
              } catch (final Exception e) {
                future.completeExceptionally(e);
              }
            });
    return future;
  }

  @Override
  public CompletableFuture<Void> reconfigurePriority(final int newPriority) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              try {
                context.setElectionPriority(newPriority);
                future.complete(null);
              } catch (final Exception e) {
                future.completeExceptionally(e);
              }
            });
    return future;
  }

  @Override
  public CompletableFuture<Void> flushLog() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              try {
                context.getLog().flush();
                future.complete(null);
              } catch (final Exception e) {
                future.completeExceptionally(e);
              }
            });
    return future;
  }

  @Override
  public CompletableFuture<Void> compact() {
    return context.compact();
  }

  /**
   * Shuts down the server without leaving the Raft cluster.
   *
   * @return A completable future to be completed once the server has been shutdown.
   */
  @Override
  public CompletableFuture<Void> shutdown() {
    if (!started && !stopped) {
      return Futures.exceptionalFuture(new IllegalStateException("Server not running"));
    }

    if (stopped) {
      return Futures.completedFuture(null);
    }

    final CompletableFuture<Void> future = new AtomixFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              stopped = true;
              started = false;
              context.transition(Role.INACTIVE);
              context.close();
              future.complete(null);
            });
    return future;
  }

  @Override
  public CompletableFuture<Void> goInactive() {
    final CompletableFuture<Void> future = new AtomixFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              context.transition(Role.INACTIVE);
              future.complete(null);
            });
    return future;
  }

  @Override
  public RaftContext getContext() {
    return context;
  }

  @Override
  public long getTerm() {
    return context.getTerm();
  }

  @Override
  public Role getRole() {
    return context.getRole();
  }

  /**
   * Returns a boolean indicating whether the server is running.
   *
   * @return Indicates whether the server is running.
   */
  @Override
  public boolean isRunning() {
    return started && !stopped && context.isRunning();
  }

  @Override
  public CompletableFuture<Void> stepDown() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              context.transition(Role.FOLLOWER);
              future.complete(null);
            });
    return future;
  }

  @Override
  public CompletableFuture<LeadershipTransferResult> transferLeadershipTo(final MemberId target) {
    final CompletableFuture<LeadershipTransferResult> future = new CompletableFuture<>();
    context
        .getThreadContext()
        .execute(
            () -> {
              if (!context.isLeader()) {
                future.complete(LeadershipTransferResult.LEADER_CHANGED);
                return;
              }
              final var role = context.getRaftRole();
              if (!(role instanceof LeaderRole leaderRole)) {
                future.complete(LeadershipTransferResult.LEADER_CHANGED);
                return;
              }
              final var request =
                  LeadershipTransferInitiateRequest.builder()
                      .withDesiredLeader(target)
                      .withCoordinator(context.getCluster().getLocalMember().memberId())
                      .withCoordinatorConfigVersion(context.getCluster().getConfiguration().index())
                      .withCorrelationId(System.nanoTime())
                      .build();
              final var response = leaderRole.getTransferRunner().handleInitiate(request);
              if (response.rejectionReason() != null) {
                future.complete(response.rejectionReason());
              } else {
                // 已接受：转移异步进行，完成即意味着目标节点即将接管
                future.complete(LeadershipTransferResult.TRANSFERRED);
              }
            });
    return future;
  }

  @Override
  public boolean isLeadershipTransferInProgress() {
    final var role = context.getRaftRole();
    return role instanceof LeaderRole leaderRole && leaderRole.getTransferRunner().isInProgress();
  }

  /** Starts the server. */
  private CompletableFuture<RaftServer> start(final Supplier<CompletableFuture<Void>> joiner) {
    if (started) {
      return CompletableFuture.completedFuture(this);
    }

    if (openFutureRef.compareAndSet(null, new AtomixFuture<>())) {
      stopped = false;
      joiner
          .get()
          .whenComplete(
              (result, error) -> {
                if (error == null) {
                  log.info("Server join completed. Waiting for the server to be READY");
                  context.awaitState(
                      RaftContext.State.READY,
                      state -> {
                        started = true;
                        openFutureRef.get().complete(this);
                      });
                } else {
                  openFutureRef.get().completeExceptionally(error);
                }
              });
    }

    return openFutureRef
        .get()
        .whenComplete(
            (result, error) -> {
              if (error == null) {
                log.debug("Server started successfully!");
              } else {
                log.warn("Failed to start server!");
              }
            });
  }

  /** Default Raft server builder. */
  public static class Builder extends RaftServer.Builder {

    public Builder(final MemberId localMemberId) {
      super(localMemberId);
    }

    @Override
    public RaftServer build() {

      // If the server name is null, set it to the member ID.
      if (name == null) {
        name = localMemberId.id();
      }

      // If the storage is not configured, create a new Storage instance with the configured
      // serializer.
      if (storage == null) {
        storage = RaftStorage.builder().build();
      }

      final RaftThreadContextFactory singleThreadFactory =
          threadContextFactory == null
              ? new DefaultRaftSingleThreadContextFactory()
              : threadContextFactory;
      final Supplier<Random> randomSupplier = randomFactory == null ? Random::new : randomFactory;
      final MeterRegistry registry = meterRegistry == null ? new SimpleMeterRegistry() : meterRegistry;

      final RaftContext raft =
          new RaftContext(
              name,
              partitionId,
              localMemberId,
              membershipService,
              protocol,
              storage,
              singleThreadFactory,
              randomSupplier,
              electionConfig,
              partitionConfig,
              registry);
      raft.setEntryValidator(entryValidator);

      return new DefaultRaftServer(raft);
    }
  }
}
