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
package io.atomix.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import io.atomix.cluster.messaging.MessagingException;
import io.atomix.utils.net.Address;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证 {@link RemoteClientConnection} 在单向消息与请求-应答两条路径上的指标埋点行为。 */
public class RemoteClientConnectionTest {

  private static final int REMOTE_PORT = 26501;
  private static final String TOPIC = "orders";
  private static final byte[] BODY = "order-payload".getBytes();

  private RecordingMetrics metrics;
  private RemoteClientConnection connection;
  private InetSocketAddress remote;

  /** 用指定主题构造一条协议请求。 */
  private static ProtocolRequest request(final String subject) {
    return new ProtocolRequest(42, new Address("", REMOTE_PORT), subject, BODY);
  }

  @BeforeEach
  public void setUp() {
    remote = new InetSocketAddress(0);
    final var channel = mock(Channel.class);
    when(channel.remoteAddress()).thenReturn(remote);
    final var writeFuture = mock(ChannelFuture.class);
    when(channel.writeAndFlush(any())).thenReturn(writeFuture);

    metrics = new RecordingMetrics();
    connection = new RemoteClientConnection(metrics, channel);
  }

  @Test
  public void oneWayMessageIsCountedAsMessageOnly() {
    // when：发送单向消息
    connection.sendAsync(request(TOPIC));

    // then：只计入 message 计数与请求体大小
    final var key = metrics.keyOf(remote.toString(), TOPIC);
    assertThat(metrics.messageCounter.get(key)).isEqualTo(1);
    assertThat(metrics.sizeBySubject.get(key)).isEqualTo(BODY.length);
    assertThat(metrics.inFlightCounter).isEmpty();
    assertThat(metrics.roundTrips).isEmpty();
  }

  @Test
  public void requestResponseIsCountedAsRoundTrip() {
    // when：发起一次请求-应答调用
    connection.sendAndReceive(request(TOPIC));

    // then：计入在途请求、往返次数与请求体大小，不计入单向消息
    final var key = metrics.keyOf(remote.toString(), TOPIC);
    assertThat(metrics.inFlightCounter.get(key)).isEqualTo(1);
    assertThat(metrics.roundTrips.get(key)).isEqualTo(1);
    assertThat(metrics.sizeBySubject.get(key)).isEqualTo(BODY.length);
    assertThat(metrics.messageCounter).isEmpty();
  }

  @Test
  public void successfulResponseDrainsInFlightAndRecordsOutcome() {
    // given：先挂起一个在途请求
    final CompletableFuture<byte[]> pending = connection.sendAndReceive(request(TOPIC));

    // when：应答正常返回
    pending.complete("done".getBytes());

    // then：在途计数归零、结果为成功，并记录了耗时
    final var key = metrics.keyOf(remote.toString(), TOPIC);
    assertThat(metrics.inFlightCounter.get(key)).isZero();
    assertThat(metrics.roundTrips.get(key)).isEqualTo(1);
    assertThat(metrics.outcomeBySubject.get(key)).isTrue();
    assertThat(metrics.lastLatencyNanos).isPositive();
    assertThat(metrics.messageCounter).isEmpty();
  }

  @Test
  public void failedResponseDrainsInFlightAndRecordsFailure() {
    // given：先挂起一个在途请求
    final CompletableFuture<byte[]> pending = connection.sendAndReceive(request(TOPIC));

    // when：应答异常返回
    pending.completeExceptionally(new IllegalStateException("boom"));

    // then：在途计数归零、结果为失败，并记录了耗时
    final var key = metrics.keyOf(remote.toString(), TOPIC);
    assertThat(metrics.inFlightCounter.get(key)).isZero();
    assertThat(metrics.roundTrips.get(key)).isEqualTo(1);
    assertThat(metrics.outcomeBySubject.get(key)).isFalse();
    assertThat(metrics.lastLatencyNanos).isPositive();
    assertThat(metrics.messageCounter).isEmpty();
  }

  @Test
  public void pendingRequestFailsWhenConnectionIsClosed() {
    // given：挂起一个在途请求
    final CompletableFuture<byte[]> pending = connection.sendAndReceive(request(TOPIC));

    // when：连接被主动关闭
    connection.close();

    // then：在途请求以 ConnectionClosed 失败收尾
    assertThat(pending)
        .failsWithin(250, TimeUnit.MILLISECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MessagingException.ConnectionClosed.class)
        .withMessageContaining("Connection")
        .withMessageContaining("was closed");
  }

  /** 把各埋点回调记录到简单 map 中，便于断言。 */
  private static final class RecordingMetrics implements MessagingMetrics {

    volatile long lastLatencyNanos;
    final Map<String, Integer> messageCounter = new ConcurrentHashMap<>();
    final Map<String, Integer> inFlightCounter = new ConcurrentHashMap<>();
    final Map<String, Integer> roundTrips = new ConcurrentHashMap<>();
    final Map<String, Integer> sizeBySubject = new ConcurrentHashMap<>();
    final Map<String, Boolean> outcomeBySubject = new ConcurrentHashMap<>();

    @Override
    public CloseableSilently startRequestTimer(final String name) {
      final long begin = System.nanoTime();
      return () -> lastLatencyNanos = System.nanoTime() - begin;
    }

    @Override
    public void observeRequestSize(
        final String to, final String name, final int requestSizeInBytes) {
      sizeBySubject.put(keyOf(to, name), requestSizeInBytes);
    }

    @Override
    public void countMessage(final String to, final String name) {
      messageCounter.merge(keyOf(to, name), 1, Integer::sum);
    }

    @Override
    public void countRequestResponse(final String to, final String name) {
      roundTrips.merge(keyOf(to, name), 1, Integer::sum);
    }

    @Override
    public void countSuccessResponse(final String address, final String name) {
      outcomeBySubject.put(keyOf(address, name), true);
    }

    @Override
    public void countFailureResponse(final String address, final String name, final String error) {
      outcomeBySubject.put(keyOf(address, name), false);
    }

    @Override
    public void incInFlightRequests(final String address, final String topic) {
      inFlightCounter.merge(keyOf(address, topic), 1, Integer::sum);
    }

    @Override
    public void decInFlightRequests(final String address, final String topic) {
      inFlightCounter.merge(keyOf(address, topic), -1, Integer::sum);
    }

    String keyOf(final String destination, final String subject) {
      return destination + "-" + subject;
    }
  }
}
