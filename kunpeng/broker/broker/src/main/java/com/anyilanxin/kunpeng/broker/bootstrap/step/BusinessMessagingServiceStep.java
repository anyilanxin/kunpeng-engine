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
package com.anyilanxin.kunpeng.broker.bootstrap.step;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.broker.topology.TopologyManager;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronMessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronTransport;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterConfigFactory;
import com.anyilanxin.kunpeng.protocol.common.member.CommPortType;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 业务面消息服务的 broker 启动步骤：构建并启动独立端口的业务消息实例（命令接收与集群面 Raft 流量隔离）， 并把端口写入本进程唯一的 broker
 * 传播实体经拓扑管理服务发布——通知整个集群（对端经元数据二次拉取补全后可寻址）。
 *
 * <p>业务面实例仅 broker 侧需要（接收端隔离）；发送侧（gateway/broker 的 BrokerClient）直接使用集群面 Atomix 通信组件向本端口寻址。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessMessagingServiceStep extends AbstractBrokerStartupStep {

  @Override
  public String getName() {
    return "Business Messaging Service";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    brokerStartupContext
        .getConcurrencyControl()
        .run(
            () -> {
              try {
                final AeronMessagingService service =
                    buildService(brokerStartupContext.getClusterCft());
                service.start().join();
                // 端口写入唯一传播实体并经管理服务发布（成员属性的唯一写入者）
                final TopologyManager topologyManager =
                    brokerStartupContext.getClusterPartitionTopology();
                topologyManager
                    .localBroker()
                    .addPort(CommPortType.BUSINESS, service.address().port());
                topologyManager.publishBroadcast();
                brokerStartupContext.setBusinessMessagingService(service);
                startupFuture.complete(brokerStartupContext);
              } catch (final Exception e) {
                startupFuture.completeExceptionally(e);
              }
            });
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    brokerShutdownContext
        .getConcurrencyControl()
        .run(
            () -> {
              // 逆序关闭时拓扑管理服务尚存活：先摘端口注册并发布，再停服务（服务未启动则无端口可摘）
              final AeronMessagingService service =
                  brokerShutdownContext.getBusinessMessagingService();
              try {
                final TopologyManager topologyManager =
                    brokerShutdownContext.getClusterPartitionTopology();
                if (topologyManager != null && service != null) {
                  topologyManager.localBroker().getPorts().remove(CommPortType.BUSINESS);
                  topologyManager.publishBroadcast();
                }
                if (service != null) {
                  service.stop().join();
                }
                brokerShutdownContext.setBusinessMessagingService(null);
                shutdownFuture.complete(brokerShutdownContext);
              } catch (final Exception e) {
                shutdownFuture.completeExceptionally(e);
              }
            });
  }

  /** 构建业务面消息服务实例：独立端口的传输底座（与集群面各自的代理线程/队列/socket，内嵌驱动为进程级共享）。 */
  private static AeronMessagingService buildService(final ClusterCfg clusterCfg) {
    final MessagingConfig messagingConfig =
        new ClusterConfigFactory().businessMessagingConfig(clusterCfg);
    if (messagingConfig.getPort() == null) {
      throw new IllegalArgumentException("业务面消息服务缺少端口配置");
    }
    final String host =
        messagingConfig.getInterfaces().isEmpty()
            ? Address.defaultAdvertisedHost().getHostAddress()
            : messagingConfig.getInterfaces().get(0);
    final Address bindAddress = Address.from(host, messagingConfig.getPort());
    // 模板取集群面默认 Aeron 配置（Aeron 参数未开放用户配置，与集群面实例恒等）；加密发送方按节点 ID 派生，与 AtomixCluster 的集群面行为一致
    final AeronMessagingConfig template = new AeronMessagingConfig();
    if (template.getCryptoSenderId() == null) {
      template.setCryptoSenderId(clusterCfg.getNodeId());
    }
    final AeronMessagingConfig businessAeronConfig = aeronConfigOf(template);
    return new AeronMessagingService(
        bindAddress,
        messagingConfig,
        businessAeronConfig,
        new AeronTransport(bindAddress, messagingConfig, businessAeronConfig));
  }

  /**
   * 由集群面 Aeron 配置派生业务面配置：继承流 id、队列容量与加密设置，term 缩为 32MiB（单条上限 4MiB，业务面无快照大块， 与业务网络配置的 maxMessageSize
   * 默认值对齐）。
   */
  private static AeronMessagingConfig aeronConfigOf(final AeronMessagingConfig clusterConfig) {
    return new AeronMessagingConfig()
        .setStreamId(clusterConfig.getStreamId())
        .setUnicastStreamId(clusterConfig.getUnicastStreamId())
        .setTermBufferLength(Math.min(clusterConfig.getTermBufferLength(), 32 * 1024 * 1024))
        .setUnicastTermBufferLength(clusterConfig.getUnicastTermBufferLength())
        .setSendQueueCapacity(clusterConfig.getSendQueueCapacity())
        .setConnectTimeout(clusterConfig.getConnectTimeout())
        .setEmbeddedDriver(clusterConfig.isEmbeddedDriver())
        .setAeronDir(clusterConfig.getAeronDir())
        .setDriverThreadingMode(clusterConfig.getDriverThreadingMode())
        .setDriverTimeoutMs(clusterConfig.getDriverTimeoutMs())
        .setIdleStrategy(clusterConfig.getIdleStrategy())
        .setMtuLength(clusterConfig.getMtuLength())
        .setSocketSendBufferBytes(clusterConfig.getSocketSendBufferBytes())
        .setSocketReceiveBufferBytes(clusterConfig.getSocketReceiveBufferBytes())
        // 集群面与业务面双实例共存于同一进程, 共享同一内嵌驱动(集群面开启 sharedDriver 时)
        .setSharedDriver(clusterConfig.isSharedDriver())
        .setCryptoEnabled(clusterConfig.isCryptoEnabled())
        .setCryptoKeys(clusterConfig.getCryptoKeys())
        .setSendCryptoKeyId(clusterConfig.getSendCryptoKeyId())
        .setCryptoSenderId(clusterConfig.getCryptoSenderId());
  }
}
