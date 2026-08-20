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
package com.anyilanxin.kunpeng.cluster.raft.cluster.impl;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftException;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftCluster;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember.Type;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/** Manages the persistent state of the Raft cluster from the perspective of a single server. */
public final class RaftClusterContext implements RaftCluster, AutoCloseable {

  private final RaftContext raft;
  private final DefaultRaftMember localMember;
  private final Map<MemberId, RaftMemberContext> membersMap = new ConcurrentHashMap<>();
  private final Set<RaftMember> members = new CopyOnWriteArraySet<>();
  private final List<RaftMemberContext> remoteMembers = new CopyOnWriteArrayList<>();
  private final Map<RaftMember.Type, List<RaftMemberContext>> memberTypes =
      new EnumMap<>(RaftMember.Type.class);
  private final AtomicReference<Configuration> configurationRef = new AtomicReference<>();
  private final AtomicReference<CompletableFuture<Void>> bootstrapFutureRef =
      new AtomicReference<>();

  public RaftClusterContext(final MemberId localMemberId, final RaftContext raft) {
    final Instant time = Instant.now();
    localMember =
        new DefaultRaftMember(localMemberId, RaftMember.Type.PASSIVE, time).setCluster(this);
    this.raft = checkNotNull(raft, "context cannot be null");

    // If a configuration is stored, use the stored configuration, otherwise configure the server
    // with the user provided configuration.
    configurationRef.set(raft.getMetaStore().loadConfiguration());

    // Iterate through members in the new configuration and add remote members.
    final var configuration = configurationRef.get();
    if (configuration != null) {
      final Instant updateTime = Instant.ofEpochMilli(configuration.time());
      for (final RaftMember member : configuration.members()) {
        if (member.equals(localMember)) {
          localMember.setType(member.getType());
          members.add(localMember);
        } else {
          // If the member state doesn't already exist, create it.
          final RaftMemberContext state =
              new RaftMemberContext(
                  new DefaultRaftMember(member.memberId(), member.getType(), updateTime),
                  this,
                  raft.getMaxAppendsPerFollower());
          state.resetState(raft.getLog());
          members.add(state.getMember());
          remoteMembers.add(state);
          membersMap.put(member.memberId(), state);

          // Add the member to a type specific map.
          List<RaftMemberContext> memberType = memberTypes.get(member.getType());
          if (memberType == null) {
            memberType = new CopyOnWriteArrayList<>();
            memberTypes.put(member.getType(), memberType);
          }
          memberType.add(state);
        }
      }
    }
  }

  @Override
  public DefaultRaftMember getMember(final MemberId id) {
    if (localMember.memberId().equals(id)) {
      return localMember;
    }
    return getRemoteMember(id);
  }

  /**
   * 加入已有集群：以 PASSIVE 启动 → 向已知成员逐个发 ReconfigureRequest → leader 接受后
   * 将本节点写入配置并经共识提交 → 本节点经日志复制收到新配置 → 转入对应角色。
   */
  @Override
  public CompletableFuture<Void> join(final Collection<MemberId> cluster) {
    // 以 PASSIVE 身份加入，确保不影响已有 quorum
    localMember.setType(Type.PASSIVE);

    final var future = new CompletableFuture<Void>();
    raft.getThreadContext()
      .execute(
        () -> {
          // 按序尝试向已知成员发起加入请求
          final var iterator = cluster.iterator();
          tryNextMember(iterator, future);
        });
    return future;
  }

  private void tryNextMember(
    final java.util.Iterator<MemberId> iterator, final CompletableFuture<Void> future) {
    if (!iterator.hasNext()) {
      future.completeExceptionally(
        new RaftException.ProtocolException("无法加入集群：所有已知成员均未应答"));
      return;
    }
    final var target = iterator.next();
    if (target.equals(localMember.memberId())) {
      tryNextMember(iterator, future);
      return;
    }
    final var request =
      ReconfigureRequest.builder()
        .withIndex(0)
        .withTerm(raft.getTerm())
        .withMember(localMember)
        .build();
    raft.getProtocol()
      .reconfigure(target, request)
      .whenComplete(
        (response, error) -> {
          if (error == null && response.status() == RaftResponse.Status.OK) {
            // leader 已接受，配置变更经共识提交后本节点将收到新配置
            future.complete(null);
          } else {
            // 该成员不可用或非 leader，尝试下一个
            tryNextMember(iterator, future);
          }
        });
  }

  @Override
  public CompletableFuture<Void> bootstrap(final Collection<MemberId> cluster) {
    final var bootstrapFuture = bootstrapFutureRef.get();
    if (bootstrapFuture != null) {
      return bootstrapFuture;
    }

    ensureConfigurationIsConsistent(cluster);

    bootstrapFutureRef.set(new CompletableFuture<>());
    final var isOnBoostrapCluster = configurationRef.get() == null;
    if (isOnBoostrapCluster) {
      localMember.setType(Type.ACTIVE);
      createInitialConfig(cluster);
    }

    raft.getThreadContext()
        .execute(
            () -> {
              // Transition the server to the appropriate state for the local member type.
              raft.transition(localMember.getType());

              if (isOnBoostrapCluster) {
                // commit configuration and transition
                commit();
              }

              completeBootstrapFuture();
            });

    return bootstrapFutureRef.get().whenComplete((result, error) -> bootstrapFutureRef.set(null));
  }

  @Override
  public RaftMember getLeader() {
    return raft.getLeader();
  }

  @Override
  public RaftMember getLocalMember() {
    return localMember;
  }

  @Override
  public Collection<RaftMember> getMembers() {
    return new ArrayList<>(members);
  }

  @Override
  public long getTerm() {
    return raft.getTerm();
  }

  private void ensureConfigurationIsConsistent(final Collection<MemberId> cluster) {
    final var configuration = configurationRef.get();
    final var hasPersistedConfiguration = configuration != null;
    if (hasPersistedConfiguration) {
      final var newClusterSize = cluster.size();
      final var persistedClusterSize = configuration.members().size();

      if (persistedClusterSize != newClusterSize) {
        throw new IllegalStateException(
            String.format(
                "Expected that persisted cluster size '%d' is equal to given one '%d', but was different. "
                    + "Persisted configuration '%s' is different then given one, new given member id's are: '%s'. Changing the configuration is not supported. "
                    + "Please restart with the same configuration or recreate a new cluster after deleting persisted data.",
                persistedClusterSize,
                newClusterSize,
                configuration,
                Arrays.toString(cluster.toArray())));
      }

      final var persistedMembers = configuration.members();
      for (final MemberId memberId : cluster) {
        final var noMatch =
            persistedMembers.stream()
                .map(RaftMember::memberId)
                .noneMatch(persistedMemberId -> persistedMemberId.equals(memberId));
        if (noMatch) {
          throw new IllegalStateException(
              String.format(
                  "Expected to find given node id '%s' in persisted members '%s', but was not found. "
                      + "Persisted configuration is different then given one. Changing the configuration is not supported. "
                      + "Please restart with the same configuration or recreate a new cluster after deleting persisted data.",
                  memberId, Arrays.toString(persistedMembers.toArray())));
        }
      }
    }
  }

  private void createInitialConfig(final Collection<MemberId> cluster) {
    // Create a set of active members.
    final Set<RaftMember> activeMembers =
        cluster.stream()
            .filter(m -> !m.equals(localMember.memberId()))
            .map(m -> new DefaultRaftMember(m, Type.ACTIVE, localMember.getLastUpdated()))
            .collect(Collectors.toSet());

    // Add the local member to the set of active members.
    activeMembers.add(localMember);

    // Create a new configuration and store it on disk to ensure the cluster can fall back to the
    // configuration.
    configure(new Configuration(0, 0, localMember.getLastUpdated().toEpochMilli(), activeMembers));
  }

  /**
   * Returns a member by ID.
   *
   * @param id The member ID.
   * @return The member.
   */
  public DefaultRaftMember getRemoteMember(final MemberId id) {
    final RaftMemberContext member = membersMap.get(id);
    return member != null ? member.getMember() : null;
  }

  /**
   * Returns a member state by ID.
   *
   * @param id The member ID.
   * @return The member state.
   */
  public RaftMemberContext getMemberState(final MemberId id) {
    return membersMap.get(id);
  }

  /**
   * 是否为当前配置成员
   */
  public boolean isMember(final MemberId id) {
    return membersMap.containsKey(id);
  }

  /** 是否处于联合共识阶段 */
  public boolean inJointConsensus() {
    final var config = configurationRef.get();
    return config != null && config.requiresJointConsensus();
  }

  /**
   * Returns a list of active members.
   *
   * @param comparator A comparator with which to sort the members list.
   * @return The sorted members list.
   */
  public List<RaftMemberContext> getActiveMemberStates(
      final Comparator<RaftMemberContext> comparator) {
    final List<RaftMemberContext> activeMembers = new ArrayList<>(getActiveMemberStates());
    activeMembers.sort(comparator);
    return activeMembers;
  }

  /**
   * Returns a list of active members.
   *
   * @return A list of active members.
   */
  public List<RaftMemberContext> getActiveMemberStates() {
    return getRemoteMemberStates(RaftMember.Type.ACTIVE);
  }

  /**
   * Returns a list of member states for the given type.
   *
   * @param type The member type.
   * @return A list of member states for the given type.
   */
  public List<RaftMemberContext> getRemoteMemberStates(final RaftMember.Type type) {
    final List<RaftMemberContext> memberContexts = memberTypes.get(type);
    return memberContexts != null ? memberContexts : List.of();
  }

  private void completeBootstrapFuture() {
    final var bootstrapFuture = bootstrapFutureRef.get();
    // If the local member is not present in the configuration, fail the future.
    if (!members.contains(localMember)) {
      bootstrapFuture.completeExceptionally(
          new IllegalStateException("not a member of the cluster"));
    } else {
      bootstrapFuture.complete(null);
    }
  }

  /**
   * Resets the cluster state to the persisted state.
   *
   * @return The cluster state.
   */
  public RaftClusterContext reset() {
    configure(raft.getMetaStore().loadConfiguration());
    return this;
  }

  /**
   * Configures the cluster state.
   *
   * @param configuration The cluster configuration.
   * @return The cluster state.
   */
  public RaftClusterContext configure(final Configuration configuration) {
    checkNotNull(configuration, "configuration cannot be null");

    // If the configuration index is less than the currently configured index, ignore it.
    // Configurations can be persisted and applying old configurations can revert newer
    // configurations.
    final var currentConfig = configurationRef.get();
    if (currentConfig != null && configuration.index() <= currentConfig.index()) {
      return this;
    }

    final Instant time = Instant.ofEpochMilli(configuration.time());

    // Iterate through members in the new configuration, add any missing members, and update
    // existing members.
    for (final RaftMember member : configuration.members()) {
      updateMember(member, time);
    }

    // Transition the local member only if the member is being promoted and not demoted.
    // Configuration changes that demote the local member are only applied to the local server
    // upon commitment. This ensures that e.g. a leader that's removing itself from the quorum
    // can commit the configuration change prior to shutting down.
    if (wasPromoted(configuration)) {
      raft.transition(localMember.getType());
    }

    // If the local member was removed from the cluster, remove it from the members list.
    if (!configuration.members().contains(localMember)) {
      members.remove(localMember);
    }

    configurationRef.set(configuration);

    // Store the configuration if it's already committed.
    if (raft.getCommitIndex() >= configuration.index()) {
      raft.getMetaStore().storeConfiguration(configuration);
    }

    return this;
  }

  private boolean wasPromoted(final Configuration configuration) {
    return configuration.members().stream()
        .anyMatch(
            m -> m.equals(localMember) && localMember.getType().ordinal() < m.getType().ordinal());
  }

  private void updateMember(final RaftMember member, final Instant time) {
    if (member.equals(localMember)) {
      localMember.update(member.getType(), time);
      members.add(localMember);
      return;
    }

    // If the member state doesn't already exist, create it.
    RaftMemberContext state = membersMap.get(member.memberId());
    if (state == null) {
      final DefaultRaftMember defaultMember =
          new DefaultRaftMember(member.memberId(), member.getType(), time);
      state = new RaftMemberContext(defaultMember, this, raft.getMaxAppendsPerFollower());
      state.resetState(raft.getLog());
      members.add(state.getMember());
      remoteMembers.add(state);
      membersMap.put(member.memberId(), state);
    }

    // If the member type has changed, update the member type and reset its state.
    if (state.getMember().getType() != member.getType()) {
      state.getMember().update(member.getType(), time);
      state.resetState(raft.getLog());
    }

    // Update the optimized member collections according to the member type.
    for (final List<RaftMemberContext> memberType : memberTypes.values()) {
      memberType.remove(state);
    }

    List<RaftMemberContext> memberType = memberTypes.get(member.getType());
    if (memberType == null) {
      memberType = new CopyOnWriteArrayList<>();
      memberTypes.put(member.getType(), memberType);
    }
    memberType.add(state);
  }

  /**
   * Commit the current configuration to disk.
   *
   * @return The cluster state.
   */
  public RaftClusterContext commit() {
    // Apply the configuration to the local server state.
    raft.transition(localMember.getType());

    // If the local stored configuration is older than the committed configuration, overwrite it.
    final var configuration = configurationRef.get();
    if (raft.getMetaStore().loadConfiguration().index() < configuration.index()) {
      raft.getMetaStore().storeConfiguration(configuration);
    }
    return this;
  }

  @Override
  public void close() {
    for (final RaftMemberContext member : remoteMembers) {
      member.getMember().close();
    }
    localMember.close();
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("server", raft.getName()).toString();
  }

  /**
   * Returns the cluster configuration.
   *
   * @return The cluster configuration.
   */
  public Configuration getConfiguration() {
    return configurationRef.get();
  }

  /**
   * Returns the parent context.
   *
   * @return The parent context.
   */
  public RaftContext getContext() {
    return raft;
  }

  /**
   * Returns the remote quorum count.
   *
   * @return The remote quorum count.
   */
  public int getQuorum() {
    return (int) Math.floor((getActiveMemberStates().size() + 1) / 2.0) + 1;
  }

  /**
   * Returns a list of all member states.
   *
   * @return A list of all member states.
   */
  public List<RaftMemberContext> getRemoteMemberStates() {
    return remoteMembers;
  }
}
