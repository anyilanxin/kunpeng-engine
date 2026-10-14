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
import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.SwimMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.BrokerNetworkCfg;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 集群配置工厂，将 ClusterCfg 映射为集群运行时所需的 ClusterConfig。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ClusterConfigFactory {

  /** DataSize 显式转 int 字节数：越界直接拒绝，防止 2GB 以上配置被静默截断为负数 */
  public static int toIntBytes(final org.springframework.util.unit.DataSize size) {
    final long bytes = size.toBytes();
    if (bytes < 0 || bytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "DataSize out of int range [0, " + Integer.MAX_VALUE + "]: " + bytes + " bytes");
    }
    return (int) bytes;
  }

  /** long 字节数显式转 int：越界直接拒绝，防止静默截断为负数 */
  public static int toIntBytes(final long bytes) {
    if (bytes < 0 || bytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "bytes out of int range [0, " + Integer.MAX_VALUE + "]: " + bytes);
    }
    return (int) bytes;
  }

  public ClusterConfig mapConfiguration(final ClusterCfg cluster, final boolean broker) {
    final var name = cluster.getClusterName();
    final var discovery = discoveryConfig(cluster.getSeedNodes());
    final var membership = membershipConfig(cluster.getMembership());
    final var network = cluster.getNetwork();
    final var member = memberConfig(network, cluster.getNodeId(), broker);
    final MessagingConfig messagingConfig = memberMessagingConfig(network);

    // Aeron 传输面仅开放自寻址 IPC 短路开关（同实例自发自收，业务面单边接收不参与），其余参数维持内置默认；驱动每实例独占
    final var aeronConfig =
        new AeronMessagingConfig().setLocalIpcEnabled(cluster.isLocalIpcEnabled());
    return new ClusterConfig()
        .setClusterId(name)
        .setNodeConfig(member)
        .setMessagingConfig(messagingConfig)
        .setAeronConfig(aeronConfig)
        .setDiscoveryConfig(discovery)
        .setProtocolConfig(membership);
  }

  /**
   * 业务面消息服务（独立端口）的 {@link MessagingConfig}：与集群面隔离，承载 gateway↔broker 业务命令 RPC。
   *
   * <p>端口与压缩等取自 broker 网络配置（{@code broker.network}，默认 start-port + port-offset）。
   */
  public MessagingConfig businessMessagingConfig(final BrokerCfg broker) {
    final BrokerNetworkCfg network = broker.getNetwork();
    final var messaging =
        new MessagingConfig()
            .setCompressionAlgorithm(network.getMessageCompression())
            .setInterfaces(Collections.singletonList(network.getHost()))
            .setPort(network.getPort());
    if (network.getSocketSendBuffer() != null) {
      messaging.setSocketSendBuffer(toIntBytes(network.getSocketSendBuffer()));
    }
    if (network.getSocketReceiveBuffer() != null) {
      messaging.setSocketReceiveBuffer(toIntBytes(network.getSocketReceiveBuffer()));
    }
    return messaging;
  }

  private MemberConfig memberConfig(
      final MembershipNetworkCfg network, final String nodeId, final boolean broker) {
    final var memberAddress = Address.from(network.getHost(), network.getPort());
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
      messaging.setSocketSendBuffer(toIntBytes(membershipNetwork.getSocketSendBuffer()));
    }
    if (membershipNetwork.getSocketReceiveBuffer() != null) {
      messaging.setSocketReceiveBuffer(toIntBytes(membershipNetwork.getSocketReceiveBuffer()));
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
