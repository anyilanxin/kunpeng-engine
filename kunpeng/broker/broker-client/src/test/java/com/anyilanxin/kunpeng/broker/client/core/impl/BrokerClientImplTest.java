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
package com.anyilanxin.kunpeng.broker.client.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseWriterImpl;
import com.anyilanxin.kunpeng.broker.client.core.BrokerException;
import com.anyilanxin.kunpeng.broker.client.core.BrokerPlane;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.broker.client.core.LeaderChangedException;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.PartitionLeaderInfo;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 网关到 broker 请求管线的重试条件、退避与在途请求终止单测：只重试"命令确定没有进入日志"的失败，leader 变更后立即终止在途请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Timeout(30)
class BrokerClientImplTest {
  private static final PartitionId PARTITION = PartitionId.from("raft-partition", 1);
  private static final MemberId LEADER = MemberId.from("member-1");
  private static final MemberId SUCCESSOR = MemberId.from("member-2");
  private static final long TERM = 2;

  private final MessagingService messagingService = mock(MessagingService.class);
  private final ClusterEventService eventService = mock(ClusterEventService.class);
  private final BrokerPlane plane = mock(BrokerPlane.class);
  private final ClusterTopologyService topologyService = mock(ClusterTopologyService.class);
  /** 拓扑服务登记的视图变化通知 */
  private final AtomicReference<Runnable> topologyChange = new AtomicReference<>();

  private ActorScheduler scheduler;
  private BrokerClientImpl client;

  @BeforeEach
  void setUp() throws Exception {
    when(plane.resolve(any())).thenReturn(destination(LEADER, TERM));
    doAnswer(
            invocation -> {
              topologyChange.set(invocation.getArgument(0));
              return null;
            })
        .when(topologyService)
        .addTopologyChangeListener(any());
    scheduler = ActorScheduler.newDefaultActorScheduler();
    scheduler.start();
    client = new BrokerClientImpl(messagingService, eventService, plane, topologyService);
    scheduler.submitActor(client).get(10, TimeUnit.SECONDS);
  }

  @AfterEach
  void tearDown() {
    client.close();
    scheduler.close();
  }

  @Test
  void shouldRetryWhenTheTargetHasNoCommandHandler() throws Exception {
    sendFailsThenSucceeds(
        CompletableFuture.failedFuture(new MessagingException.NoRemoteHandler("topic")));

    final BrokerResponse<?> response = client.sendRequestWithRetry(request()).get(10, TimeUnit.SECONDS);

    assertThat(response.isSuccess()).isTrue();
    verify(plane, times(2)).resolve(any());
    verify(messagingService, times(2)).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldRetryWhenThePartitionLeaderIsUnresolvable() throws Exception {
    when(plane.resolve(any()))
        .thenThrow(new BrokerException(BrokerException.LEADER_UNRESOLVABLE, "no leader"))
        .thenReturn(destination(LEADER, TERM));
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(CompletableFuture.completedFuture(success()));

    final BrokerResponse<?> response = client.sendRequestWithRetry(request()).get(10, TimeUnit.SECONDS);

    assertThat(response.isSuccess()).isTrue();
    // 第一次寻址就失败，命令没有发出去：重新寻址后只真正发送了一次
    verify(plane, times(2)).resolve(any());
    verify(messagingService, times(1))
        .sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldRetryWhenRejectedByTheLeadershipTransferWindow() throws Exception {
    sendFailsThenSucceeds(CompletableFuture.completedFuture(error(BrokerResponseCodes.NOT_LEADER)));

    final BrokerResponse<?> response = client.sendRequestWithRetry(request()).get(10, TimeUnit.SECONDS);

    assertThat(response.isSuccess()).isTrue();
    verify(messagingService, times(2)).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldNotRetryWhenTheReplyTimesOut() {
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(CompletableFuture.failedFuture(new TimeoutException("reply timeout")));

    assertFailsWith(client.sendRequestWithRetry(request()), TimeoutException.class);

    verify(messagingService, times(1)).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldNotRetryARequestTerminatedByLeadershipChange() throws Exception {
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(CompletableFuture.completedFuture(error(BrokerResponseCodes.LEADER_STEPPED_DOWN)));

    final BrokerResponse<?> response = client.sendRequestWithRetry(request()).get(10, TimeUnit.SECONDS);

    assertThat(response.isError()).isTrue();
    assertThat(response.code()).isEqualTo(BrokerResponseCodes.LEADER_STEPPED_DOWN);
    verify(messagingService, times(1)).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldNotRetryAnUnaddressableRequest() throws Exception {
    when(plane.resolve(any())).thenThrow(new BrokerException(10, "Invalid key"));

    assertFailsWith(client.sendRequestWithRetry(request()), BrokerException.class);

    verify(messagingService, never()).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldNotRetryWhenRetriesAreNotRequested() {
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(CompletableFuture.failedFuture(new MessagingException.NoRemoteHandler("topic")));

    assertFailsWith(client.sendRequest(request()), MessagingException.NoRemoteHandler.class);

    verify(messagingService, times(1)).sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
  }

  @Test
  void shouldGiveUpWhenTheRetryBudgetIsExhausted() throws Exception {
    when(plane.resolve(any()))
        .thenThrow(new BrokerException(BrokerException.LEADER_UNRESOLVABLE, "no leader"));

    assertFailsWith(
        client.sendRequestWithRetry(request(), Duration.ofMillis(600)), TimeoutException.class);

    // 预算内反复重试（20ms 起退避），直到预算耗尽：至少完成三次重试
    verify(plane, atLeast(4)).resolve(any());
  }

  @Test
  void shouldBackOffExponentiallyUpToTheCap() {
    assertThat(BrokerClientImpl.retryDelay(0)).isEqualTo(Duration.ofMillis(20));
    assertThat(BrokerClientImpl.retryDelay(1)).isEqualTo(Duration.ofMillis(40));
    assertThat(BrokerClientImpl.retryDelay(2)).isEqualTo(Duration.ofMillis(80));
    assertThat(BrokerClientImpl.retryDelay(3)).isEqualTo(Duration.ofMillis(160));
    assertThat(BrokerClientImpl.retryDelay(4)).isEqualTo(Duration.ofMillis(320));
    assertThat(BrokerClientImpl.retryDelay(5)).isEqualTo(Duration.ofMillis(500));
    assertThat(BrokerClientImpl.retryDelay(6)).isEqualTo(Duration.ofMillis(500));
    assertThat(BrokerClientImpl.retryDelay(100)).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  void shouldTerminateInFlightRequestsWhenTheLeaderMovesToAnotherMember() throws Exception {
    when(topologyService.getPartitionLeaderInfo(PARTITION)).thenReturn(leader(SUCCESSOR, TERM + 1));

    final var response = sendRequestAwaitingReply();

    topologyChanged();

    assertTerminatedByLeaderChange(response);
  }

  @Test
  void shouldTerminateInFlightRequestsWhenTheLeaderTermChanges() throws Exception {
    when(topologyService.getPartitionLeaderInfo(PARTITION)).thenReturn(leader(LEADER, TERM + 1));

    final var response = sendRequestAwaitingReply();

    topologyChanged();

    assertTerminatedByLeaderChange(response);
  }

  @Test
  void shouldTerminateInFlightRequestsWhenThePartitionHasNoLeader() throws Exception {
    final var response = sendRequestAwaitingReply();

    topologyChanged();

    assertTerminatedByLeaderChange(response);
  }

  @Test
  void shouldKeepInFlightRequestsWhenTheirLeaderIsUnchanged() throws Exception {
    final CompletableFuture<byte[]> reply = new CompletableFuture<>();
    when(topologyService.getPartitionLeaderInfo(PARTITION)).thenReturn(leader(LEADER, TERM));

    final var response = sendRequestAwaitingReply(reply);
    topologyChanged();
    // 通知只是把核对任务排进 actor：等 actor 处理完它，请求仍未被终止
    client.submit(() -> {}).get(10, TimeUnit.SECONDS);
    assertThat(response).isNotDone();

    reply.complete(success());

    assertThat(response.get(10, TimeUnit.SECONDS).isSuccess()).isTrue();
  }

  @Test
  void shouldNotTerminateRequestsWaitingForARetry() throws Exception {
    // 首次尝试在转移窗口被拒（命令没进日志）后进入退避，此后重新寻址一直解析不出 leader：
    // 退避中的请求没有命令在路上，leader 变化不终止它，继续在预算内重试直到预算耗尽
    when(plane.resolve(any()))
        .thenReturn(destination(LEADER, TERM))
        .thenThrow(new BrokerException(BrokerException.LEADER_UNRESOLVABLE, "no leader"));
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(CompletableFuture.completedFuture(error(BrokerResponseCodes.NOT_LEADER)));
    when(topologyService.getPartitionLeaderInfo(PARTITION)).thenReturn(leader(SUCCESSOR, TERM + 1));

    final var response = client.sendRequestWithRetry(request(), Duration.ofMillis(300));
    topologyChanged();

    assertFailsWith(response, TimeoutException.class);
  }

  @Test
  void shouldNotTrackRequestsWithoutALeaderKey() throws Exception {
    // 管理面请求不带目标 leader 身份，不参与在途核对
    final CompletableFuture<byte[]> reply = new CompletableFuture<>();
    when(plane.resolve(any()))
        .thenReturn(new BrokerPlane.Destination("topic", Address.from("127.0.0.1", 26502), null));

    final var response = sendRequestAwaitingReply(reply);
    topologyChanged();
    client.submit(() -> {}).get(10, TimeUnit.SECONDS);
    assertThat(response).isNotDone();
    verify(topologyService, never()).getPartitionLeaderInfo(any());

    reply.complete(success());

    assertThat(response.get(10, TimeUnit.SECONDS).isSuccess()).isTrue();
  }

  /** 模拟拓扑视图重建后的通知（运行在拓扑服务线程上，只把核对任务排进客户端 actor） */
  private void topologyChanged() {
    topologyChange.get().run();
  }

  /** 发出一个得不到应答的请求，等它真正发出：登记在途与发送在同一 actor 任务内，发出时已登记在途 */
  private CompletableFuture<BrokerResponse<ResponseRecordValue>> sendRequestAwaitingReply() {
    return sendRequestAwaitingReply(new CompletableFuture<>());
  }

  private CompletableFuture<BrokerResponse<ResponseRecordValue>> sendRequestAwaitingReply(
      final CompletableFuture<byte[]> reply) {
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(reply);
    final var response = client.sendRequestWithRetry(request());
    verify(messagingService, timeout(10_000))
        .sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class));
    return response;
  }

  private static void assertTerminatedByLeaderChange(final CompletableFuture<?> response) {
    final ExecutionException failure =
        catchThrowableOfType(() -> response.get(10, TimeUnit.SECONDS), ExecutionException.class);
    assertThat(failure).hasCauseInstanceOf(LeaderChangedException.class);
    assertThat(failure.getCause()).hasMessageContaining("may have been applied");
  }

  /** 第一次发送按给定结果失败，第二次成功 */
  private void sendFailsThenSucceeds(final CompletableFuture<byte[]> firstAttempt) {
    when(messagingService.sendAndReceive(any(), anyString(), any(byte[].class), any(Duration.class)))
        .thenReturn(firstAttempt)
        .thenReturn(CompletableFuture.completedFuture(success()));
  }

  private static void assertFailsWith(
      final CompletableFuture<?> response, final Class<? extends Throwable> expectedCause) {
    assertThatThrownBy(() -> response.get(10, TimeUnit.SECONDS))
        .hasCauseInstanceOf(expectedCause);
  }

  private static ProcessInstanceCreateRequest request() {
    return new ProcessInstanceCreateRequest()
        .setProcessDefinitionId(123L)
        .setTenantId("")
        .setVariable(Map.of());
  }

  /** 发往 {@code leader} 在任期 {@code term} 上的业务面目标 */
  private static BrokerPlane.Destination destination(final MemberId leader, final long term) {
    return new BrokerPlane.Destination(
        "topic", Address.from("127.0.0.1", 26502), new BrokerPlane.LeaderKey(PARTITION, leader, term));
  }

  private static PartitionLeaderInfo leader(final MemberId memberId, final long term) {
    return new PartitionLeaderInfo(memberId, term, Address.from("127.0.0.1", 26502));
  }

  private static byte[] success() {
    return response(true, 0, null);
  }

  private static byte[] error(final int code) {
    return response(false, code, "broker message");
  }

  private static byte[] response(final boolean isSuccess, final int code, final String message) {
    final var writer = new ApiResponseWriterImpl().requestType(1).valueType(1).lifeCycle(-1);
    if (isSuccess) {
      writer.success();
    } else {
      writer.fail(code, message);
    }
    final var buffer = new UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(buffer, 0);
    return buffer.byteArray();
  }
}
