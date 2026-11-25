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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

/**
 * 跨分区命令交付单测：目标分区确认已写入才算完结，明确没写入的命令重新寻址后重发，等不到确认的不重发。
 *
 * <p>与接收器之间的请求应答经接收器登记的应答处理器在进程内路由，走的是真实的编解码。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Timeout(30)
class InterPartitionCommandSenderServiceTest {
  private static final PartitionId PARTITION = PartitionId.from("business", 1);
  private static final String SUBJECT = INNER_PARTITION_TOPIC_PREFIX + PARTITION;
  private static final int SOURCE_ID = 3;
  private static final MemberId LEADER = MemberId.from("leader");
  private static final MemberId FORMER_LEADER = MemberId.from("former-leader");
  private static final MemberId NEW_LEADER = MemberId.from("new-leader");
  private static final DistributeSerialLifeCycle LIFE_CYCLE =
      DistributeSerialLifeCycle.DISTRIBUTE_ACK;
  /** 等待确认的命令数上限 */
  private static final int MAX_PENDING_COMMANDS = 10_000;
  /** 一次重发退避（200 毫秒）之后留出的观察窗：窗口内没有重发即认为不会重发 */
  private static final int NO_RESEND_WINDOW_MILLIS = 600;

  private final ClusterCommunicationService communicationService =
      mock(ClusterCommunicationService.class);
  private final ClusterTopologyService topologyService = mock(ClusterTopologyService.class);
  private final EventLogWriter logStreamWriter = mock(EventLogWriter.class);
  /** 目标分区接收器登记的应答处理器 */
  private final AtomicReference<ReplyHandler> replyHandler = new AtomicReference<>();

  private ActorScheduler scheduler;
  private InterPartitionCommandSenderService sender;

  @BeforeEach
  void setUp() throws Exception {
    when(topologyService.getPartitionBySourceId(SOURCE_ID)).thenReturn(PARTITION);
    when(topologyService.getPartitionLeader(PARTITION)).thenReturn(LEADER);
    scheduler = ActorScheduler.newDefaultActorScheduler();
    scheduler.start();
    sender = new InterPartitionCommandSenderService(communicationService, topologyService);
    scheduler.submitActor(sender).get(10, TimeUnit.SECONDS);
  }

  @AfterEach
  void tearDown() {
    sender.close();
    scheduler.close();
  }

  @Test
  void shouldWriteTheCommandOnTheTargetLeaderAndStopOnceItIsAcknowledged() throws Exception {
    startReceiverLeader();
    when(logStreamWriter.tryAppend(eq(WriteContext.INTER_PARTITION), any(AppendEntry.class)))
        .thenReturn(new AppendResult.Appended(1L, 1L));
    final var appendedEntries = ArgumentCaptor.forClass(AppendEntry.class);

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, 7L, -1L, command());

    verify(logStreamWriter, timeout(10_000).atLeastOnce())
        .tryAppend(eq(WriteContext.INTER_PARTITION), appendedEntries.capture());
    // 跨分区命令带上发送方指定的 key 与完整的命令值
    assertThat(appendedEntries.getValue().key()).isEqualTo(7L);
    assertThat(appendedEntries.getValue().value().getLength()).isEqualTo(command().getLength());
    verifySentTo(LEADER, after(NO_RESEND_WINDOW_MILLIS).times(1));
  }

  @Test
  void shouldResendARejectedCommandUnchangedUntilTheTargetPartitionTakesIt() throws Exception {
    startReceiverLeader();
    // 目标分区先在冻结窗口内拒收，解冻后收下
    when(logStreamWriter.tryAppend(eq(WriteContext.INTER_PARTITION), any(AppendEntry.class)))
        .thenReturn(new AppendResult.Rejected(AppendResult.RejectionReason.FROZEN))
        .thenReturn(new AppendResult.Appended(2L, 2L));
    final var messages = ArgumentCaptor.forClass(byte[].class);

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    verify(communicationService, timeout(10_000).times(2))
        .send(eq(SUBJECT), messages.capture(), any(), any(), eq(LEADER), any(Duration.class));
    assertThat(messages.getAllValues().get(1)).isEqualTo(messages.getAllValues().get(0));
    // 第二次已被确认写入：到此为止
    verifySentTo(LEADER, after(NO_RESEND_WINDOW_MILLIS).times(2));
  }

  @Test
  void shouldResendACommandTheTargetPartitionRejectsForMissingDiskSpace() throws Exception {
    final var receiver = startReceiverLeader();
    receiver.onDiskSpaceNotAvailable();
    // 等接收器把"无磁盘空间"标记处理完，首次尝试才会因它被拒
    receiver.submit(() -> {}).get(10, TimeUnit.SECONDS);

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    // 没有磁盘空间的接收方不写日志，但明确答复拒收：发送端据此重发
    verifySentTo(LEADER, timeout(10_000).times(2));
    verify(logStreamWriter, never())
        .tryAppend(eq(WriteContext.INTER_PARTITION), any(AppendEntry.class));
  }

  @Test
  void shouldResendToTheNewLeaderWhenTheOldOneNoLongerHandlesCommands() {
    doReturn(
            CompletableFuture.failedFuture(new MessagingException.NoRemoteHandler(SUBJECT)),
            CompletableFuture.completedFuture(InterPartitionDeliveryStatus.APPENDED))
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());
    // 首次尝试打给了已退位的旧 leader，重发时拓扑已指向新 leader
    when(topologyService.getPartitionLeader(PARTITION)).thenReturn(FORMER_LEADER, NEW_LEADER);

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    verifySentTo(NEW_LEADER, timeout(10_000).times(1));
    verifySentTo(FORMER_LEADER, times(1));
  }

  @Test
  void shouldHoldACommandUntilItsPartitionHasALeader() {
    doReturn(CompletableFuture.completedFuture(InterPartitionDeliveryStatus.APPENDED))
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());
    when(topologyService.getPartitionLeader(PARTITION)).thenReturn(null).thenReturn(LEADER);

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    verifySentTo(LEADER, timeout(10_000).times(1));
  }

  @Test
  void shouldNotResendWhenTheTargetPartitionDoesNotAnswer() {
    doReturn(CompletableFuture.failedFuture(new TimeoutException("Request timed out in PT5S")))
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    verifySentTo(LEADER, after(NO_RESEND_WINDOW_MILLIS).times(1));
  }

  @Test
  void shouldNotResendWhenTheTargetPartitionFailsToHandleTheCommand() {
    // 处理器异常时命令是否已写入未知
    doReturn(CompletableFuture.failedFuture(new MessagingException.RemoteHandlerFailure("boom")))
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());

    verifySentTo(LEADER, after(NO_RESEND_WINDOW_MILLIS).times(1));
  }

  @Test
  void shouldDropCommandsBeyondThePendingLimit() throws Exception {
    // 目标分区一直不应答：等待确认的命令只增不减
    doReturn(new CompletableFuture<InterPartitionDeliveryStatus>())
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());

    for (int i = 0; i <= MAX_PENDING_COMMANDS; i++) {
      sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());
    }
    // 等发送器把 MAX_PENDING_COMMANDS + 1 条命令都处理完
    sender.submit(() -> {}).get(10, TimeUnit.SECONDS);

    verifySentTo(LEADER, times(MAX_PENDING_COMMANDS));
  }

  @Test
  void shouldStopResendingOnceClosed() throws Exception {
    doReturn(CompletableFuture.completedFuture(InterPartitionDeliveryStatus.REJECTED))
        .when(communicationService)
        .send(anyString(), any(), any(), any(), any(), any());

    sender.sendCommand(SOURCE_ID, LIFE_CYCLE, command());
    verifySentTo(LEADER, timeout(10_000).times(1));

    sender.closeAsync().get(10, TimeUnit.SECONDS);

    verifySentTo(LEADER, after(NO_RESEND_WINDOW_MILLIS).times(1));
  }

  @Test
  void shouldRoundTripTheDeliveryStatusThroughItsSingleByte() {
    for (final var status : InterPartitionDeliveryStatus.values()) {
      assertThat(InterPartitionDeliveryStatus.fromBytes(InterPartitionDeliveryStatus.toBytes(status)))
          .isEqualTo(status);
    }
  }

  @Test
  void shouldBackOffExponentiallyUpToTheCap() {
    assertThat(InterPartitionCommandSenderService.retryDelay(0)).isEqualTo(Duration.ofMillis(200));
    assertThat(InterPartitionCommandSenderService.retryDelay(1)).isEqualTo(Duration.ofMillis(400));
    assertThat(InterPartitionCommandSenderService.retryDelay(2)).isEqualTo(Duration.ofMillis(800));
    assertThat(InterPartitionCommandSenderService.retryDelay(3))
        .isEqualTo(Duration.ofMillis(1600));
    assertThat(InterPartitionCommandSenderService.retryDelay(4)).isEqualTo(Duration.ofSeconds(2));
    assertThat(InterPartitionCommandSenderService.retryDelay(100))
        .isEqualTo(Duration.ofSeconds(2));
  }

  /** 核对发往 {@code leader} 的请求数 */
  private void verifySentTo(final MemberId leader, final VerificationMode mode) {
    verify(communicationService, mode)
        .send(eq(SUBJECT), any(byte[].class), any(), any(), eq(leader), any(Duration.class));
  }

  /** 在本进程内起一个目标分区的接收器（leader 角色），并把发送端的请求路由到它登记的应答处理器 */
  private InterPartitionCommandReceiverActor startReceiverLeader() throws Exception {
    doAnswer(
            invocation -> {
              replyHandler.set(
                  new ReplyHandler(
                      invocation.getArgument(1),
                      invocation.getArgument(2),
                      invocation.getArgument(3),
                      invocation.getArgument(4)));
              return null;
            })
        .when(communicationService)
        .replyTo(anyString(), any(), any(), any(), any());
    when(communicationService.send(anyString(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              final ReplyHandler handler = replyHandler.get();
              if (handler == null) {
                return CompletableFuture.failedFuture(
                    new MessagingException.NoRemoteHandler(invocation.getArgument(0)));
              }
              final Function<Object, byte[]> requestEncoder = invocation.getArgument(2);
              final Function<byte[], Object> responseDecoder = invocation.getArgument(3);
              final byte[] request = requestEncoder.apply(invocation.getArgument(1));
              // 与集群通信服务的应答包装一致：处理器在登记的执行器上运行，结果经登记的编码器编码后才回给发送端
              final CompletableFuture<byte[]> response = new CompletableFuture<>();
              handler
                  .executor()
                  .execute(
                      () -> {
                        try {
                          response.complete(
                              handler
                                  .encoder()
                                  .apply(
                                      handler
                                          .handler()
                                          .apply(
                                              invocation.getArgument(4),
                                              handler.decoder().apply(request))));
                        } catch (final Exception e) {
                          response.completeExceptionally(e);
                        }
                      });
              return response.thenApply(responseDecoder);
            });
    final var receiver =
        new InterPartitionCommandReceiverActor(
            PARTITION,
            communicationService,
            logStreamWriter,
            DefaultRecordValueMapper.getInstance());
    scheduler.submitActor(receiver).get(10, TimeUnit.SECONDS);
    return receiver;
  }

  /** 接收器登记的应答处理器：命令解码器、处理器、应答编码器与处理器的执行线程 */
  private record ReplyHandler(
      Function<byte[], Object> decoder,
      BiFunction<MemberId, Object, Object> handler,
      Function<Object, byte[]> encoder,
      Executor executor) {}

  private static DistributeSerialRecord command() {
    return new DistributeSerialRecord()
        .setDistributeId(42L)
        .setDistributeRecordId(43L)
        .setCurrentDistributeSourceId(1);
  }
}
