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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.business.TopicUtils;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.broker.client.core.ApiRequestWriterImpl;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReaderImpl;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 命令入口句柄在分区退位时的在途请求处理单测。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class CommandApiHandleImplTest {
  private static final int PARTITION_ID = 1;
  private static final PartitionId PARTITION = PartitionId.from("business", PARTITION_ID);
  private static final String TOPIC =
      TopicUtils.getTopicName(RecordType.COMMAND_API.value(), String.valueOf(PARTITION_ID));

  private final MessagingService messagingService = mock(MessagingService.class);
  private final EventLogWriter logStreamWriter = mock(EventLogWriter.class);
  private final RaftPartitionSource partitionSource = mock(RaftPartitionSource.class);
  private final ConcurrencyControl actor = mock(ConcurrencyControl.class);
  private final AtomicInteger requestIds = new AtomicInteger();
  @SuppressWarnings("unchecked")
  private final AtomicReference<BiFunction<Address, byte[], CompletableFuture<byte[]>>> handler =
      new AtomicReference<>();

  private CommandApiHandleImpl handle;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    // 被测逻辑全部跑在 actor 任务里：让任务就地执行，测试线程即 actor 线程
    doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return null;
            })
        .when(actor)
        .run(any(Runnable.class));
    when(partitionSource.getPartitionId()).thenReturn(PARTITION);
    when(logStreamWriter.canAppend(any(WriteContext.class), anyInt(), anyInt())).thenReturn(true);
    when(logStreamWriter.tryAppend(any(WriteContext.class), any(AppendEntry.class)))
        .thenReturn(new AppendResult.Appended(1L, 1L));
    doAnswer(
            invocation -> {
              handler.set(invocation.getArgument(1));
              return null;
            })
        .when(messagingService)
        .registerHandler(anyString(), any(BiFunction.class));

    handle = new CommandApiHandleImpl(messagingService, requestIds::incrementAndGet, actor);
    handle.registerHandlers(partitionSource, logStreamWriter, null);
  }

  @Test
  void shouldFailPendingRequestsWhenLeadershipIsLost() {
    final var first = submitCommand();
    final var second = submitCommand();
    assertThat(first.isDone()).isFalse();
    assertThat(second.isDone()).isFalse();

    handle.unregisterHandlers(partitionSource);

    assertLeaderSteppedDown(first);
    assertLeaderSteppedDown(second);
    verify(messagingService).unregisterHandler(TOPIC);
  }

  @Test
  void shouldFailPendingRequestsBeforeTheHandlerIsRemoved() {
    final var pending = submitCommand();
    // 处理器注销之后应答送不回调用方：注销那一刻在途请求必须已经失败
    final var failedWhenUnregistered = new AtomicBoolean();
    doAnswer(
            invocation -> {
              failedWhenUnregistered.set(pending.isDone());
              return null;
            })
        .when(messagingService)
        .unregisterHandler(TOPIC);

    handle.unregisterHandlers(partitionSource);

    assertThat(failedWhenUnregistered).isTrue();
  }

  @Test
  void shouldOnlyUnregisterTheHandlerWhenNothingIsPending() {
    handle.unregisterHandlers(partitionSource);

    verify(messagingService).unregisterHandler(TOPIC);
  }

  @Test
  void shouldDropResponsesArrivingAfterTheRequestWasFailed() {
    final var pending = submitCommand();
    handle.unregisterHandlers(partitionSource);

    // 引擎迟到的应答（另一个错误码）不得覆盖已经送出的 leader 变更失败
    handle.errorResponse(
        BrokerResponseCodes.APPEND_REJECTED, "late response", partitionSource, 1L);

    assertLeaderSteppedDown(pending);
  }

  @Test
  void shouldRejectNewCommandsWithNotLeaderWhileLeadershipIsTransferring() {
    handle.setLeadershipTransferring(PARTITION_ID, true);

    final var response = submitCommand();

    // 命令没有进日志：网关据此退避后重新寻址再发
    assertError(response, BrokerResponseCodes.NOT_LEADER, "transferring leadership");
    verify(logStreamWriter, never()).tryAppend(any(WriteContext.class), any(AppendEntry.class));
  }

  @Test
  void shouldAcceptCommandsAgainOnceTheTransferWindowEnds() {
    handle.setLeadershipTransferring(PARTITION_ID, true);
    handle.setLeadershipTransferring(PARTITION_ID, false);

    final var response = submitCommand();

    assertThat(response.isDone()).isFalse();
    verify(logStreamWriter).tryAppend(eq(WriteContext.USER_COMMAND), any(AppendEntry.class));
  }

  @Test
  void shouldClearTheTransferWindowWhenLeadershipIsLost() {
    handle.setLeadershipTransferring(PARTITION_ID, true);
    handle.unregisterHandlers(partitionSource);

    // 下一任期重新登记处理器：转移标记不能留过来
    handle.registerHandlers(partitionSource, logStreamWriter, null);
    final var response = submitCommand();

    assertThat(response.isDone()).isFalse();
    verify(logStreamWriter).tryAppend(eq(WriteContext.USER_COMMAND), any(AppendEntry.class));
  }

  /** 按网关的编码方式构造一条创建流程实例命令帧并交给已登记的处理器 */
  private CompletableFuture<byte[]> submitCommand() {
    final var request =
        new ProcessInstanceCreateRequest()
            .setProcessDefinitionId(123L)
            .setTenantId("")
            .setVariable(Map.of());
    final var value = request.getValue();
    final var valueBuffer = new UnsafeBuffer(new byte[value.getLength()]);
    value.write(valueBuffer, 0);
    final var writer =
        new ApiRequestWriterImpl()
            .requestType(request.requestType())
            .key(request.key())
            .valueType(request.valueType())
            .lifeCycle(request.lifeCycle());
    writer.data(valueBuffer);
    final var frame = new UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(frame, 0);
    return handler.get().apply(null, frame.byteArray());
  }

  private static void assertLeaderSteppedDown(final CompletableFuture<byte[]> response) {
    assertError(response, BrokerResponseCodes.LEADER_STEPPED_DOWN, "may have been applied");
  }

  private static void assertError(
      final CompletableFuture<byte[]> response, final int code, final String messagePart) {
    assertThat(response.isDone()).isTrue();
    final byte[] bytes = response.join();
    final var reader = new ApiResponseReaderImpl();
    reader.wrap(new UnsafeBuffer(bytes), 0, bytes.length);
    assertThat(reader.success()).isFalse();
    assertThat(reader.code()).isEqualTo(code);
    assertThat(reader.message()).contains(messagePart);
  }
}
