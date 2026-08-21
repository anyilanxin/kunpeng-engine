/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException.NoSuchMemberException;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.UnicastService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证 {@link DefaultClusterCommunicationService} 在收到来源不明的请求/消息时，
 * 会拒绝请求或静默丢弃消息，避免未识别节点进入业务处理链路。
 */
@ExtendWith(MockitoExtension.class)
class DefaultClusterCommunicationServiceTest {

  private static final String TOPIC = "kunpeng-internal-topic";
  private static final byte[] PAYLOAD = "internal-payload".getBytes();

  @Mock private ClusterMembershipService membership;
  @Mock private MessagingService messaging;
  @Mock private UnicastService unicast;

  private DefaultClusterCommunicationService service;
  private Address strangerAddress;

  private final Function<byte[], String> payloadDecoder = String::new;
  private final Function<String, byte[]> payloadEncoder = String::getBytes;

  @BeforeEach
  void createService() {
    service = new DefaultClusterCommunicationService(membership, messaging, unicast);
    strangerAddress = new Address("stranger.host", 9099);
    when(membership.getMember(strangerAddress)).thenReturn(null);
  }

  /** 捕获注册到 MessagingService 上的请求处理函数。 */
  @SuppressWarnings("unchecked")
  private BiFunction<Address, byte[], CompletableFuture<byte[]>> registeredHandler() {
    final ArgumentCaptor<BiFunction<Address, byte[], CompletableFuture<byte[]>>> captor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(messaging).registerHandler(eq(TOPIC), captor.capture());
    return captor.getValue();
  }

  /** 断言来自陌生地址的请求会被以 NoSuchMemberException 拒绝。 */
  private void assertHandlerRejectsStranger(
      final BiFunction<Address, byte[], CompletableFuture<byte[]>> handler) {
    final var result = handler.apply(strangerAddress, PAYLOAD);

    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(NoSuchMemberException.class)
        .hasMessageContaining(strangerAddress.toString());
  }

  @Test
  void rejectsStrangerInSyncRequestReplyHandler() {
    // given：注册一个同步请求-应答处理器
    service.replyTo(
        TOPIC,
        payloadDecoder,
        msg -> CompletableFuture.completedFuture("pong"),
        payloadEncoder);

    // when / then：调用被捕获的处理器，陌生来源应被拒绝
    assertHandlerRejectsStranger(registeredHandler());
  }

  @Test
  void rejectsStrangerInAsyncRequestReplyHandler() {
    // given：注册一个带执行器的异步请求-应答处理器
    service.replyToAsync(
        TOPIC,
        payloadDecoder,
        msg -> CompletableFuture.completedFuture("pong"),
        payloadEncoder,
        Runnable::run);

    // when / then
    assertHandlerRejectsStranger(registeredHandler());
  }

  @Test
  void rejectsStrangerInBiFunctionRequestReplyHandler() {
    // given：注册一个携带 MemberId 的双参处理器
    service.replyTo(
        TOPIC,
        payloadDecoder,
        (MemberId sender, String msg) -> "pong:" + msg,
        payloadEncoder,
        Runnable::run);

    // when / then
    assertHandlerRejectsStranger(registeredHandler());
  }

  @Test
  void dropsStrangerMessageInConsumerSubscription() {
    // given：注册一个普通消费者
    @SuppressWarnings("unchecked")
    final Consumer<String> businessHandler = mock(Consumer.class);
    service.consume(TOPIC, payloadDecoder, businessHandler, Runnable::run);

    // when：捕获底层处理器并用陌生地址触发
    final ArgumentCaptor<BiConsumer<Address, byte[]>> captor =
        ArgumentCaptor.forClass(BiConsumer.class);
    verify(messaging).registerHandler(eq(TOPIC), captor.capture(), eq(Runnable::run));
    captor.getValue().accept(strangerAddress, PAYLOAD);

    // then：业务处理器不会被调用
    verify(businessHandler, never()).accept(any());
  }

  @Test
  void dropsStrangerMessageInBiConsumerSubscription() {
    // given：注册一个携带 MemberId 的消费者
    @SuppressWarnings("unchecked")
    final BiConsumer<MemberId, String> businessHandler = mock(BiConsumer.class);
    service.consume(TOPIC, payloadDecoder, businessHandler, Runnable::run);

    // when
    final ArgumentCaptor<BiConsumer<Address, byte[]>> captor =
        ArgumentCaptor.forClass(BiConsumer.class);
    verify(messaging).registerHandler(eq(TOPIC), captor.capture(), eq(Runnable::run));
    captor.getValue().accept(strangerAddress, PAYLOAD);

    // then：业务处理器不会被调用
    verify(businessHandler, never()).accept(any(), any());
  }
}
