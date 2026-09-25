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
package com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover;

import com.anyilanxin.kunpeng.cluster.cluster.*;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.gateway.grpc.health.ActiveConnectionCounter;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayConnectorInfoSerializer;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayConnectorRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayInfo;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.agrona.collections.Object2ObjectHashMap;
import org.slf4j.Logger;

/**
 * 网关拓扑管理实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayTopologyManagerImpl extends Actor
    implements GatewayTopologyManager, ClusterMembershipEventListener {
  public static final String LOAD_QUERY_SUBJECT = "gateway-load-query";
  private static final Duration FORWARD_TIMEOUT = Duration.ofSeconds(8);
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;

  private final Object2ObjectHashMap<String, GatewayInfo> gateways = new Object2ObjectHashMap<>();
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicationService;
  private final GatewayInfo localGateway;
  private volatile ActiveConnectionCounter connectionCounter;

  private final String actorName;

  public GatewayTopologyManagerImpl(
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicationService,
      final GatewayInfo localGateway) {
    this.membershipService = membershipService;
    this.communicationService = communicationService;
    this.localGateway = localGateway;
    actorName = "GatewayTopologyManager";
  }

  /**
   * Called by {@code GatewayGrpcService} after both are wired up; safe to invoke before actor
   * start. The counter is read on-demand by {@link
   * com.anyilanxin.kunpeng.gateway.grpc.service.impl.GrpcClusterManageServiceImpl#queryGatewayLoad}
   * — load is no longer broadcast through cluster metadata.
   */
  public void setConnectionCounter(final ActiveConnectionCounter connectionCounter) {
    this.connectionCounter = connectionCounter;
  }

  @Override
  public List<GatewayInfo> getGateways() {
    synchronized (gateways) {
      return new ArrayList<>(gateways.values());
    }
  }

  @Override
  public CompletableFuture<GatewayConnectorRecord> getGatewayLoad(final String gatewayGrpcAddress) {
    final CompletableFuture<GatewayConnectorRecord> future = new CompletableFuture<>();
    if (gatewayGrpcAddress == null || localGateway.getGrpcAddress().equals(gatewayGrpcAddress)) {
      final GatewayConnectorRecord localGatewayConnectorRecord =
          new GatewayConnectorRecord(
              localGateway.getNodeId(), connectionCounter.getActiveConnections());
      future.complete(localGatewayConnectorRecord);
    } else {
      final Optional<GatewayInfo> first =
          getGateways().stream()
              .filter(gateway -> gateway.getGrpcAddress().equals(gatewayGrpcAddress))
              .findFirst();
      if (first.isEmpty()) {
        future.completeExceptionally(new IllegalStateException("网关信息未知"));
      } else {
        final GatewayInfo gatewayInfo = first.get();
        communicationService
            .send(
                LOAD_QUERY_SUBJECT,
                new byte[0],
                Function.identity(),
                GatewayConnectorInfoSerializer::decode,
                MemberId.from(gatewayInfo.getNodeId()),
                FORWARD_TIMEOUT)
            .whenComplete(
                (count, ex) -> {
                  if (ex != null) {
                    future.completeExceptionally(new IllegalStateException("网关信息未知"));
                  } else {
                    future.complete(count);
                  }
                });
      }
    }
    return future;
  }

  @Override
  public String getName() {
    return actorName;
  }

  @Override
  protected void onActorStarted() {
    // ensures that the first published event will contain the broker's info
    publishTopologyChanges();
    synchronized (gateways) {
      gateways.put(localGateway.getNodeId(), localGateway);
    }
    membershipService.addListener(this);
    membershipService
        .getMembers()
        .forEach(
            m -> event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, m)));
    registerLoadQueryHandler();
  }

  /**
   * Replies to {@value #LOAD_QUERY_SUBJECT} requests from peer gateways with this node's current
   * active-connection count. Runs on the actor thread; the counter is a volatile read so the
   * handler never blocks.
   */
  private void registerLoadQueryHandler() {
    communicationService.replyTo(
        LOAD_QUERY_SUBJECT,
        Function.identity(),
        (_, _) -> currentActiveConnections(),
        GatewayConnectorInfoSerializer::encode,
        actor::run);
  }

  private GatewayConnectorRecord currentActiveConnections() {
    final ActiveConnectionCounter counter = connectionCounter;
    return new GatewayConnectorRecord(
        localGateway.getNodeId(), counter == null ? 0 : counter.getActiveConnections());
  }

  @Override
  public void event(final ClusterMembershipEvent clusterMembershipEvent) {
    final Member eventSource = clusterMembershipEvent.subject();
    final GatewayInfo gatewayInfo = GatewayInfo.fromProperties(eventSource.properties());
    if (gatewayInfo != null && !gatewayInfo.getNodeId().equals(localGateway.getNodeId())) {
      actor.run(
          () -> {
            switch (clusterMembershipEvent.type()) {
              case MEMBER_ADDED:
              case METADATA_CHANGED:
                onMetadataChanged(gatewayInfo);
                break;
              case MEMBER_REMOVED:
                onMemberRemoved(gatewayInfo);
                break;
              case REACHABILITY_CHANGED:
              default:
                LOG.debug(
                    "Gateway Topology Received {} from member {}, was not handled.",
                    clusterMembershipEvent.type(),
                    gatewayInfo.getNodeId());
                break;
            }
          });
    }
  }

  // Remove a member from the gateway topology
  private void onMemberRemoved(final GatewayInfo gatewayInfo) {
    LOG.debug("Received member removed {} ", gatewayInfo);
    synchronized (gateways) {
      gateways.remove(gatewayInfo.getNodeId());
    }
  }

  // Update local knowledge about the remote node
  private void onMetadataChanged(final GatewayInfo gatewayInfo) {
    LOG.debug("Received metadata change for {}", gatewayInfo.getNodeId());
    synchronized (gateways) {
      gateways.put(gatewayInfo.getNodeId(), gatewayInfo);
    }
  }

  // Propagate local node info to other nodes through Atomix member properties
  private void publishTopologyChanges() {
    final Properties memberProperties = membershipService.getLocalMember().properties();
    localGateway.writeIntoProperties(memberProperties);
  }
}
