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

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.protocol.InterPartitionMessageDecoder;
import com.anyilanxin.kunpeng.broker.protocol.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.Optional;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 跨分区命令接收器实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class InterPartitionCommandReceiverImpl {
  private static final Logger LOG = BrokerLoggers.TRANSPORT_LOGGER;
  private final Decoder decoder = new Decoder();
  private final EventLogWriter logStreamWriter;
  private boolean diskSpaceAvailable = true;
  private final RecordValueMapper valueMapper;

  InterPartitionCommandReceiverImpl(
      final EventLogWriter logStreamWriter, final RecordValueMapper valueMapper) {
    this.logStreamWriter = logStreamWriter;
    this.valueMapper = valueMapper;
  }

  void handleMessage(final MemberId memberId, final byte[] message) {
    LOG.trace("Received message from {}", memberId);

    final var decoded = decoder.decodeMessage(message, valueMapper);

    if (!diskSpaceAvailable) {
      LOG.warn(
          "Ignoring command {} {} from {}, no disk space available",
          decoded.metadata.getValueType(),
          decoded.metadata.getLifeCycle(),
          memberId);
      return;
    }

    final AppendResult result = writeCommand(decoded);
    if (result instanceof AppendResult.Rejected rejected) {
      logWriteFailure(memberId, decoded, rejected);
    }
  }

  private void logWriteFailure(
      final MemberId memberId, final DecodedMessage decoded, final AppendResult.Rejected failure) {
    LOG.warn(
        "Failed to write command {} {} from {} to logstream (error = {})",
        decoded.metadata.getValueType(),
        decoded.metadata.getLifeCycle(),
        memberId,
        failure);
  }

  private AppendResult writeCommand(final DecodedMessage decoded) {
    final var appendEntry =
        decoded
            .recordKey()
            .map(key -> RecordAppendEntryFactory.of(key, decoded.metadata(), decoded.command()))
            .orElseGet(() -> RecordAppendEntryFactory.of(decoded.metadata(), decoded.command()));

    return logStreamWriter.tryAppend(WriteContext.INTER_PARTITION, appendEntry);
  }

  void setDiskSpaceAvailable(final boolean available) {
    diskSpaceAvailable = available;
  }

  private record DecodedMessage(
      Optional<Long> recordKey, RecordMetadata metadata, UnifiedRecordValue command) {}

  private static final class Decoder {
    private final InterPartitionMessageDecoder messageDecoder = new InterPartitionMessageDecoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    DecodedMessage decodeMessage(final byte[] message, final RecordValueMapper valueMapper) {
      final var messageBuffer = new UnsafeBuffer();
      final var recordMetadata = new RecordMetadata();

      messageBuffer.wrap(message);
      messageDecoder.wrapAndApplyHeader(messageBuffer, 0, headerDecoder);

      final var operationReference = messageDecoder.operationReference();
      Optional<Long> recordKey = Optional.empty();
      if (messageDecoder.recordKey() != InterPartitionMessageDecoder.recordKeyNullValue()) {
        recordKey = Optional.of(messageDecoder.recordKey());
      }

      final var valueType = ValueType.valueOf((short) messageDecoder.valueType());
      final var lifeCycle =
          ValueLifeCycle.fromProtocolValue(valueType, (short) messageDecoder.valueState());

      // rebuild the record metadata first, all messages must contain commands
      recordMetadata
          .reset()
          .recordType(RecordType.COMMAND)
          .valueType(valueType)
          .valueLifeCycle(lifeCycle)
          .operationReference(operationReference);

      // wrap the command buffer around the rest of the message
      // this does not try to parse the command, we are just assuming that these bytes
      // are a valid command
      final var commandOffset =
          messageDecoder.limit() + InterPartitionMessageDecoder.commandHeaderLength();
      final var commandLength = messageDecoder.commandLength();

      final UnifiedRecordValue recordValue = valueMapper.getValue(lifeCycle);
      if (recordValue == null) {
        throw new IllegalArgumentException(
            "No value type mapped to %s, can't decode message".formatted(valueType));
      }
      recordValue.wrap(messageBuffer, commandOffset, commandLength);
      return new DecodedMessage(recordKey, recordMetadata, recordValue);
    }
  }
}
