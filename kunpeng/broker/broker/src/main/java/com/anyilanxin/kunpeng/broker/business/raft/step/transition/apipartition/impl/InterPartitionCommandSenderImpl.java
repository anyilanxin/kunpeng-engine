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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.CLUSTER_LEADER_PARTITION;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.PARTITION_GLOBAL_SOURCE;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.protocol.InterPartitionMessageEncoder;
import com.anyilanxin.kunpeng.broker.protocol.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.utils.serializer.serializers.DefaultSerializers;
import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.HashSet;
import java.util.Set;
import org.agrona.collections.IntHashSet;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 跨分区命令发送器实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class InterPartitionCommandSenderImpl implements InterPartitionCommandSender {
  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();
  public static final String TOPIC_PREFIX = "inter-partition-";

  private static final Logger LOG = BrokerLoggers.TRANSPORT_LOGGER;
  private final ClusterCommunicationService communicationService;
  private final IntHashSet activitySources = new IntHashSet(-1);
  private final PartitionRouteService routeService = new PartitionRouteService();
  // Reused per send — safe because all entry points are dispatched through the sender actor
  // (single-threaded). Only the byte[] backing the message buffer is allocated per call, since
  // ownership is handed off to the cluster communication service.
  private final MessageHeaderEncoder reusableHeaderEncoder = new MessageHeaderEncoder();
  private final InterPartitionMessageEncoder reusableBodyEncoder =
      new InterPartitionMessageEncoder();
  private final UnsafeBuffer reusableCommandBuffer = new UnsafeBuffer(0, 0);
  private final UnsafeBuffer reusableMessageBuffer = new UnsafeBuffer(0, 0);
  private byte[] reusableCommandBytes = new byte[0];

  public InterPartitionCommandSenderImpl(final ClusterCommunicationService communicationService) {
    this.communicationService = communicationService;
  }

  @Override
  public void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue command) {
    sendCommand(receiverResourceId, lifeCycle, null, command);
  }

  @Override
  public void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final UnifiedRecordValue command) {
    sendCommand(receiverResourceId, lifeCycle, recordKey, -1L, command);
  }

  @Override
  public void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command) {

    if (!activitySources.contains(receiverResourceId)) {
      LOG.warn(
          "Not sending command {} {} to {}, no known leader for this partition",
          lifeCycle.getValueType(),
          lifeCycle,
          receiverResourceId);
      return;
    }
    final PartitionRouteInfo routeInfo = routeService.getRouteInfo(receiverResourceId);

    final String partitionLeader = routeInfo.leaderMemberId();
    final int partitionId = routeInfo.partitionId();
    LOG.info(
        "Sending command {} {} to partition {}, leader {}",
        lifeCycle.getValueType(),
        lifeCycle,
        partitionId,
        partitionLeader);

    final byte[] message =
        encode(
            operationReference == null ? -1 : operationReference,
            receiverResourceId,
            lifeCycle.getValueType(),
            lifeCycle,
            recordKey,
            valueMapper.copyValue(lifeCycle, command));

    communicationService.unicast(
        TOPIC_PREFIX + partitionId,
        message,
        DefaultSerializers.BASIC::encode,
        MemberId.from(partitionLeader),
        true);
  }

  void setCurrentLeader(
      final int partitionId,
      final int resourceId,
      final Set<Integer> agentResourceIds,
      final String currentLeader) {
    LOG.debug(
        "setCurrentLeader resourceId={} partitionId={} leader={}",
        resourceId,
        partitionId,
        currentLeader);
    for (final int agentResourceId : agentResourceIds) {
      routeService.add(agentResourceId, partitionId, currentLeader);
    }
    routeService.add(resourceId, partitionId, currentLeader);
    if (partitionId == CLUSTER_LEADER_PARTITION) {
      routeService.add(PARTITION_GLOBAL_SOURCE, partitionId, currentLeader);
      activitySources.add(PARTITION_GLOBAL_SOURCE);
    }
    activitySources.add(resourceId);
  }

  private byte[] encode(
      final long operationReference,
      final int receiverPartitionId,
      final ValueType valueType,
      final ValueLifeCycle intent,
      final Long recordKey,
      final BufferWriter command) {
    final int messageLength =
        MessageHeaderEncoder.ENCODED_LENGTH
            + InterPartitionMessageEncoder.BLOCK_LENGTH
            + InterPartitionMessageEncoder.commandHeaderLength()
            + command.getLength();

    final int commandLength = command.getLength();
    if (reusableCommandBytes.length < commandLength) {
      reusableCommandBytes = new byte[commandLength];
    }
    reusableCommandBuffer.wrap(reusableCommandBytes, 0, commandLength);
    final byte[] messageBytes = new byte[messageLength];
    reusableMessageBuffer.wrap(messageBytes);
    command.write(reusableCommandBuffer, 0);
    reusableBodyEncoder
        .wrapAndApplyHeader(reusableMessageBuffer, 0, reusableHeaderEncoder)
        .operationReference(operationReference)
        .receiverPartitionId(receiverPartitionId)
        .valueType(valueType.getValue())
        .valueState(intent.value())
        .putCommand(reusableCommandBuffer, 0, commandLength);

    reusableBodyEncoder.recordKey(
        recordKey != null ? recordKey : InterPartitionMessageEncoder.recordKeyNullValue());

    return messageBytes;
  }

  @Override
  public Set<Integer> getActivitySourceIds() {
    final Set<Integer> result = new HashSet<>(activitySources.size());
    activitySources.forEach(value -> result.add(value));
    return result;
  }
}
