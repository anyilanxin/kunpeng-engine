/*
 * Copyright 2018-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.cluster.protocol;

import static com.anyilanxin.kunpeng.cluster.utils.concurrent.Threads.namedThreads;
import static com.google.common.base.MoreObjects.toStringHelper;

import com.anyilanxin.kunpeng.cluster.cluster.BootstrapService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.Node;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEvent;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryService;
import com.anyilanxin.kunpeng.cluster.cluster.impl.AddressSerializer;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.cluster.utils.event.AbstractListenerManager;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespaces;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SWIM group membership protocol implementation. */
public class SwimMembershipProtocol
    extends AbstractListenerManager<GroupMembershipEvent, GroupMembershipEventListener>
    implements GroupMembershipProtocol {

  public static final Type TYPE = new Type();
  private static final Logger LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.cluster.protocol.swim");
  private static final Logger GOSSIP_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.cluster.protocol.swim.gossip");
  private static final Logger PROBE_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.cluster.protocol.swim.probe");
  private static final Logger SYNC_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.cluster.protocol.swim.sync");
  private static final String MEMBERSHIP_SYNC = "atomix-membership-sync";
  private static final String MEMBERSHIP_GOSSIP = "atomix-membership-gossip";
  private static final String MEMBERSHIP_PROBE = "atomix-membership-probe";
  private static final String MEMBERSHIP_PROBE_REQUEST = "atomix-membership-probe-request";
  private static final String MEMBERSHIP_METADATA_PULL = "atomix-membership-metadata";
  private static final Serializer SERIALIZER =
      Serializer.using(
          new Namespace.Builder()
              .register(Namespaces.BASIC)
              .nextId(Namespaces.BEGIN_USER_CUSTOM_ID)
              .register(MemberId.class)
              .register(AddressSerializer.class, Address.class)
              .register(ImmutableMember.class)
              .register(State.class)
              .register(ImmutablePair.class)
              .name("ClusterMembershipService")
              .build());

  private final SwimMembershipProtocolConfig config;
  private final AtomicBoolean started = new AtomicBoolean();
  private final Map<MemberId, SwimMember> members = Maps.newConcurrentMap();
  private final List<SwimMember> randomMembers = Lists.newCopyOnWriteArrayList();
  private final Map<MemberId, ImmutableMember> updates = new LinkedHashMap<>();
  private final List<SwimMember> syncMembers = new ArrayList<>();

  /** 元数据拉取去重表：成员 id → 触发时的线上版本；在途/已排队期间不重复调度。 */
  private final Map<MemberId, Long> pendingMetadataPulls = Maps.newConcurrentMap();

  private long lastMetadataAntiEntropyNs;
  private final ScheduledExecutorService swimScheduler;
  private final ExecutorService eventExecutor;
  private final AtomicInteger probeCounter = new AtomicInteger();
  private NodeDiscoveryService discoveryService;
  private BootstrapService bootstrapService;
  private SwimMember localMember;
  private volatile Properties localProperties = new Properties();
  private ScheduledFuture<?> gossipFuture;
  private ScheduledFuture<?> probeFuture;
  private ScheduledFuture<?> syncFuture;
  private final BiFunction<Address, byte[], CompletableFuture<byte[]>> probeRequestHandler =
      (address, payload) ->
          handleProbeRequest(SERIALIZER.decode(payload)).thenApply(SERIALIZER::encode);
  private final NodeDiscoveryEventListener discoveryEventListener = this::handleDiscoveryEvent;
  private final SwimMembershipProtocolMetrics swimMembershipProtocolMetrics;
  private final BiFunction<Address, byte[], byte[]> syncHandler =
      (address, payload) -> SERIALIZER.encode(handleSync(SERIALIZER.decode(payload)));
  private final BiConsumer<Address, byte[]> gossipListener =
      (address, payload) -> handleGossipUpdates(address, SERIALIZER.decode(payload));
  private final BiFunction<Address, byte[], byte[]> probeHandler =
      (address, payload) -> SERIALIZER.encode(handleProbe(SERIALIZER.decode(payload), address));
  private final BiFunction<Address, byte[], byte[]> metadataPullHandler =
      (address, payload) -> SERIALIZER.encode(handleMetadataPull(SERIALIZER.decode(payload)));

  SwimMembershipProtocol(final SwimMembershipProtocolConfig config, final MeterRegistry registry) {
    this.config = config;
    swimMembershipProtocolMetrics = new SwimMembershipProtocolMetrics(registry);

    swimScheduler =
        Executors.newSingleThreadScheduledExecutor(
            namedThreads("atomix-cluster-heartbeat-sender", LOGGER));
    eventExecutor =
        Executors.newSingleThreadExecutor(namedThreads("atomix-cluster-events", LOGGER));
  }

  /**
   * Creates a new SWIM membership protocol builder.
   *
   * @return a new SWIM membership protocol builder
   */
  public static SwimMembershipProtocolBuilder builder(final MeterRegistry registry) {
    return new SwimMembershipProtocolBuilder(registry);
  }

  @Override
  public SwimMembershipProtocolConfig config() {
    return config;
  }

  @Override
  public Set<Member> getMembers() {
    return ImmutableSet.copyOf(members.values());
  }

  @Override
  public Member getMember(final MemberId memberId) {
    return members.get(memberId);
  }

  @Override
  public CompletableFuture<Void> join(
      final BootstrapService bootstrap, final NodeDiscoveryService discovery, final Member member) {
    if (started.compareAndSet(false, true)) {
      bootstrapService = bootstrap;
      discoveryService = discovery;
      localMember =
          new SwimMember(
              member.id(),
              member.nodeVersion(),
              member.address(),
              member.zone(),
              member.rack(),
              member.host(),
              member.properties(),
              member.version(),
              System.currentTimeMillis());
      localProperties.putAll(localMember.properties());
      discoveryService.addListener(discoveryEventListener);

      // we need to add our local node to the member list,
      // to share the mapping between node id and address in the cluster
      localMember.setState(State.ALIVE);
      localMember.setMetadataVersion(1);
      localMember.setPulledMetadataVersion(1);
      members.put(localMember.id(), localMember);
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_ADDED, localMember));

      LOGGER.debug("Nodes from discovery service {}", discoveryService.getNodes());

      registerHandlers();

      scheduleGossip();
      scheduleProbe();
      scheduleSync();

      LOGGER.info("Started");
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> leave(final Member member) {
    if (started.compareAndSet(true, false)) {
      discoveryService.removeListener(discoveryEventListener);
      gossipFuture.cancel(false);
      probeFuture.cancel(false);
      syncFuture.cancel(false);
      swimScheduler.shutdownNow();
      eventExecutor.shutdownNow();
      LOGGER.info("{} - Member deactivated: {}", localMember.id(), localMember);
      localMember.setState(State.DEAD);
      members.clear();
      unregisterHandlers();
      LOGGER.info("Stopped");
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected void post(final GroupMembershipEvent event) {
    if (event.type() == GroupMembershipEvent.Type.MEMBER_ADDED) {
      swimMembershipProtocolMetrics.countMemberAdded(members.size());
    } else if (event.type() == GroupMembershipEvent.Type.MEMBER_REMOVED) {
      swimMembershipProtocolMetrics.countMemberRemoved(members.size());
    }
    eventExecutor.execute(() -> super.post(event));
  }

  public boolean isStarted() {
    return started.get();
  }

  /** Checks the local member metadata for changes. */
  private void checkMetadata() {
    final var liveProperties = localMember.properties();
    if (liveProperties.equals(localProperties)) {
      return;
    }

    final var currentProperties = (Properties) liveProperties.clone();
    localProperties = currentProperties;
    LOGGER.debug("{} - Detected local properties change {}", localMember.id(), currentProperties);
    localMember.setIncarnationNumber(localMember.getIncarnationNumber() + 1);
    // 元数据版本 +1: 线上记录携带新版本, 接收方比对缓存版本后经拉取通道获取全量元数据
    localMember.setMetadataVersion(localMember.metadataVersion() + 1);

    // The properties of the local member are mutated in place by other components, e.g. to publish
    // partition roles and health. Listeners are notified asynchronously, so they must observe
    // exactly the properties we diffed against here: if they read the live properties instead, a
    // change applied and reverted before the next check is observed but never published again,
    // leaving those listeners permanently stale.
    final var snapshot = localMember.copy(currentProperties);
    post(new GroupMembershipEvent(GroupMembershipEvent.Type.METADATA_CHANGED, snapshot));
    recordUpdate(localMember);
  }

  /**
   * Updates the state for the given member.
   *
   * @param member the member for which to update the state
   * @return whether the state for the member was updated
   */
  private boolean updateState(final ImmutableMember member) {
    return updateState(member, null);
  }

  /**
   * Updates the state for the given member.
   *
   * @param member the member for which to update the state
   * @param sourceHint 提供该记录的对端地址；owner 直连失败时作为元数据拉取的兜底来源
   * @return whether the state for the member was updated
   */
  private boolean updateState(final ImmutableMember member, final Address sourceHint) {
    // If the member matches the local member, ignore the update.
    if (member.id().equals(localMember.id())) {
      if (member.nodeVersion() > localMember.nodeVersion()) {
        LOGGER.debug(
            "Detected higher version of the local member {} (local: {}, remote: {}). Leaving the cluster.",
            localMember.id(),
            localMember.nodeVersion(),
            member.nodeVersion());
        leave(localMember);
      }
      return false;
    }

    SwimMember swimMember = members.get(member.id());

    // If the local member is not present, add the member in the ALIVE state.
    if (swimMember == null) {
      if (member.state() == State.ALIVE) {
        swimMember = new SwimMember(member);
        members.put(swimMember.id(), swimMember);
        randomMembers.add(swimMember);
        Collections.shuffle(randomMembers);
        LOGGER.info("{} - Member added {}", localMember.id(), swimMember);
        swimMember.setState(State.ALIVE);
        post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_ADDED, swimMember.copy()));
        recordUpdate(swimMember);
        // 全量元数据未随线上记录到达, 向 owner(兜底 hint)惰性拉取
        scheduleMetadataPull(swimMember, sourceHint);
        return true;
      } else {
        LOGGER.info(
            "{} - Ignoring update about not alive member {}. Member is already removed.",
            localMember.id(),
            member);
      }
      return false;
    } else if (member.nodeVersion() < swimMember.nodeVersion()) {
      LOGGER.debug(
          "{} - Detected lower version for member {} (local: {}, remote: {}). Ignoring the update.",
          localMember.id(),
          swimMember.id(),
          swimMember.nodeVersion(),
          member.nodeVersion());
      return false;
    }
    // If the term has been increased, update the member and record a gossip event.
    else if (member.incarnationNumber() > swimMember.getIncarnationNumber()
        // Although this would not happen, we handle the case where the higher node version has a
        // lower incarnation number.
        || member.nodeVersion() > swimMember.nodeVersion()) {
      // If the member's software version or nodeVersion has changed, remove the old member and add
      // the new member.
      if (!Objects.equals(member.version(), swimMember.version())
          || member.nodeVersion() > swimMember.nodeVersion()) {
        members.remove(member.id());
        randomMembers.remove(swimMember);
        post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_REMOVED, swimMember.copy()));
        swimMember = new SwimMember(member);
        swimMember.setState(State.ALIVE);
        members.put(member.id(), swimMember);
        randomMembers.add(swimMember);
        Collections.shuffle(randomMembers);
        LOGGER.info("{} - Evicted member for new version {}", localMember.id(), swimMember);
        post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_ADDED, swimMember.copy()));
        recordUpdate(swimMember);
        scheduleMetadataPull(swimMember, sourceHint);
      } else {
        // Update the term for the local member.
        swimMember.setIncarnationNumber(member.incarnationNumber());

        // If the state has been changed to ALIVE, trigger a REACHABILITY_CHANGED event and then
        // update metadata.
        if (member.state() == State.ALIVE && swimMember.getState() != State.ALIVE) {
          triggerReachabilityChangedEventOnAlive(member, swimMember);
        }
        // If the state has been changed to SUSPECT, update metadata and then trigger a
        // REACHABILITY_CHANGED event.
        else if (member.state() == State.SUSPECT && swimMember.getState() != State.SUSPECT) {
          triggerReachabilityEventOnSuspect(member, swimMember);
        }
        // If the state has been changed to DEAD, trigger a REACHABILITY_CHANGED event if necessary
        // and then remove
        // the member from the members list and trigger a MEMBER_REMOVED event.
        else if (member.state() == State.DEAD && swimMember.getState() != State.DEAD) {
          triggerReachabilityEventOnDeath(swimMember);
        } else {
          mergeMetadataFromRecord(member, swimMember);
        }
        scheduleMetadataPull(swimMember, sourceHint);

        // Always enqueue an update for gossip when the term changes.
        recordUpdate(swimMember);
        return true;
      }
    }
    // If the term remained the same but the state has progressed, update the state and trigger
    // events.
    else if (member.incarnationNumber() == swimMember.getIncarnationNumber()
        && member.state().ordinal() > swimMember.getState().ordinal()) {
      swimMember.setState(member.state());

      // If the updated state is SUSPECT, post a REACHABILITY_CHANGED event and record an update.
      if (member.state() == State.SUSPECT) {
        LOGGER.info("{} - Member unreachable {}", localMember.id(), swimMember);
        post(
            new GroupMembershipEvent(
                GroupMembershipEvent.Type.REACHABILITY_CHANGED, swimMember.copy()));
        if (config.isNotifySuspect()) {
          gossip(swimMember, Lists.newArrayList(wireCopy(swimMember)));
        }
      }
      // If the updated state is DEAD, post a REACHABILITY_CHANGED event if necessary, then post a
      // MEMBER_REMOVED
      // event and record an update.
      else if (member.state() == State.DEAD) {
        tryRemoveMember(swimMember);
      }
      recordUpdate(swimMember);
      return true;
    }
    return false;
  }

  /**
   * 合并线上记录携带的内联属性并推进元数据版本。
   *
   * <p>线上 {@code properties} 仅含白名单内联键（如事件订阅表），立即合并保证广播寻址新鲜； 全量元数据由 {@link #scheduleMetadataPull}
   * 按版本差触发拉取。
   */
  private void mergeMetadataFromRecord(final ImmutableMember member, final SwimMember swimMember) {
    if (member.metadataVersion() > swimMember.metadataVersion()) {
      swimMember.setMetadataVersion(member.metadataVersion());
    }
    if (inlinePropertiesChanged(member.properties(), swimMember.properties())) {
      swimMember.properties().putAll(member.properties());
      LOGGER.debug("{} - Member inline metadata changed {}", localMember.id(), swimMember);
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.METADATA_CHANGED, swimMember.copy()));
    }
  }

  private void triggerReachabilityEventOnDeath(final SwimMember swimMember) {
    if (swimMember.getState() == State.ALIVE) {
      swimMember.setState(State.SUSPECT);
      LOGGER.info("{} - Member unreachable {}", localMember.id(), swimMember);
      post(
          new GroupMembershipEvent(
              GroupMembershipEvent.Type.REACHABILITY_CHANGED, swimMember.copy()));
    }
    swimMember.setState(State.DEAD);
    tryRemoveMember(swimMember);
  }

  private void triggerReachabilityEventOnSuspect(
      final ImmutableMember member, final SwimMember swimMember) {
    mergeMetadataFromRecord(member, swimMember);
    swimMember.setState(State.SUSPECT);
    LOGGER.info("{} - Member unreachable {}", localMember.id(), swimMember);
    post(
        new GroupMembershipEvent(
            GroupMembershipEvent.Type.REACHABILITY_CHANGED, swimMember.copy()));
    if (config.isNotifySuspect()) {
      gossip(swimMember, Lists.newArrayList(wireCopy(swimMember)));
    }
  }

  private void triggerReachabilityChangedEventOnAlive(
      final ImmutableMember member, final SwimMember swimMember) {
    swimMember.setState(State.ALIVE);
    LOGGER.info("{} - Member reachable {}", localMember.id(), swimMember);
    post(
        new GroupMembershipEvent(
            GroupMembershipEvent.Type.REACHABILITY_CHANGED, swimMember.copy()));
    mergeMetadataFromRecord(member, swimMember);
  }

  /**
   * Records an update as an immutable member.
   *
   * @param member the updated member
   */
  private void recordUpdate(final SwimMember member) {
    updates.put(member.id(), wireCopy(member));
    swimMembershipProtocolMetrics.updateMemberIncarnationNumber(
        member.id().id(), member.getIncarnationNumber());
  }

  /** Checks suspect nodes for failures. */
  private void checkFailures() {
    for (final SwimMember member : members.values()) {
      final long suspectedDuration = System.currentTimeMillis() - member.getUpdated();
      if (member.getState() == State.SUSPECT
          && suspectedDuration > config.getFailureTimeout().toMillis()) {
        LOGGER.info(
            "{} - Member {} not reachable for {}",
            localMember.id(),
            member.id(),
            Duration.ofMillis(suspectedDuration));
        member.setState(State.DEAD);

        tryRemoveMember(member);
        recordUpdate(member);
      }
    }
  }

  private void tryRemoveMember(final SwimMember member) {
    final var deadMember = members.remove(member.id());
    if (deadMember != null) {
      randomMembers.remove(member);
      syncMembers.remove(member);
      Collections.shuffle(randomMembers);
      LOGGER.info("{} - Member removed {}", localMember.id(), member.id());
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_REMOVED, member.copy()));
    }
  }

  /**
   * Synchronizes the node state with the given peer.
   *
   * @param member the peer with which to synchronize the node state
   */
  private void sync(final ImmutableMember member) {
    SYNC_LOGGER.debug("{} - Start synchronizing membership with {}", localMember.id(), member);
    bootstrapService
        .getMessagingService()
        .sendAndReceive(
            member.address(),
            MEMBERSHIP_SYNC,
            SERIALIZER.encode(wireCopy(localMember)),
            false,
            config.getProbeTimeout())
        .whenCompleteAsync(
            (response, error) -> {
              // 自调度链: 重调度放 finally, 回调抛错(如坏响应解码失败)不至于永久杀死 sync 循环
              try {
                if (error == null) {
                  final Collection<ImmutableMember> members = SERIALIZER.decode(response);
                  SYNC_LOGGER.debug(
                      "{} - Finished synchronizing membership with {}, received: '{}'",
                      localMember.id(),
                      member,
                      members);
                  final var source = member.address();
                  members.forEach(m -> updateState(m, source));
                } else {
                  SYNC_LOGGER.warn(
                      "{} - Failed to synchronize membership with {}",
                      localMember.id(),
                      member,
                      error);
                }
              } finally {
                scheduleSync();
              }
            },
            swimScheduler);
  }

  private void sync() {
    if (syncMembers.isEmpty()) {
      syncMembers.addAll(members.values());
      syncMembers.remove(localMember);
      Collections.shuffle(syncMembers);
    }

    if (!syncMembers.isEmpty()) {
      final SwimMember member = syncMembers.remove(0);
      if (member != null) {
        sync(member.copy());
      }
    } else {
      scheduleSync();
    }
  }

  /**
   * Handles a synchronize request from a peer.
   *
   * @param member the peer from which to handle the request
   * @return the member list (基础信息 + 内联属性 + 元数据版本)
   */
  private Collection<ImmutableMember> handleSync(final ImmutableMember member) {
    SYNC_LOGGER.trace("{} - Received sync request from {}", localMember.id(), member);
    updateState(member);
    return members.values().stream().map(this::wireCopy).collect(Collectors.toList());
  }

  /** Sends probes to all members or to the next member in round robin fashion. */
  private void probe() {
    // First get a sorted list of discovery service nodes that are not present in the SWIM members.
    // This is necessary to ensure we attempt to probe all nodes that are provided by the discovery
    // provider.
    final List<SwimMember> probeMembers =
        discoveryService.getNodes().stream()
            .map(node -> new SwimMember(MemberId.from(node.id().id()), node.address()))
            .filter(member -> !members.containsKey(member.id()))
            .filter(member -> !member.id().equals(localMember.id()))
            .filter(member -> !member.address().equals(localMember.address()))
            .sorted(Comparator.comparing(Member::id))
            .collect(Collectors.toList());

    // Then add the randomly sorted list of SWIM members.
    probeMembers.addAll(randomMembers);

    // If there are members to probe, select the next member to probe using a counter for round
    // robin probes.
    if (!probeMembers.isEmpty()) {
      PROBE_LOGGER.trace("Possible members to probe '{}'", probeMembers);
      final SwimMember probeMember =
          probeMembers.get(Math.abs(probeCounter.incrementAndGet() % probeMembers.size()));
      probe(wireCopy(probeMember));
    } else {
      scheduleProbe();
    }
  }

  /**
   * Probes the given member.
   *
   * @param member the member to probe
   */
  private void probe(final ImmutableMember member) {
    PROBE_LOGGER.trace("{} - Probing {}", localMember.id(), member);
    bootstrapService
        .getMessagingService()
        .sendAndReceive(
            member.address(),
            MEMBERSHIP_PROBE,
            SERIALIZER.encode(Pair.of(wireCopy(localMember), member)),
            false,
            config.getProbeTimeout())
        .whenCompleteAsync(
            (response, error) -> {
              // 自调度链: 重调度放 finally, 回调抛错(如坏响应解码失败)不至于永久杀死 probe 循环
              try {
                if (error == null) {
                  updateState(SERIALIZER.decode(response), member.address());
                } else {
                  PROBE_LOGGER.trace("{} - Failed to probe {}", localMember.id(), member, error);
                  // Verify that the local member term has not changed and request probes from
                  // peers.
                  final SwimMember swimMember = members.get(member.id());
                  if (swimMember != null
                      && swimMember.getIncarnationNumber() == member.incarnationNumber()) {
                    PROBE_LOGGER.warn(
                        "{} - Failed to probe {}", localMember.id(), member.id(), error);
                    requestProbes(wireCopy(swimMember));
                  }
                }
              } finally {
                scheduleProbe();
              }
            },
            swimScheduler);
  }

  /**
   * Handles a probe from another peer.
   *
   * @param members the probing member and local member info
   * @return the current term
   */
  private ImmutableMember handleProbe(
      final Pair<ImmutableMember, ImmutableMember> members, final Address source) {
    final ImmutableMember remoteMember = members.getLeft();
    final ImmutableMember localMember = members.getRight();

    PROBE_LOGGER.trace(
        "{} - Received probe {} from {}", this.localMember.id(), localMember, remoteMember);

    if (Objects.equals(this.localMember.id(), localMember.id())) {
      // id can be just the host address if the probe uses the configured initial contact points. In
      // that case do not compare nodeVersion as it will be always 0.
      if (localMember.nodeVersion() < this.localMember.nodeVersion()) {
        LOGGER.debug(
            "Detected lower version for member {} (local: {}, remote: {}). Ignoring the probe.",
            this.localMember.id(),
            this.localMember.nodeVersion(),
            localMember.nodeVersion());
        return wireCopy(this.localMember);
      } else if (localMember.nodeVersion() > this.localMember.nodeVersion()) {
        LOGGER.debug(
            "Detected higher version for member {} (local: {}, remote: {}). Leaving the cluster.",
            this.localMember.id(),
            this.localMember.nodeVersion(),
            localMember.nodeVersion());
        leave(this.localMember);
        return wireCopy(this.localMember);
      }
    }

    // If the probe indicates a term greater than the local term, update the local term, increment
    // and respond.
    if (localMember.incarnationNumber() > this.localMember.getIncarnationNumber()) {
      this.localMember.setIncarnationNumber(localMember.incarnationNumber() + 1);
      if (config.isBroadcastDisputes()) {
        broadcast(wireCopy(this.localMember));
      }
    }
    // If the probe indicates this member is suspect, increment the local term and respond.
    else if (localMember.state() == State.SUSPECT) {
      this.localMember.setIncarnationNumber(this.localMember.getIncarnationNumber() + 1);
      if (config.isBroadcastDisputes()) {
        broadcast(wireCopy(this.localMember));
      }
    }

    // Update the state of the probing member.
    updateState(remoteMember, source);
    return wireCopy(this.localMember);
  }

  /** Requests probes from n peers. */
  private void requestProbes(final ImmutableMember suspect) {
    final Collection<SwimMember> members =
        selectRandomMembers(config.getSuspectProbes() - 1, suspect);
    if (!members.isEmpty()) {
      final AtomicInteger counter = new AtomicInteger();
      final AtomicBoolean succeeded = new AtomicBoolean();
      PROBE_LOGGER.debug(
          "{} - Requesting probe of {} from [{}]", localMember.id(), suspect, members);
      for (final SwimMember member : members) {
        requestProbe(member, suspect)
            .whenCompleteAsync(
                (success, error) -> {
                  final int count = counter.incrementAndGet();
                  if (error == null && success) {
                    succeeded.set(true);
                  }
                  // If the count is equal to the number of probe peers and no probe has succeeded,
                  // the node is unreachable.
                  else if (count == members.size() && !succeeded.get()) {
                    failProbes(suspect);
                  }
                },
                swimScheduler);
      }
    } else {
      failProbes(suspect);
    }
  }

  /** Marks the given member suspect after all probes failing. */
  private void failProbes(final ImmutableMember suspect) {
    final SwimMember swimMember = new SwimMember(suspect);
    PROBE_LOGGER.info(
        "{} - Failed all probes of {}. Marking as suspect.", localMember.id(), swimMember);
    swimMember.setState(State.SUSPECT);
    if (updateState(swimMember.copy()) && config.isBroadcastUpdates()) {
      broadcast(wireCopy(swimMember));
    }
  }

  /**
   * Requests a probe of the given suspect from the given member.
   *
   * @param member the member to perform the probe
   * @param suspect the suspect member to probe
   */
  private CompletableFuture<Boolean> requestProbe(
      final SwimMember member, final ImmutableMember suspect) {
    return bootstrapService
        .getMessagingService()
        .sendAndReceive(
            member.address(),
            MEMBERSHIP_PROBE_REQUEST,
            SERIALIZER.encode(suspect),
            false,
            config.getProbeTimeout().multipliedBy(2))
        .<Boolean>thenApply(SERIALIZER::decode)
        .exceptionally(e -> false)
        .thenApply(
            succeeded -> {
              PROBE_LOGGER.debug(
                  "{} - Probe request of {} from {} {}",
                  localMember.id(),
                  suspect,
                  member,
                  succeeded ? "succeeded" : "failed");
              return succeeded;
            });
  }

  /**
   * Selects a set of random members, excluding the local member and a given member.
   *
   * @param count count the number of random members to select
   * @param exclude the member to exclude
   * @return members a set of random members
   */
  private Collection<SwimMember> selectRandomMembers(
      final int count, final ImmutableMember exclude) {
    final List<SwimMember> members =
        this.members.values().stream()
            .filter(
                member ->
                    !member.id().equals(localMember.id()) && !member.id().equals(exclude.id()))
            .collect(Collectors.toList());
    Collections.shuffle(members);
    return members.subList(0, Math.min(members.size(), count));
  }

  /**
   * Handles a probe request.
   *
   * @param member the member to probe
   */
  private CompletableFuture<Boolean> handleProbeRequest(final ImmutableMember member) {
    final CompletableFuture<Boolean> future = new CompletableFuture<>();
    swimScheduler.execute(
        () -> {
          PROBE_LOGGER.trace("{} - Probing {}", localMember.id(), member);
          bootstrapService
              .getMessagingService()
              .sendAndReceive(
                  member.address(),
                  MEMBERSHIP_PROBE,
                  SERIALIZER.encode(Pair.of(wireCopy(localMember), member)),
                  false,
                  config.getProbeTimeout())
              .whenCompleteAsync(
                  (response, error) -> {
                    if (error != null) {
                      PROBE_LOGGER.info("{} - Failed to probe {}", localMember.id(), member.id());
                      future.complete(false);
                    } else {
                      future.complete(true);
                    }
                  },
                  swimScheduler);
        });
    return future;
  }

  /**
   * Broadcasts the given update to all peers.
   *
   * @param update the update to broadcast
   */
  private void broadcast(final ImmutableMember update) {
    for (final SwimMember member : members.values()) {
      if (!localMember.id().equals(member.id())) {
        unicast(member, update);
      }
    }
  }

  /**
   * Unicasts the given update to the given member.
   *
   * @param member the member to which to unicast the update
   * @param update the update to unicast
   */
  private void unicast(final SwimMember member, final ImmutableMember update) {
    bootstrapService
        .getUnicastService()
        .unicast(
            member.address(), MEMBERSHIP_GOSSIP, SERIALIZER.encode(Lists.newArrayList(update)));
  }

  /** Gossips pending updates to the cluster. */
  private void gossip() {
    // 自调度链: 重调度放 finally, 循环体抛错不至于永久杀死 gossip(连带 checkFailures 的失效检测)
    try {
      // Check suspect nodes for failure timeouts.
      checkFailures();

      // Check local metadata for changes.
      checkMetadata();

      // 元数据反熵: 定期重查版本落后的成员并重新拉取, 兜底丢失的变化通知(gossip 尽力而为)
      checkMetadataAntiEntropy();

      // Copy and clear the list of pending updates.
      if (!updates.isEmpty()) {
        final List<ImmutableMember> updates = Lists.newArrayList(this.updates.values());
        this.updates.clear();

        // Gossip the pending updates to peers.
        gossip(updates);
      }
    } finally {
      scheduleGossip();
    }
  }

  /** 只保留白名单内联属性（如事件订阅表）, 其余属性经拉取通道传播, 缩减稳态报文体积。 */
  private Properties inlineProperties(final Properties full) {
    final Properties inline = new Properties();
    for (final String name : config.getInlinePropertyNames()) {
      final String value = full.getProperty(name);
      if (value != null) {
        inline.setProperty(name, value);
      }
    }
    return inline;
  }

  private boolean inlinePropertiesChanged(final Properties wire, final Properties cached) {
    for (final String name : config.getInlinePropertyNames()) {
      if (!Objects.equals(wire.getProperty(name), cached.getProperty(name))) {
        return true;
      }
    }
    return false;
  }

  /** 线上副本: 基础信息 + 内联属性 + 元数据版本; 本地事件仍用 {@link SwimMember#copy()} 携带全量视图。 */
  private ImmutableMember wireCopy(final SwimMember member) {
    return member.copy(inlineProperties(member.properties()));
  }

  private ImmutableMember wireCopy(final ImmutableMember member) {
    return new ImmutableMember(
        member.id(),
        member.nodeVersion(),
        member.address(),
        member.zone(),
        member.rack(),
        member.host(),
        inlineProperties(member.properties()),
        member.version(),
        member.timestamp(),
        member.state(),
        member.incarnationNumber(),
        member.metadataVersion());
  }

  /** 元数据反熵: 每个周期扫描成员表, 版本落后的重新调度拉取(去重表天然合并重复)。 */
  private void checkMetadataAntiEntropy() {
    final long now = System.nanoTime();
    if (now - lastMetadataAntiEntropyNs < config.getMetadataAntiEntropyInterval().toNanos()) {
      return;
    }
    lastMetadataAntiEntropyNs = now;
    for (final SwimMember member : members.values()) {
      scheduleMetadataPull(member, null);
    }
  }

  /**
   * 调度一次元数据拉取: 缓存版本落后于线上版本时, 带随机抖动入队(错峰防拉取风暴), 在途去重。
   *
   * @param member 目标成员
   * @param sourceHint 提供该记录的对端; owner 直连失败时的兜底拉取来源
   */
  private void scheduleMetadataPull(final SwimMember member, final Address sourceHint) {
    if (member.id().equals(localMember.id()) || member.getState() == State.DEAD) {
      return;
    }
    if (member.pulledMetadataVersion() >= member.metadataVersion()) {
      return;
    }
    if (pendingMetadataPulls.putIfAbsent(member.id(), member.metadataVersion()) != null) {
      return;
    }
    final long jitterMs = config.getMetadataPullJitter().toMillis();
    final long delayMs = jitterMs <= 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterMs);
    swimScheduler.schedule(
        () -> {
          final SwimMember current = members.get(member.id());
          if (current == null) {
            pendingMetadataPulls.remove(member.id());
            return;
          }
          if (current.pulledMetadataVersion() >= current.metadataVersion()) {
            pendingMetadataPulls.remove(member.id());
            return;
          }
          pullMetadataFrom(member.id(), current.address(), sourceHint);
        },
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  /**
   * 依次尝试候选地址拉取成员元数据: owner 优先, sourceHint 兜底(owner 与本节点分区但记录经其他对端 中转到达的场景)。第一个带新版本的响应即采纳,
   * 全部失败留给反熵重试。
   */
  private void pullMetadataFrom(
      final MemberId memberId, final Address owner, final Address sourceHint) {
    final List<Address> candidates = new ArrayList<>(2);
    candidates.add(owner);
    if (sourceHint != null && !sourceHint.equals(owner)) {
      candidates.add(sourceHint);
    }
    pullFromCandidates(memberId, candidates, 0);
  }

  private void pullFromCandidates(
      final MemberId memberId, final List<Address> candidates, final int index) {
    if (index >= candidates.size()) {
      pendingMetadataPulls.remove(memberId);
      return;
    }
    bootstrapService
        .getMessagingService()
        .sendAndReceive(
            candidates.get(index),
            MEMBERSHIP_METADATA_PULL,
            SERIALIZER.encode(memberId),
            false,
            config.getProbeTimeout())
        .whenCompleteAsync(
            (response, error) -> {
              if (error == null) {
                try {
                  final Pair<Long, Properties> metadata = SERIALIZER.decode(response);
                  if (applyPulledMetadata(memberId, metadata)) {
                    pendingMetadataPulls.remove(memberId);
                    return;
                  }
                } catch (final Exception decodeError) {
                  LOGGER.debug(
                      "{} - Failed to decode metadata pull response from {}",
                      localMember.id(),
                      candidates.get(index),
                      decodeError);
                }
              }
              pullFromCandidates(memberId, candidates, index + 1);
            },
            swimScheduler);
  }

  /**
   * 应用拉取到的元数据; 仅当响应版本更新时采纳(版本号仲裁, 旧响应/旧中转缓存直接忽略)。
   *
   * @return 是否采纳
   */
  private boolean applyPulledMetadata(final MemberId memberId, final Pair<Long, Properties> data) {
    final SwimMember member = members.get(memberId);
    if (member == null || data.getKey() <= member.pulledMetadataVersion()) {
      return data.getKey() > 0 && member != null;
    }
    final Properties metadata = data.getValue();
    if (metadata != null && !Objects.equals(metadata, member.properties())) {
      member.properties().putAll(metadata);
      LOGGER.debug("{} - Member metadata pulled {}", localMember.id(), member);
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.METADATA_CHANGED, member.copy()));
    }
    member.setPulledMetadataVersion(data.getKey());
    return true;
  }

  /**
   * 应答元数据拉取: 任何节点都可以用自己的成员表应答(含 owner 本人), 新鲜度由版本号仲裁。
   *
   * @param targetId 目标成员
   * @return (元数据版本, 全量属性); 未知成员返回版本 0
   */
  private Pair<Long, Properties> handleMetadataPull(final MemberId targetId) {
    if (localMember.id().equals(targetId)) {
      // 先克隆属性再读版本, 保证版本对应的属性内容完整
      final Properties properties = (Properties) localMember.properties().clone();
      return ImmutablePair.of(localMember.metadataVersion(), properties);
    }
    final SwimMember member = members.get(targetId);
    if (member == null) {
      return ImmutablePair.of(0L, new Properties());
    }
    return ImmutablePair.of(
        member.pulledMetadataVersion() > 0
            ? member.pulledMetadataVersion()
            : member.metadataVersion(),
        (Properties) member.properties().clone());
  }

  /**
   * Gossips this node's pending updates with a random set of peers.
   *
   * @param updates a collection of updated to gossip
   */
  private void gossip(final Collection<ImmutableMember> updates) {
    // Get a list of available peers. If peers are available, randomize the peer list and select a
    // subset of
    // peers with which to gossip updates.
    final List<SwimMember> members = Lists.newArrayList(randomMembers);
    if (!members.isEmpty()) {
      Collections.shuffle(members);
      for (int i = 0; i < Math.min(members.size(), config.getGossipFanout()); i++) {
        gossip(members.get(i), updates);
      }
    }
  }

  /**
   * Gossips this node's pending updates with the given peer.
   *
   * @param member the peer with which to gossip this node's updates
   * @param updates the updated members to gossip
   */
  private void gossip(final SwimMember member, final Collection<ImmutableMember> updates) {
    GOSSIP_LOGGER.trace("{} - Gossipping updates {} to {}", localMember.id(), updates, member);
    bootstrapService
        .getUnicastService()
        .unicast(member.address(), MEMBERSHIP_GOSSIP, SERIALIZER.encode(updates));
  }

  /** Handles a gossip message from a peer. */
  private void handleGossipUpdates(
      final Address source, final Collection<ImmutableMember> updates) {
    for (final ImmutableMember update : updates) {
      GOSSIP_LOGGER.trace("{} - Received gossip {}", localMember.id(), update);
      updateState(update, source);
    }
  }

  /**
   * Handles a node discovery event.
   *
   * <p>discovery 提供者在自己的刷新线程上同步回调本方法, 而 SWIM 状态(members/syncMembers 等) 仅应由 swimScheduler
   * 单线程触达——这里统一切到 swimScheduler, 否则 tryRemoveMember 会与 sync() 并发操作非线程安全集合。
   *
   * @param event the node discovery event
   */
  private void handleDiscoveryEvent(final NodeDiscoveryEvent event) {
    if (!started.get()) {
      return;
    }
    swimScheduler.execute(
        () -> {
          switch (event.type()) {
            case JOIN:
              handleJoinEvent(event.subject());
              break;
            case LEAVE:
              handleLeaveEvent(event.subject());
              break;
            default:
              throw new AssertionError();
          }
        });
  }

  /** Handles a node join event. */
  private void handleJoinEvent(final Node node) {
    // just joined, there's no version yet: it will be sent afterward
    final SwimMember member = new SwimMember(MemberId.from(node.id().id()), node.address());
    if (!members.containsKey(member.id())) {
      probe(wireCopy(member));
    }
  }

  /** Handles a node leave event. */
  private void handleLeaveEvent(final Node node) {
    final SwimMember member = members.get(MemberId.from(node.id().id()));
    if (member != null) {
      // 与 tryRemoveMember 保持同一移除路径：清理 randomMembers/syncMembers 并发布 MEMBER_REMOVED。
      // 移除不经 gossip 传播 DEAD（区别于 checkFailures）：leaves 依赖 discovery 层对每个节点逐一通知
      tryRemoveMember(member);
    }
  }

  /** Registers message handlers for the SWIM protocol. */
  private void registerHandlers() {
    // Register TCP message handlers.
    bootstrapService
        .getMessagingService()
        .registerHandler(MEMBERSHIP_SYNC, syncHandler, swimScheduler);
    bootstrapService
        .getMessagingService()
        .registerHandler(MEMBERSHIP_PROBE, probeHandler, swimScheduler);
    bootstrapService
        .getMessagingService()
        .registerHandler(MEMBERSHIP_PROBE_REQUEST, probeRequestHandler);
    bootstrapService
        .getMessagingService()
        .registerHandler(MEMBERSHIP_METADATA_PULL, metadataPullHandler, swimScheduler);

    // Register UDP message listeners.
    bootstrapService
        .getUnicastService()
        .addListener(MEMBERSHIP_GOSSIP, gossipListener, swimScheduler);
  }

  /** Unregisters handlers for the SWIM protocol. */
  private void unregisterHandlers() {
    // Unregister TCP message handlers.
    bootstrapService.getMessagingService().unregisterHandler(MEMBERSHIP_SYNC);
    bootstrapService.getMessagingService().unregisterHandler(MEMBERSHIP_PROBE);
    bootstrapService.getMessagingService().unregisterHandler(MEMBERSHIP_PROBE_REQUEST);
    bootstrapService.getMessagingService().unregisterHandler(MEMBERSHIP_METADATA_PULL);

    // Unregister UDP message listeners.
    bootstrapService.getUnicastService().removeListener(MEMBERSHIP_GOSSIP, gossipListener);
  }

  private void scheduleGossip() {
    gossipFuture =
        swimScheduler.schedule(
            (Runnable) this::gossip, config.getGossipInterval().toMillis(), TimeUnit.MILLISECONDS);
  }

  private void scheduleSync() {
    syncFuture =
        swimScheduler.schedule(
            (Runnable) this::sync, config.getSyncInterval().toMillis(), TimeUnit.MILLISECONDS);
  }

  private void scheduleProbe() {
    probeFuture =
        swimScheduler.schedule(
            (Runnable) this::probe, config.getProbeInterval().toMillis(), TimeUnit.MILLISECONDS);
  }

  /** SWIM membership protocol type. */
  public static class Type implements GroupMembershipProtocol.Type<SwimMembershipProtocolConfig> {
    private static final String NAME = "swim";

    @Override
    public String name() {
      return NAME;
    }

    @Override
    public GroupMembershipProtocol newProtocol(
        final SwimMembershipProtocolConfig config, final MeterRegistry registry) {
      return new SwimMembershipProtocol(config, registry);
    }
  }

  /**
   * Immutable member.
   *
   * <p>This class is serialized with Fory.
   *
   * <h2>Serialization Revisions</h2>
   *
   * <ul>
   *   <li><b>Revision 1:</b> Initial version with fields: id, address, zone, rack, host,
   *       properties, version, timestamp, state, incarnationNumber
   *   <li><b>Revision 2:</b> Added {@code nodeVersion} field. Defaults to 0L when deserializing
   *       messages from older versions.
   *   <li><b>Revision 3:</b> Added {@code metadataVersion} field. Defaults to 0L when deserializing
   *       messages from older versions. 线上 {@code properties} 仅携带白名单内联 属性, 全量元数据由版本比对触发的拉取通道传播.
   * </ul>
   */
  static class ImmutableMember extends Member {
    private final Version version;
    private final long timestamp;
    private final State state;
    private final long incarnationNumber;
    private final long metadataVersion;

    ImmutableMember(
        final MemberId id,
        final long nodeVersion,
        final Address address,
        final String zone,
        final String rack,
        final String host,
        final Properties properties,
        final Version version,
        final long timestamp,
        final State state,
        final long incarnationNumber,
        final long metadataVersion) {
      super(id, nodeVersion, address, zone, rack, host, properties);
      this.version = version;
      this.timestamp = timestamp;
      this.state = state;
      this.incarnationNumber = incarnationNumber;
      this.metadataVersion = metadataVersion;
    }

    @Override
    public String toString() {
      final var helper = toStringHelper(Member.class);
      helper.add("id", id());
      if (nodeVersion() > 0) {
        helper.add("nodeVersion", nodeVersion());
      }
      helper
          .add("address", address())
          .add("properties", properties())
          .add("version", version())
          .add("timestamp", timestamp())
          .add("state", state())
          .add("incarnationNumber", incarnationNumber());
      return helper.toString();
    }

    @Override
    public Version version() {
      return version;
    }

    @Override
    public long timestamp() {
      return timestamp;
    }

    /**
     * Returns the member's state.
     *
     * @return the member's state
     */
    State state() {
      return state;
    }

    /**
     * Returns the member's incarnation number.
     *
     * @return the member's incarnation number
     */
    long incarnationNumber() {
      return incarnationNumber;
    }

    /**
     * Returns the metadata version carried on the wire; receivers pull full metadata when their
     * cached version is older.
     *
     * @return the metadata version
     */
    long metadataVersion() {
      return metadataVersion;
    }
  }

  /**
   * Swim member.
   *
   * <p>This class is serialized with Fory.
   *
   * <h2>Serialization Revisions</h2>
   *
   * <ul>
   *   <li><b>Revision 1:</b> Initial version with fields: id, address, zone, rack, host,
   *       properties, version, timestamp, state, incarnationNumber, updated
   *   <li><b>Revision 2:</b> Added {@code nodeVersion} field. Defaults to 0L when deserializing
   *       messages from older versions.
   * </ul>
   */
  static class SwimMember extends Member {
    private final Version version;
    private final long timestamp;
    private volatile State state;
    private volatile long incarnationNumber;
    private volatile long updated;

    /** 最近一次线上记录携带的元数据版本（版本号仲裁用）。 */
    private volatile long metadataVersion;

    /** 本地缓存的全量元数据对应的版本；0 表示尚未拉取过（本地成员恒等于 metadataVersion）。 */
    private volatile long pulledMetadataVersion;

    SwimMember(final MemberId id, final Address address) {
      super(id, address);
      version = null;
      timestamp = 0;
    }

    SwimMember(
        final MemberId id,
        final long nodeVersion,
        final Address address,
        final String zone,
        final String rack,
        final String host,
        final Properties properties,
        final Version version,
        final long timestamp) {
      super(id, nodeVersion, address, zone, rack, host, properties);
      this.version = version;
      this.timestamp = timestamp;
      incarnationNumber = System.currentTimeMillis();
    }

    SwimMember(final ImmutableMember member) {
      super(
          member.id(),
          member.nodeVersion(),
          member.address(),
          member.zone(),
          member.rack(),
          member.host(),
          member.properties());
      version = member.version;
      timestamp = member.timestamp;
      state = member.state;
      incarnationNumber = member.incarnationNumber;
      metadataVersion = member.metadataVersion;
      // 线上 properties 仅内联白名单, 全量元数据未知 → 拉取前视为 0
      pulledMetadataVersion = 0;
    }

    /**
     * Returns the member's state.
     *
     * @return the member's state
     */
    State getState() {
      return state;
    }

    /**
     * Changes the member's state.
     *
     * @param state the member's state
     */
    void setState(final State state) {
      if (this.state != state) {
        this.state = state;
        setUpdated(System.currentTimeMillis());
      }
    }

    /**
     * Returns the member incarnation number.
     *
     * @return the member incarnation number
     */
    long getIncarnationNumber() {
      return incarnationNumber;
    }

    /**
     * Sets the member's incarnation number.
     *
     * @param incarnationNumber the member's incarnation number
     */
    void setIncarnationNumber(final long incarnationNumber) {
      this.incarnationNumber = incarnationNumber;
    }

    /** Returns the latest wire metadata version seen for this member. */
    long metadataVersion() {
      return metadataVersion;
    }

    void setMetadataVersion(final long metadataVersion) {
      this.metadataVersion = metadataVersion;
    }

    /** Returns the version of the locally cached full metadata; 0 when never pulled. */
    long pulledMetadataVersion() {
      return pulledMetadataVersion;
    }

    void setPulledMetadataVersion(final long pulledMetadataVersion) {
      this.pulledMetadataVersion = pulledMetadataVersion;
    }

    /**
     * Returns the wall clock timestamp.
     *
     * @return the wall clock timestamp
     */
    long getUpdated() {
      return updated;
    }

    /**
     * Sets the wall clock timestamp.
     *
     * @param updated the wall clock timestamp
     */
    void setUpdated(final long updated) {
      this.updated = updated;
    }

    /**
     * Copies the member's state to a new object. The copy shares the properties instance with this
     * member, so it observes any later mutation of them. Use {@link #copy(Properties)} to copy with
     * a stable set of properties.
     *
     * @return the copied object
     */
    ImmutableMember copy() {
      return copy(properties());
    }

    /**
     * Copies the member's state to a new object, using the given properties instead of this
     * member's own, mutable ones.
     *
     * @param properties the properties the copy should report
     * @return the copied object
     */
    ImmutableMember copy(final Properties properties) {
      return new ImmutableMember(
          id(),
          nodeVersion(),
          address(),
          zone(),
          rack(),
          host(),
          properties,
          version(),
          timestamp(),
          state,
          incarnationNumber,
          metadataVersion);
    }

    @Override
    public boolean isActive() {
      return state.isActive();
    }

    @Override
    public boolean isReachable() {
      return state.isReachable();
    }

    @Override
    public Version version() {
      return version;
    }

    @Override
    public long timestamp() {
      return timestamp;
    }
  }

  /** Member states. */
  enum State {
    ALIVE(true, true),
    SUSPECT(true, false),
    DEAD(false, false);

    private final boolean active;
    private final boolean reachable;

    State(final boolean active, final boolean reachable) {
      this.active = active;
      this.reachable = reachable;
    }

    boolean isActive() {
      return active;
    }

    boolean isReachable() {
      return reachable;
    }
  }
}
