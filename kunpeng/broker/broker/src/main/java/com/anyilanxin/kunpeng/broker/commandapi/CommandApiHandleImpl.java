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
package com.anyilanxin.kunpeng.broker.commandapi;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.client.business.TopicUtils;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.core.ApiRequestReader;
import com.anyilanxin.kunpeng.broker.client.core.ApiRequestReaderImpl;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseWriterImpl;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseWriter;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.common.VersionInfo;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.IdGenerator;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 业务面命令 API 处理句柄默认实现：注册命令处理器并静默关闭。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandApiHandleImpl implements CommandApiHandle, CloseableSilently {
  private static final String ERROR_MSG_MISSING_PARTITON_MAP =
      "Node already unsubscribed from partition %d, this can only happen when atomix does not cleanly remove its handlers.";

  private final MessagingService messagingService;
  private boolean isDiskSpaceAvailable = true;
  private final ConcurrencyControl actor;
  private final Int2ObjectHashMap<Long2ObjectHashMap<PartitionRequest>> partitionsRequestMap;
  // sink 滞后背压闸门（每分区一个；配置禁用或未创建时无条目=闸门不存在）
  private final Int2ObjectHashMap<SinkBackpressureGate> backpressureGates =
      new Int2ObjectHashMap<>();
  private final IdGenerator idGenerator;
  private static final Logger LOG = BrokerLoggers.CLUSTER_BUSINESS;
  private final RecordValueMapper valueMapper;
  // Parsed once at construction — VersionUtil.getVersion() is a stable string set at build time.
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
      final IdGenerator idGenerator,
      final ConcurrencyControl actor) {
    this.messagingService = messagingService;
    this.actor = actor;
    this.idGenerator = idGenerator;
    partitionsRequestMap = new Int2ObjectHashMap<>(2000, Hashing.DEFAULT_LOAD_FACTOR);
    valueMapper = DefaultRecordValueMapper.getInstance();
    brokerVersion = VersionInfo.parse(VersionUtil.getVersion());
  }

  @Override
  public void close() {
    // 先失败在途请求：注销处理器后响应无法送回，立即失败避免调用方悬挂到超时
    for (final int partitionId : partitionsRequestMap.keySet().toArray(new Integer[0])) {
      final var requestMap = partitionsRequestMap.get(partitionId);
      if (requestMap != null) {
        requestMap
            .values()
            .forEach(
                request ->
                    request
                        .responseFuture()
                        .completeExceptionally(
                            new IllegalStateException("Command API handle closed")));
      }
      removePartition(partitionId);
    }
  }

  private void removePartition(final int partitionId) {
    removeRequestHandlers(partitionId);
    backpressureGates.remove(partitionId);
    final var requestMap = partitionsRequestMap.remove(partitionId);
    if (requestMap != null) {
      requestMap.clear();
    }
  }

  private void removeRequestHandlers(final int partitionId) {
    final var topicName =
        TopicUtils.getTopicName(RecordType.COMMAND_API.value(), String.valueOf(partitionId));
    LOG.info("Unsubscribe from topic {}", topicName);
    messagingService.unregisterHandler(topicName);
  }

  private void addRequestHandlers(
      final RaftPartitionSource partitionSource, final EventLogWriter logStreamWriter) {
    final int partitionId = partitionSource.getPartitionId().id();
    final var topicName =
        TopicUtils.getTopicName(RecordType.COMMAND_API.value(), String.valueOf(partitionId));
    LOG.info("subscribe from topic {}", topicName);
    messagingService.registerHandler(
        topicName, (sender, request) -> handleRequest(request, partitionSource, logStreamWriter));
  }

  void registerHandlers(
      final RaftPartitionSource partitionSource,
      final EventLogWriter logStreamWriter,
      final SinkBackpressureGate backpressureGate) {
    actor.run(
        () -> {
          partitionsRequestMap.computeIfAbsent(
              partitionSource.getPartitionId().id(), _ -> new Long2ObjectHashMap<>());
          if (backpressureGate == null) {
            backpressureGates.remove(partitionSource.getPartitionId().id());
          } else {
            backpressureGates.put(partitionSource.getPartitionId().id(), backpressureGate);
          }
          addRequestHandlers(partitionSource, logStreamWriter);
        });
  }

  /** 绑定分区 sink 最低确认位 supplier（背压闸门信号源，repository 就绪后由 transition 装配） */
  void bindSinkPositionSupplier(final int partitionId, final LongSupplier lowestSinkPosition) {
    actor.run(
        () -> {
          final var gate = backpressureGates.get(partitionId);
          if (gate != null) {
            gate.bindSinkPositionSupplier(lowestSinkPosition);
          }
        });
  }

  private CompletableFuture<byte[]> handleRequest(
      final byte[] requestBytes,
      final RaftPartitionSource partitionSource,
      final EventLogWriter logStreamWriter) {
    final var completableFuture = new CompletableFuture<byte[]>();
    actor.run(
        () -> {
          if (!isDiskSpaceAvailable) {
            completableFuture.completeExceptionally(
                new RuntimeException("Broker is out of disk space"));
            return;
          }
          final long requestId = idGenerator.nextId();
          final var requestMap = partitionsRequestMap.get(partitionSource.getPartitionId().id());
          if (requestMap == null) {
            final var errorMsg =
                String.format(
                    ERROR_MSG_MISSING_PARTITON_MAP, partitionSource.getPartitionId().id());
            LOG.trace(errorMsg);
            completableFuture.completeExceptionally(new IllegalStateException(errorMsg));
            return;
          }

          // 统一响应格式解码
          reusableRequestBuffer.wrap(requestBytes);
          final ApiRequestReaderImpl requestReader = reusableRequestReader;
          requestReader.reset();
          requestReader.wrap(reusableRequestBuffer, 0, reusableRequestBuffer.capacity());

          requestMap.put(
              requestId,
              new PartitionRequest(
                  completableFuture, requestReader.requestType(), requestReader.valueType()));

          // sink 滞后背压闸门：只拦业务面 USER_COMMAND 入口（本方法即唯一入口），超限立即回 code=2 失败应答
          final var backpressureGate = backpressureGates.get(partitionSource.getPartitionId().id());
          if (backpressureGate != null && !backpressureGate.tryAdmit()) {
            errorResponse(
                BrokerResponseCodes.SINK_BACKPRESSURE,
                "Rejected by sink backpressure gate: sink export backlog reached the configured"
                    + " threshold, retry after the backlog drains",
                partitionSource,
                requestId);
            return;
          }

          // 数据写入日志中
          writeCommand(partitionSource, requestId, logStreamWriter, requestReader);
        });
    return completableFuture;
  }

  private void writeCommand(
      final RaftPartitionSource partitionSource,
      final long requestId,
      final EventLogWriter logStreamWriter,
      final ApiRequestReader requestReader) {

    final AppendEntry appendEntry;
    // 面别编号 → 业务面枚举（与 BusinessBrokerPlane 物化侧同一翻译惯例）
    final ValueType valueType = ValueType.valueOf((short) requestReader.valueType());
    final short lifeCycleValue = (short) requestReader.lifeCycle();
    final ValueLifeCycle lifeCycle =
        ValueLifeCycle.UNKNOWN.value() == lifeCycleValue
            ? ValueLifeCycle.UNKNOWN
            : ValueLifeCycle.fromProtocolValue(valueType, lifeCycleValue);
    final RecordMetadata metadata = new RecordMetadata();
    metadata.requestId(requestId);
    metadata.valueType(valueType);
    metadata.valueLifeCycle(lifeCycle);
    metadata.recordType(RecordType.get((short) requestReader.requestType()));
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
      if (result instanceof final AppendResult.Rejected rejected) {
        if (rejected.reason() == RejectionReason.FROZEN) {
          // 分区冻结（数据手术窗口）：可重试——RESUME 后自动恢复，与背压拒绝同类的暂时性失败
          errorResponse(
              BrokerResponseCodes.PARTITION_FROZEN,
              "Partition is frozen for data surgery; retry after the plan resumes the partition",
              partitionSource,
              requestId);
        } else {
          errorResponse(
              BrokerResponseCodes.APPEND_REJECTED,
              "Append of command to the event log was rejected by the log writer",
              partitionSource,
              requestId);
        }
      }
    } else {
      errorResponse(
          BrokerResponseCodes.APPEND_REJECTED,
          "Cannot append command to the event log: in-flight window is exhausted",
          partitionSource,
          requestId);
    }
  }

  /** 写入日志被拒（流控窗口耗尽/事件日志拒绝追加）时立即回失败应答： 走统一响应路径清理请求登记并让调用方拿到明确失败，而非悬挂至超时。 */
  void errorResponse(
      final int code,
      final String message,
      final RaftPartitionSource partitionSource,
      final long requestId) {
    final BrokerResponseWriter<UnifiedRecordValue> response =
        newResponse(null, requestId, partitionSource.getPartitionId().id());
    response.setError(code, message);
    sendResponse(response);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Response extends UnifiedRecordValue> BrokerResponseWriter<Response> newResponse(
      final CommandApiValueLifeCycle lifeCycle, final long requestId, final int partitionId) {
    // null lifeCycle = 错误响应（无生命状态）：封装层落 UNKNOWN，与旧版 writer 的默认行为一致
    return new BrokerResponseWriter<>(
        requestId,
        partitionId,
        lifeCycle == null ? -1 : lifeCycle.getValueType().getValue(),
        lifeCycle == null ? ValueLifeCycle.UNKNOWN.value() : lifeCycle.value());
  }

  @Override
  public <Response extends UnifiedRecordValue> void sendResponse(
      final BrokerResponseWriter<Response> response) {
    actor.run(
        () -> {
          final int partitionId = response.getPartitionId();
          final long requestId = response.getRequestId();
          final var requestMap = partitionsRequestMap.get(partitionId);
          if (requestMap == null) {
            LOG.warn(
                "Node is no longer leader for partition {}, tried to respond on request with id {}",
                partitionId,
                requestId);
            return;
          }
          final var partitionRequest = requestMap.remove(requestId);
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

  public void unregisterHandlers(final RaftPartitionSource partitionSource) {
    removeRequestHandlers(partitionSource.getPartitionId().id());
  }

  void onDiskSpaceNotAvailable() {
    isDiskSpaceAvailable = false;
    LOG.debug("Broker is out of disk space. All client requests will be rejected");
  }

  void onDiskSpaceAvailable() {
    isDiskSpaceAvailable = true;
  }

  record PartitionRequest(
      CompletableFuture<byte[]> responseFuture, int requestType, int valueType) {}
}
