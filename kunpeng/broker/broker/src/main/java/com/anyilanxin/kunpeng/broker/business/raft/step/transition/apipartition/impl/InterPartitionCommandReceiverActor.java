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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.INNER_PARTITION_TOPIC_PREFIX;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.monitoring.DiskSpaceUsageListener;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.utils.serializer.serializers.DefaultSerializers;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Receives messages send by @{@link InterPartitionCommandSenderService} and tries to write them as
 * commands to the partition's log stream. Whether the command made it into the log is acknowledged
 * to the sender with {@link InterPartitionDeliveryStatus}; the sender retries commands that were
 * definitely not appended.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InterPartitionCommandReceiverActor extends Actor
    implements DiskSpaceUsageListener {
  private static final Logger LOG = BrokerLoggers.CLUSTER_BUSINESS;
  private final String actorName;
  private final ClusterCommunicationService communicationService;
  private final PartitionId partitionId;
  private final InterPartitionCommandReceiverImpl receiver;

  public InterPartitionCommandReceiverActor(
      final PartitionId partitionId,
      final ClusterCommunicationService communicationService,
      final EventLogWriter logStreamWriter,
      final RecordValueMapper valueMapper) {
    this.partitionId = partitionId;
    this.communicationService = communicationService;
    receiver = new InterPartitionCommandReceiverImpl(logStreamWriter, valueMapper);
    actorName = buildActorName(getClass().getSimpleName(), partitionId.toString());
  }

  @Override
  protected Map<String, String> createContext() {
    final var context = super.createContext();
    context.put("partitionId", partitionId.toString());
    return context;
  }

  @Override
  public String getName() {
    return actorName;
  }

  @Override
  protected void onActorStarting() {
    communicationService.replyTo(
        INNER_PARTITION_TOPIC_PREFIX + partitionId,
        DefaultSerializers.BASIC::decode,
        this::handleMessage,
        InterPartitionDeliveryStatus::toBytes,
        actor::run);
  }

  @Override
  protected void onActorClosing() {
    communicationService.unsubscribe(INNER_PARTITION_TOPIC_PREFIX + partitionId);
  }

  @Override
  public void onDiskSpaceNotAvailable() {
    actor.run(() -> receiver.setDiskSpaceAvailable(false));
  }

  @Override
  public void onDiskSpaceAvailable() {
    actor.run(() -> receiver.setDiskSpaceAvailable(true));
  }

  private InterPartitionDeliveryStatus handleMessage(
      final MemberId memberId, final byte[] message) {
    try {
      return receiver.handleMessage(memberId, message);
    } catch (final RuntimeException e) {
      LOG.error("Error while handling message from {}", memberId, e);
      // 原样抛出让应答以处理器异常完结：命令是否已写入未知，发送端据此不重发
      throw e;
    }
  }
}
