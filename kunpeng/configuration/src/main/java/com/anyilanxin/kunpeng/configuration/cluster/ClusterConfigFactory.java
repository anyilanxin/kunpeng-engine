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
package com.anyilanxin.kunpeng.configuration.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterConfig;
import com.anyilanxin.kunpeng.cluster.cluster.MemberConfig;
import com.anyilanxin.kunpeng.cluster.cluster.NodeConfig;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.BootstrapDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/** 集群配置工厂，将 ClusterCfg 映射为集群运行时所需的 ClusterConfig。 */
public final class ClusterConfigFactory {

  public ClusterConfig mapConfiguration(final ClusterCfg cluster, final boolean broker) {
    final var name = cluster.getClusterName();
    final var discovery = discoveryConfig(cluster.getSeedNodes());
    final var membership = membershipConfig(cluster.getMembership());
    final var network = cluster.getNetwork();
    final var member = memberConfig(network, cluster.getNodeId(), broker);
    final MessagingConfig messagingConfig = memberMessagingConfig(network.getMembership());

    return new ClusterConfig()
        .setClusterId(name)
        .setNodeConfig(member)
        .setMessagingConfig(messagingConfig)
        .setDiscoveryConfig(discovery)
        .setProtocolConfig(membership);
  }

  private MemberConfig memberConfig(
      final NetworkInfoCfg network, final String nodeId, final boolean broker) {
    final MembershipNetworkCfg membershipNetwork = network.getMembership();
    final var memberAddress =
        Address.from(membershipNetwork.getHost(), membershipNetwork.getPort());
    final String zone = broker ? ZoneType.BROKER.getType() : ZoneType.GATEWAY.getType();
    return new MemberConfig().setId(nodeId, zone).setAddress(memberAddress);
  }

  private SwimMembershipProtocolConfig membershipConfig(final MembershipCfg config) {
    return new SwimMembershipProtocolConfig()
        .setBroadcastDisputes(config.isBroadcastDisputes())
        .setBroadcastUpdates(config.isBroadcastUpdates())
        .setFailureTimeout(config.getFailureTimeout())
        .setGossipFanout(config.getGossipFanout())
        .setGossipInterval(config.getGossipInterval())
        .setNotifySuspect(config.isNotifySuspect())
        .setProbeInterval(config.getProbeInterval())
        .setProbeTimeout(config.getProbeTimeout())
        .setSuspectProbes(config.getSuspectProbes())
        .setSyncInterval(config.getSyncInterval());
  }

  private BootstrapDiscoveryConfig discoveryConfig(final Collection<String> contactPoints) {
    final var nodes =
        contactPoints.stream()
            .map(Address::from)
            .map(address -> new NodeConfig().setAddress(address))
            .collect(Collectors.toSet());
    return new BootstrapDiscoveryConfig().setNodes(nodes);
  }

  private MessagingConfig memberMessagingConfig(final MembershipNetworkCfg membershipNetwork) {
    final var messaging =
        new MessagingConfig()
            .setCompressionAlgorithm(membershipNetwork.getMessageCompression())
            .setInterfaces(Collections.singletonList(membershipNetwork.getHost()))
            .setPort(membershipNetwork.getPort())
            .setHeartbeatTimeout(membershipNetwork.getHeartbeatTimeout())
            .setHeartbeatInterval(membershipNetwork.getHeartbeatInterval());
    if (membershipNetwork.getSocketSendBuffer() != null) {
      messaging.setSocketSendBuffer((int) membershipNetwork.getSocketSendBuffer().toBytes());
    }
    if (membershipNetwork.getSocketReceiveBuffer() != null) {
      messaging.setSocketReceiveBuffer((int) membershipNetwork.getSocketReceiveBuffer().toBytes());
    }

    if (membershipNetwork.getSecurity().isEnabled()) {
      final var security = membershipNetwork.getSecurity();

      messaging
          .setTlsEnabled(true)
          .configureTls(
              security.getKeyStore().getFilePath(),
              security.getKeyStore().getPassword(),
              security.getPrivateKeyPath(),
              security.getCertificateChainPath());
    }
    return messaging;
  }
}
