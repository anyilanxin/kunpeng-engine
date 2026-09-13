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
package com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorService;
import com.anyilanxin.kunpeng.broker.client.admin.*;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.common.VersionInfo;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import java.util.concurrent.CompletableFuture;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Hashing;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class CommandApiHandleImpl implements CommandApiHandle, CloseableSilently {
  private final MessagingService messagingService;
  private final boolean isDiskSpaceAvailable = true;
  private final ConcurrencyControl actor;
  private final Long2ObjectHashMap<PartitionRequest> partitionsRequestMap;
  private final NodeIdGeneratorService idGenerator;
  private static final Logger LOG = BrokerLoggers.BROKER_LOGGER;
  private final RecordValueMapper valueMapper;
  // Parsed once at construction — brokerInfo.getVersion() is a stable string set at broker startup.
  private final VersionInfo brokerVersion;
  // Pooled per-call helpers — safe because handleRequest runs inside actor.run(...) (single
  // threaded), and the reader's data buffer is fully consumed by logStreamWriter.tryWrite before
  // the actor task returns.
  private final ApiRequestReaderImpl reusableRequestReader = new ApiRequestReaderImpl();
  private final UnsafeBuffer reusableRequestBuffer = new UnsafeBuffer(0, 0);
  // Pooled response-side helpers — safe for the same reason (sendResponse runs on the same actor).
  // Only the byte[] returned to the client future is allocated fresh per call.
  private final ApiResponseWriterImpl reusableResponseWriter = new ApiResponseWriterImpl();
  private final UnsafeBuffer reusableResponseDataBuffer = new UnsafeBuffer(0, 0);
  private byte[] reusableResponseDataBytes = new byte[0];

  public CommandApiHandleImpl(
      final MessagingService messagingService,
      final NodeIdGeneratorService idGenerator,
      final ConcurrencyControl actor) {
    this.messagingService = messagingService;
    this.actor = actor;
    this.idGenerator = idGenerator;
    partitionsRequestMap = new Long2ObjectHashMap<>(100, Hashing.DEFAULT_LOAD_FACTOR);
    valueMapper = DefaultRecordValueMapper.getInstance();
    brokerVersion = VersionInfo.parse(VersionUtil.getVersion());
  }

  @Override
  public void close() {
    removeRequestHandlers();
  }

  private void removeRequestHandlers() {
    final var topicName = TopicUtils.getTopicName();
    LOG.info("Unsubscribe from topic {}", topicName);
    messagingService.unregisterHandler(topicName);
  }

  private void addRequestHandlers(final EventLogWriter logStreamWriter) {
    final var topicName = TopicUtils.getTopicName();
    LOG.info("subscribe from topic {}", topicName);
    messagingService.registerHandler(
        topicName, (sender, request) -> handleRequest(request, logStreamWriter));
  }

  void registerHandlers(final EventLogWriter logStreamWriter) {
    actor.run(() -> addRequestHandlers(logStreamWriter));
  }

  private CompletableFuture<byte[]> handleRequest(
      final byte[] requestBytes, final EventLogWriter logStreamWriter) {
    final var completableFuture = new CompletableFuture<byte[]>();
    actor.run(
        () -> {
          if (!isDiskSpaceAvailable) {
            completableFuture.completeExceptionally(
                new RuntimeException("Broker is out of disk space"));
            return;
          }
          final long requestId = idGenerator.nextId();
          // 统一响应格式解码
          reusableRequestBuffer.wrap(requestBytes);
          final ApiRequestReaderImpl requestReader = reusableRequestReader;
          requestReader.reset();
          requestReader.wrap(reusableRequestBuffer, 0, reusableRequestBuffer.capacity());

          partitionsRequestMap.put(
              requestId,
              new PartitionRequest(
                  completableFuture, requestReader.requestType(), requestReader.valueType()));

          // 数据写入日志中
          writeCommand(requestId, logStreamWriter, requestReader);
        });
    return completableFuture;
  }

  private void writeCommand(
      final long requestId,
      final EventLogWriter logStreamWriter,
      final ApiRequestReader requestReader) {

    final AppendEntry appendEntry;
    final AdminRecordMetadata metadata = new AdminRecordMetadata();
    metadata.requestId(requestId);
    metadata.valueType(requestReader.valueType());
    metadata.valueLifeCycle(requestReader.lifeCycle());
    //    metadata.recordType(requestReader.requestType());
    metadata.recordType(RecordType.COMMAND_API);
    metadata.brokerVersion(brokerVersion);
    final long key = requestReader.key();
    final UnifiedRecordValue recordValue = valueMapper.getValue(metadata.getLifeCycle());
    recordValue.wrap(requestReader.data());
    if (key != -1) {
      appendEntry = RecordAppendEntryFactory.of(key, metadata, recordValue);
    } else {
      appendEntry = RecordAppendEntryFactory.of(metadata, recordValue);
    }
    if (logStreamWriter.canAppend(1, appendEntry.getLength())) {
      final AppendResult result = logStreamWriter.tryAppend(WriteContext.USER_COMMAND, appendEntry);
      if (result instanceof AppendResult.Rejected) {
        errorResponse(0, null, requestId);
      }
    } else {
      errorResponse(0, null, requestId);
    }
  }

  void errorResponse(
      final int code, final long key, final DirectBuffer directBuffer, final long requestId) {
    actor.run(
        () -> {
          //        final ApiResponseWriter response = new ApiResponseWriterImpl();
          //        response.key(key);
          //        response.requestId(requestId);
          //        response.dataType(DataType.USERINFO);
          //        sendResponse(response);
        });
  }

  void errorResponse(final int code, final DirectBuffer directBuffer, final long requestId) {
    errorResponse(code, 0L, directBuffer, requestId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Response extends UnifiedRecordValue> BrokerResponseWriter<Response> newResponse(
      final CommandApiValueLifeCycle lifeCycle, final long requestId) {
    return new BrokerResponseWriter<>(requestId, lifeCycle);
  }

  @Override
  public <Response extends UnifiedRecordValue> void sendResponse(
      final BrokerResponseWriter<Response> response) {
    actor.run(
        () -> {
          final long requestId = response.getRequestId();
          final var partitionRequest = partitionsRequestMap.remove(requestId);
          if (partitionRequest != null) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("Send response to request {}", requestId);
            }
            // 统一响应
            final ApiResponseWriterImpl apiResponseWriter = reusableResponseWriter.reset();
            //          apiResponseWriter.requestType(partitionRequest.requestType);
            apiResponseWriter.valueType(partitionRequest.valueType);
            apiResponseWriter.lifeCycle(response.getLifeCycle());
            // 数据
            if (response.isSuccess()) {
              final UnifiedRecordValue value = response.getResponse();
              final int valueLength = value.getLength();
              if (reusableResponseDataBytes.length < valueLength) {
                reusableResponseDataBytes = new byte[valueLength];
              }
              reusableResponseDataBuffer.wrap(reusableResponseDataBytes, 0, valueLength);
              value.write(reusableResponseDataBuffer, 0);
              apiResponseWriter.data(reusableResponseDataBuffer);
              apiResponseWriter.success();
            } else {
              apiResponseWriter.fail(response.getCode(), response.getMessage());
            }
            // 统一请求格式编码 byte[]
            final MutableDirectBuffer responseDataBuffer =
                new UnsafeBuffer(new byte[apiResponseWriter.getLength()]);
            apiResponseWriter.write(responseDataBuffer, 0);
            partitionRequest.responseFuture.complete(responseDataBuffer.byteArray());
          } else if (LOG.isTraceEnabled()) {
            LOG.trace("Wasn't able to send response to request {}", requestId);
          }
        });
  }

  public void unregisterHandlers() {
    removeRequestHandlers();
  }

  record PartitionRequest(
      CompletableFuture<byte[]> responseFuture, RecordType requestType, AdminValueType valueType) {}
}
