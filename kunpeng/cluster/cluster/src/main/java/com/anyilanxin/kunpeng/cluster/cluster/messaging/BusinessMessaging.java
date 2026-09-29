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
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronMessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.AeronTransport;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务面消息通信（gateway↔broker 业务命令 RPC）的公共约定。
 *
 * <p>业务面是独立端口的 {@link AeronMessagingService} 实例，与集群面（SWIM/Raft/快照/元数据）隔离——独立代理线程、独立有界入队队列与独立
 * socket，互不挤压。对端寻址依赖端口广播：本节点将业务端口写入本地成员属性（<b>非内联白名单键</b>，随全量元数据经版本差触发的二次拉取传播， 拉取合并完成前消费方读不到该端口）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessMessaging {
  private static final Logger LOGGER = LoggerFactory.getLogger(BusinessMessaging.class);

  private BusinessMessaging() {}

  /**
   * 解析成员的业务面地址（成员主机 + 其广播的业务端口，端口经 {@link MemberNodeInfo} 注册表读取）。
   *
   * @return 业务面地址; 成员尚未广播端口（元数据未拉取补全）时返回 {@code null}
   */
  public static Address businessAddressOf(final Member member) {
    if (member == null || member.host() == null) {
      return null;
    }
    final Integer port = MemberNodeInfo.portOf(member, CommPortType.BUSINESS);
    return port == null ? null : Address.from(member.host(), port);
  }

  /**
   * 由集群面 Aeron 配置派生业务面配置：继承流 id、队列容量与加密设置，term 缩为 32MiB（单条上限 4MiB，业务面无快照大块， 与业务网络配置的 maxMessageSize
   * 默认值对齐）。
   */
  public static AeronMessagingConfig aeronConfigOf(final AeronMessagingConfig clusterConfig) {
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

  /**
   * 启动业务面服务并把端口写入本地成员属性（广播给集群）。
   *
   * <p>属性为原地变更，SWIM 成员协议的元数据巡检会差分检测到变化：版本 +1 后线上携带新版本，对端经二次拉取补全并触发成员事件。
   */
  public static void startAndAdvertise(
      final AeronMessagingService service, final ClusterMembershipService membershipService) {
    service.start().join();
    final int port = service.address().port();
    MemberNodeInfo.updatePort(
        membershipService.getLocalMember().properties(), CommPortType.BUSINESS, port);
    LOGGER.info("业务面消息服务已启动并广播端口: {}", service.address());
  }

  /**
   * 构建业务面消息服务实例：独立端口的传输底座（与集群面各自的代理线程/队列/socket，内嵌驱动为进程级共享）。
   *
   * @param messagingConfig 业务面网络配置（host/port）
   * @param clusterAeronConfig 集群面 Aeron 配置（派生业务面配置的模板）
   */
  public static AeronMessagingService buildService(
      final MessagingConfig messagingConfig, final AeronMessagingConfig clusterAeronConfig) {
    if (messagingConfig.getPort() == null) {
      throw new IllegalArgumentException("业务面消息服务缺少端口配置");
    }
    final String host =
        messagingConfig.getInterfaces().isEmpty()
            ? Address.defaultAdvertisedHost().getHostAddress()
            : messagingConfig.getInterfaces().get(0);
    final Address bindAddress = Address.from(host, messagingConfig.getPort());
    final AeronMessagingConfig businessAeronConfig = aeronConfigOf(clusterAeronConfig);
    return new AeronMessagingService(
        bindAddress,
        messagingConfig,
        businessAeronConfig,
        new AeronTransport(bindAddress, messagingConfig, businessAeronConfig));
  }
}
