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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_GROUP;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.INNER_PARTITION_TOPIC_PREFIX;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.protocol.InterPartitionMessageEncoder;
import com.anyilanxin.kunpeng.broker.protocol.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.utils.serializer.serializers.DefaultSerializers;
import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.time.Duration;
import org.agrona.collections.IntHashSet;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 跨分区命令发送服务：每条命令都要等到目标分区确认已写入其日志。
 *
 * <p>目标分区明确没收下（磁盘不可用、日志拒绝写入），或者命令根本没发到 leader 那里（对端已不是 leader、解析不出
 * leader）时，命令确定不在日志里：在交付预算内退避重发，每次重发都重新寻址。等不到确认的命令可能已经写入，不重发——是否重发由上层的分发协议决定。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InterPartitionCommandSenderService extends Actor
    implements InterPartitionCommandSender {

  private static final Logger LOG = BrokerLoggers.CLUSTER_BUSINESS;

  /** 等待确认的命令数上限（含退避中等着重发的）：每条都持有编码后的完整命令，目标分区长时间拒收时不设上限会耗尽内存 */
  private static final int MAX_PENDING_COMMANDS = 10_000;

  /** 一条命令从首次发出到最后一次重发的总预算 */
  private static final Duration DELIVERY_BUDGET = Duration.ofSeconds(30);

  /** 等目标分区确认的时限 */
  private static final Duration ACK_TIMEOUT = Duration.ofSeconds(5);

  private static final Duration RETRY_DELAY_MIN = Duration.ofMillis(200);
  private static final Duration RETRY_DELAY_MAX = Duration.ofSeconds(2);

  /** 退避翻倍的次数上限：下限的 2 的 4 次方倍已越过上限，再多移位没有意义 */
  private static final int RETRY_BACKOFF_DOUBLINGS = 4;

  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();
  private final ClusterCommunicationService communicationService;
  private final ClusterTopologyService topologyService;
  private final MessageHeaderEncoder reusableHeaderEncoder = new MessageHeaderEncoder();
  private final InterPartitionMessageEncoder reusableBodyEncoder =
      new InterPartitionMessageEncoder();
  private final UnsafeBuffer reusableCommandBuffer = new UnsafeBuffer(0, 0);
  private final UnsafeBuffer reusableMessageBuffer = new UnsafeBuffer(0, 0);
  private byte[] reusableCommandBytes = new byte[0];

  /** 等待确认的命令数，只在 actor 线程上读写 */
  private int pendingCommandCount;

  public InterPartitionCommandSenderService(
      final ClusterCommunicationService communicationService,
      final ClusterTopologyService topologyService) {
    this.communicationService = communicationService;
    this.topologyService = topologyService;
  }

  /** 一条等待目标分区确认的命令：各次重发共用同一份编码结果与截止预算，只有重发序号在变。 */
  private record PendingCommand(
      int receiverSourceId,
      ValueLifeCycle lifeCycle,
      byte[] message,
      long deadlineNs,
      int attempt) {

    PendingCommand nextAttempt() {
      return new PendingCommand(receiverSourceId, lifeCycle, message, deadlineNs, attempt + 1);
    }
  }

  @Override
  public void sendCommand(
      final int receiverPartitionId,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue command) {
    submit(receiverPartitionId, lifeCycle, null, null, command);
  }

  @Override
  public void sendCommand(
      final int receiverPartitionId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final UnifiedRecordValue command) {
    submit(receiverPartitionId, lifeCycle, recordKey, null, command);
  }

  @Override
  public void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command) {
    submit(receiverResourceId, lifeCycle, recordKey, operationReference, command);
  }

  /** 命令在 actor 线程上编码（编码缓冲区不是线程安全的）；等待数已满的命令直接丢弃，留给上层的分发协议重发。 */
  private void submit(
      final int receiverSourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command) {
    actor.submit(
        () -> {
          if (pendingCommandCount >= MAX_PENDING_COMMANDS) {
            LOG.error(
                "Dropping command {} {} to partition of source {}: {} commands are already awaiting acknowledgement",
                lifeCycle.getValueType(),
                lifeCycle,
                receiverSourceId,
                pendingCommandCount);
            return;
          }
          pendingCommandCount++;
          deliver(
              new PendingCommand(
                  receiverSourceId,
                  lifeCycle,
                  encode(receiverSourceId, lifeCycle, recordKey, operationReference, command),
                  System.nanoTime() + DELIVERY_BUDGET.toNanos(),
                  0));
        });
  }

  /** 向目标分区当前的 leader 发出一次交付；每次重发都重新寻址（leader 可能已经切换）。 */
  private void deliver(final PendingCommand pending) {
    final PartitionId partitionId =
        topologyService.getPartitionBySourceId(pending.receiverSourceId());
    final MemberId leader =
        partitionId == null ? null : topologyService.getPartitionLeader(partitionId);
    if (leader == null) {
      retryOrFail(pending, "no leader known for its partition");
      return;
    }
    LOG.info(
        "Sending command {} {} to partition {}, leader {}",
        pending.lifeCycle().getValueType(),
        pending.lifeCycle(),
        partitionId,
        leader);
    // 请求沿用原有的序列化方式（滚动升级期间新旧版本互通）；应答是新增的，只带一个字节的状态码
    communicationService
        .send(
            INNER_PARTITION_TOPIC_PREFIX + partitionId,
            pending.message(),
            DefaultSerializers.BASIC::encode,
            InterPartitionDeliveryStatus::fromBytes,
            leader,
            ACK_TIMEOUT)
        .whenComplete((status, error) -> actor.run(() -> onDeliveryResult(pending, status, error)));
  }

  /** 目标分区的应答（actor 线程）。已写入即完结；明确没写入的退避重发；其余结果（等确认超时、处理器异常、发送失败）命令可能已写入，不重发。 */
  private void onDeliveryResult(
      final PendingCommand pending,
      final InterPartitionDeliveryStatus status,
      final Throwable error) {
    if (error == null) {
      if (status == InterPartitionDeliveryStatus.APPENDED) {
        pendingCommandCount--;
      } else {
        retryOrFail(pending, "rejected by the target partition");
      }
      return;
    }
    // 对端节点上没有本分区的命令处理器（它已退位或尚未就任 leader），或者它已不在成员列表里：命令没有送达
    if (error instanceof MessagingException.NoRemoteHandler
        || error instanceof MessagingException.NoSuchMemberException) {
      retryOrFail(pending, error.toString());
      return;
    }
    pendingCommandCount--;
    LOG.info(
        "Not resending command {} {} to partition of source {}: {}",
        pending.lifeCycle().getValueType(),
        pending.lifeCycle(),
        pending.receiverSourceId(),
        error);
  }

  /** 明确没写入的命令退避后重发；预算内排不进下一次重发就放弃，留给上层的分发协议重发。 */
  private void retryOrFail(final PendingCommand pending, final String failure) {
    final PendingCommand next = pending.nextAttempt();
    final Duration delay = retryDelay(pending.attempt());
    if (System.nanoTime() + delay.toNanos() >= next.deadlineNs()) {
      pendingCommandCount--;
      LOG.warn(
          "Giving up command {} {} to partition of source {} after {} attempt(s): {}",
          next.lifeCycle().getValueType(),
          next.lifeCycle(),
          next.receiverSourceId(),
          next.attempt(),
          failure);
      return;
    }
    if (pending.attempt() == 0) {
      LOG.warn(
          "Command {} {} to partition of source {} was not appended ({}), resending",
          pending.lifeCycle().getValueType(),
          pending.lifeCycle(),
          pending.receiverSourceId(),
          failure);
    }
    actor.schedule(delay, () -> deliver(next));
  }

  /** 第 {@code attempt} 次重发前的退避：200 毫秒起每次翻倍，封顶 2 秒。 */
  static Duration retryDelay(final int attempt) {
    return Duration.ofMillis(
        Math.min(
            RETRY_DELAY_MIN.toMillis() << Math.min(attempt, RETRY_BACKOFF_DOUBLINGS),
            RETRY_DELAY_MAX.toMillis()));
  }

  @Override
  public IntHashSet getActivitySourceIds() {
    return topologyService.getActivitySourceIds();
  }

  @Override
  public IntHashSet getActivityPartitionIds() {
    return topologyService.getActivityPartitionIds();
  }

  @Override
  public int getPartitionSource(final int partitionId) {
    return topologyService.getPartitionSource(PartitionId.from(BUSINESS_RAFT_GROUP, partitionId));
  }

  /** 命令值在此拷贝进消息，编码结果可以在重发之间复用。 */
  private byte[] encode(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command) {
    return encode(
        operationReference == null ? -1 : operationReference,
        receiverResourceId,
        lifeCycle.getValueType(),
        lifeCycle,
        recordKey,
        valueMapper.copyValue(lifeCycle, command));
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
}
