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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_PARTITION_SOURCE;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorService;
import com.anyilanxin.kunpeng.broker.client.admin.TopicUtils;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.AdminCommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.core.ApiRequestReader;
import com.anyilanxin.kunpeng.broker.client.core.ApiRequestReaderImpl;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseWriterImpl;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseWriter;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.RecordAppendEntryFactory;
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
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Hashing;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 管理面命令 API 处理句柄默认实现：注册命令处理器并静默关闭。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdminCommandApiHandleImpl implements AdminCommandApiHandle, CloseableSilently {
  /** 日志写入被拒（流控窗口耗尽/事件日志拒绝追加）的错误码 */
  private static final int ERROR_CODE_APPEND_REJECTED = 1;

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

  public AdminCommandApiHandleImpl(
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
    // 先失败在途请求：注销处理器后响应无法送回，立即失败避免调用方悬挂到超时
    partitionsRequestMap
        .values()
        .forEach(
            request ->
                request
                    .responseFuture()
                    .completeExceptionally(new IllegalStateException("Command API handle closed")));
    partitionsRequestMap.clear();
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
    // 面别编号 → 管理面枚举（与 AdminBrokerPlane 物化侧同一翻译惯例）
    final AdminValueType valueType = AdminValueType.valueOf((short) requestReader.valueType());
    final short lifeCycleValue = (short) requestReader.lifeCycle();
    final AdminValueLifeCycle lifeCycle =
        AdminValueLifeCycle.UNKNOWN.value() == lifeCycleValue
            ? AdminValueLifeCycle.UNKNOWN
            : AdminValueLifeCycle.fromProtocolValue(valueType, lifeCycleValue);
    final AdminRecordMetadata metadata = new AdminRecordMetadata();
    metadata.requestId(requestId);
    metadata.valueType(valueType);
    metadata.valueLifeCycle(lifeCycle);
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
        errorResponse(
            ERROR_CODE_APPEND_REJECTED,
            "Append of command to the event log was rejected by the log writer",
            requestId);
      }
    } else {
      errorResponse(
          ERROR_CODE_APPEND_REJECTED,
          "Cannot append command to the event log: in-flight window is exhausted",
          requestId);
    }
  }

  /** 写入日志被拒（流控窗口耗尽/事件日志拒绝追加）时立即回失败应答： 走统一响应路径清理请求登记并让调用方拿到明确失败，而非悬挂至超时。 */
  void errorResponse(final int code, final String message, final long requestId) {
    final BrokerResponseWriter<UnifiedRecordValue> response = newResponse(null, requestId);
    response.setError(code, message);
    sendResponse(response);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Response extends UnifiedRecordValue> BrokerResponseWriter<Response> newResponse(
      final CommandApiValueLifeCycle lifeCycle, final long requestId) {
    // null lifeCycle = 错误响应（无生命状态）：封装层落 UNKNOWN，与旧版 writer 的默认行为一致
    return new BrokerResponseWriter<>(
        requestId,
        ADMIN_PARTITION_SOURCE,
        lifeCycle == null ? -1 : lifeCycle.getValueType().getValue(),
        lifeCycle == null ? AdminValueLifeCycle.UNKNOWN.value() : lifeCycle.value());
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
            apiResponseWriter.requestType(partitionRequest.requestType);
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
      CompletableFuture<byte[]> responseFuture, int requestType, int valueType) {}
}
