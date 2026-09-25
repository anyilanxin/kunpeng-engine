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
package com.anyilanxin.kunpeng.cluster.config.messaging;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.ClusterSwimTopologyService;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分区级消息服务：把 topic 的投递范围限定在当前分区的成员内。
 *
 * <p>成员列表来自 {@link ClusterSwimTopologyService} 的 SWIM 拓扑视图；wire 上的 subject 以分区标识做 命名空间隔离，不同分区的同名
 * topic 互不串扰。广播走不可靠多播：适合周期性幂等快照，丢一轮等下轮。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class PartitionMessagingService {

  private static final Logger LOG = LoggerFactory.getLogger(PartitionMessagingService.class);

  private final ClusterCommunicationService communicationService;
  private final ClusterSwimTopologyService topologyService;
  private final PartitionId partitionId;
  private final MemberId localMemberId;

  public PartitionMessagingService(
      final ClusterCommunicationService communicationService,
      final ClusterSwimTopologyService topologyService,
      final PartitionId partitionId,
      final MemberId localMemberId) {
    this.communicationService = communicationService;
    this.topologyService = topologyService;
    this.partitionId = partitionId;
    this.localMemberId = localMemberId;
  }

  /**
   * 订阅本分区上的 topic，收到的消息在 {@code executor} 上派发给 {@code handler}。
   *
   * @param topic 逻辑 topic 名
   * @param handler 消息处理器
   * @param executor 处理器执行线程
   */
  public void subscribe(
      final String topic, final Consumer<ByteBuffer> handler, final Executor executor) {
    communicationService.consume(subject(topic), ByteBuffer::wrap, handler, executor);
  }

  /**
   * 向本分区的其他成员广播一条消息（不含本地成员）。
   *
   * @param topic 逻辑 topic 名
   * @param payload 待广播的消息内容
   */
  public void broadcast(final String topic, final ByteBuffer payload) {
    final var recipientIds = partitionMembersExcludingSelf();
    if (recipientIds.isEmpty()) {
      LOG.warn(
          "No other members found for partition {}, skipping broadcast on topic {}",
          partitionId,
          topic);
      return;
    }
    communicationService.multicast(
        subject(topic), payload, PartitionMessagingService::toByteArray, recipientIds, false);
  }

  /** 取消订阅本分区上的 topic。 */
  public void unsubscribe(final String topic) {
    communicationService.unsubscribe(subject(topic));
  }

  private String subject(final String topic) {
    return topic + "-" + partitionId;
  }

  private Set<MemberId> partitionMembersExcludingSelf() {
    final var memberInfos = topologyService.getPartitionMemberInfo(partitionId);
    if (memberInfos == null || memberInfos.isEmpty()) {
      return Set.of();
    }
    return memberInfos.stream()
        .map(PartitionMemberInfo::getMemberId)
        .filter(Objects::nonNull)
        .map(MemberId::from)
        .filter(memberId -> !memberId.equals(localMemberId))
        .collect(Collectors.toSet());
  }

  private static byte[] toByteArray(final ByteBuffer buffer) {
    if (buffer.hasArray() && buffer.position() == 0 && buffer.limit() == buffer.array().length) {
      return buffer.array();
    }
    final var bytes = new byte[buffer.remaining()];
    buffer.duplicate().get(bytes);
    return bytes;
  }
}
