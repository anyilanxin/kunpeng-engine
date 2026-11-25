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

import com.anyilanxin.kunpeng.broker.client.core.ApiRequestWriterImpl;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReader;
import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReaderImpl;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerException;
import com.anyilanxin.kunpeng.broker.client.core.BrokerPlane;
import com.anyilanxin.kunpeng.broker.client.core.BrokerRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.broker.client.core.LeaderChangedException;
import com.anyilanxin.kunpeng.broker.client.core.RequestContent;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.Subscription;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.TopologyListener;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.PartitionLeaderInfo;
import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * broker 客户端默认实现：Actor 单线程化的统一封装发送管线。
 *
 * <p>寻址与响应物化委托给 {@link BrokerPlane}；编码、发送、重试、响应解码在 actor 线程内完成。
 *
 * <p>重试（{@link #sendRequestWithRetry}）只针对"命令确定没有进入日志"的失败：分区解析不出
 * leader、目标节点上没有命令处理器、转移窗口被拒。等应答超时与连接类失败命令可能已被执行，不重试，原样交回调用方。
 *
 * <p>在途请求带着发出时的目标 leader 身份登记。拓扑变化后目标分区的 leader 或任期变了，说明原 leader 已不可能应答（崩溃、退位、被隔离），立即以 {@link
 * LeaderChangedException} 结束，不等调用方超时。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerClientImpl extends Actor implements BrokerClient {
  private static final Logger LOG = LoggerFactory.getLogger(BrokerClientImpl.class);

  /** 重试退避下限 */
  private static final Duration RETRY_DELAY_MIN = Duration.ofMillis(20);

  /** 重试退避上限 */
  private static final Duration RETRY_DELAY_MAX = Duration.ofMillis(500);

  /** 退避翻倍的次数上限：下限的 2 的 5 次方倍已越过上限，再多移位只会溢出 */
  private static final int RETRY_BACKOFF_DOUBLINGS = 5;

  private final MessagingService messagingService;
  private final ClusterEventService eventService;
  private final BrokerPlane plane;
  private final ClusterTopologyService topologyService;
  private final BrokerTopologyManager topologyManager;

  /** 拓扑变化通知（来自拓扑服务线程）：切回本 actor 后核对在途请求 */
  private final TopologyListener topologyListener =
      topology -> actor.run(this::failRequestsToFormerLeaders);

  /** 在途请求登记，只在 actor 线程上读写：响应 future -> 发出时的目标 leader 身份 */
  private final Map<CompletableFuture, BrokerPlane.LeaderKey> inFlightRequests = new HashMap<>();

  private Subscription jobAvailableSubscription;
  private boolean isClosed;

  /**
   * 可复用的工作缓冲区。由于 {@link BrokerClientImpl} 是一个 {@link Actor}（单线程执行），且缓冲区在 请求被异步分发之前，已于同步的 {@code
   * value.write(...)} 调用中被完全消费，因此可以安全复用。
   */
  private byte[] reusableValueBytes = new byte[0];

  private final UnsafeBuffer reusableValueBuffer = new UnsafeBuffer(0, 0);

  public BrokerClientImpl(
      final MessagingService messagingService,
      final ClusterEventService eventService,
      final BrokerPlane plane,
      final ClusterTopologyService topologyService) {
    this.messagingService = messagingService;
    this.eventService = eventService;
    this.plane = plane;
    this.topologyManager = new BrokerTopologyManager(topologyService);
    this.topologyService = topologyService;
    topologyManager.addTopologyListener(topologyListener);
  }

  @Override
  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(
          final BrokerRequest<Request> request) {
    final CompletableFuture<BrokerResponse<Response>> responseFuture = new CompletableFuture<>();
    sendRequest(request, Duration.ofSeconds(20), false, responseFuture);
    return responseFuture;
  }

  @Override
  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(
          final BrokerRequest<Request> request, final Duration requestTimeout) {
    final CompletableFuture<BrokerResponse<Response>> responseFuture = new CompletableFuture<>();
    sendRequest(request, requestTimeout, false, responseFuture);
    return responseFuture;
  }

  @Override
  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          final BrokerRequest<Request> request) {
    final CompletableFuture<BrokerResponse<Response>> responseFuture = new CompletableFuture<>();
    sendRequest(request, Duration.ofSeconds(20), true, responseFuture);
    return responseFuture;
  }

  @Override
  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          final BrokerRequest<Request> request, final Duration requestTimeout) {
    final CompletableFuture<BrokerResponse<Response>> responseFuture = new CompletableFuture<>();
    sendRequest(request, requestTimeout, true, responseFuture);
    return responseFuture;
  }

  @Override
  public BrokerTopologyManager getTopologyManager() {
    return topologyManager;
  }

  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void sendRequest(
          final BrokerRequest<Request> request,
          final Duration requestTimeout,
          final boolean retry,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    // 重试共用首次发送的截止预算：只在预算内重试，超预算即超时失败，不无限延长
    final long deadlineNs = System.nanoTime() + requestTimeout.toNanos();
    // 寻址与编码都必须在 actor 线程内执行：sendRequest 由多个调用方线程并发调用，
    // 而 reusableValueBytes/Buffer 是实例字段。在 actor.run 内串行化，既消除竞态，又能让池化跨请求真正安全生效。
    actor.run(
        () ->
            resolveAndSend(
                new Call<>(request, retry, requestTimeout, deadlineNs, 0, responseFuture)));
  }

  /** 一次请求从首次发送到最后一次重试的调用上下文：各次尝试共用截止预算与响应 future，只有重试序号在变。 */
  private record Call<Request extends RequestRecordValue, Response extends ResponseRecordValue>(
      BrokerRequest<Request> request,
      boolean retry,
      Duration requestTimeout,
      long deadlineNs,
      int attempt,
      CompletableFuture<BrokerResponse<Response>> responseFuture) {

    /** 进入下一次尝试：重试序号加一（决定退避时长），其余不变 */
    Call<Request, Response> nextAttempt() {
      return new Call<>(request, retry, requestTimeout, deadlineNs, attempt + 1, responseFuture);
    }
  }

  /** 第 {@code attempt} 次重试前的退避：20 毫秒起每次翻倍，封顶 500 毫秒。转移窗口内的拒绝是立即返回的，退避太密会在预算里空转成百上千次。 */
  static Duration retryDelay(final int attempt) {
    return Duration.ofMillis(
        Math.min(
            RETRY_DELAY_MIN.toMillis() << Math.min(attempt, RETRY_BACKOFF_DOUBLINGS),
            RETRY_DELAY_MAX.toMillis()));
  }

  /**
   * 该失败是否"命令确定没有进入日志"：目标分区解析不出 leader（选举进行中、成员离线、端口未广播），或目标节点上没有命令处理器 （打到了非 leader、或新 leader
   * 还没登记处理器）。只有这两类可以安全重试。
   */
  static boolean isRetrySafe(final Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof final BrokerException brokerException
          && brokerException.getCode() == BrokerException.LEADER_UNRESOLVABLE) {
        return true;
      }
      if (cause instanceof MessagingException.NoRemoteHandler) {
        return true;
      }
    }
    return false;
  }

  /** 在 actor 线程上解析目标并派发请求；解析失败按失败出口处理。 */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void resolveAndSend(final Call<Request, Response> call) {
    // 请求已被终止（退避期间 leader 变更）就不再发出：排定的重试可能在终止之后才到点
    if (call.responseFuture().isDone()) {
      return;
    }
    final BrokerPlane.Destination destination;
    try {
      destination = plane.resolve(call.request());
    } catch (final BrokerException e) {
      failOrRetry(call, e);
      return;
    }
    encodeAndSend(call, destination);
  }

  /** 失败出口：可安全重试的失败在预算内退避重发，其余原样交回调用方；非重试请求任何失败都不重试。 */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void failOrRetry(final Call<Request, Response> call, final Throwable failure) {
    if (!call.retry() || !isRetrySafe(failure)) {
      call.responseFuture().completeExceptionally(failure);
      return;
    }
    scheduleRetry(call, failure);
  }

  /** 预算内退避后重新寻址再发（leader 可能已切换）；预算耗尽以超时失败完结 future。 */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void scheduleRetry(final Call<Request, Response> call, final Throwable lastFailure) {
    if (System.nanoTime() >= call.deadlineNs()) {
      final var timeout =
          new TimeoutException(
              "Retry budget of "
                  + call.requestTimeout().toMillis()
                  + " ms exhausted, last failure: "
                  + lastFailure.getMessage());
      timeout.initCause(lastFailure);
      call.responseFuture().completeExceptionally(timeout);
      return;
    }
    scheduleRetry(call, lastFailure.getMessage());
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void scheduleRetry(final Call<Request, Response> call, final String lastFailure) {
    if (System.nanoTime() >= call.deadlineNs()) {
      call.responseFuture()
          .completeExceptionally(
              new TimeoutException(
                  "Retry budget of "
                      + call.requestTimeout().toMillis()
                      + " ms exhausted, last failure: "
                      + lastFailure));
      return;
    }
    // 退避期间没有请求在路上：先摘除登记，否则拓扑变化会把等着重试的请求当成在途而终止它
    final CompletableFuture responseFuture = call.responseFuture();
    actor.run(() -> inFlightRequests.remove(responseFuture));
    actor.schedule(retryDelay(call.attempt()), () -> resolveAndSend(call.nextAttempt()));
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void encodeAndSend(
          final Call<Request, Response> call, final BrokerPlane.Destination destination) {
    final BrokerRequest<Request> request = call.request();
    // 统一封装格式
    final ApiRequestWriterImpl apiRequestWriter = new ApiRequestWriterImpl();
    apiRequestWriter
        .requestType(request.requestType())
        .key(request.key())
        .valueType(request.valueType())
        .lifeCycle(request.lifeCycle());
    final Request value = request.getValue();
    if (value != null) {
      final int valueLength = value.getLength();
      // 复用工作缓冲区 —— 按需扩容、从不收缩（高水位标记）。
      // 安全性说明：该方法仅在 actor 线程中执行（单线程）。
      if (reusableValueBytes.length < valueLength) {
        reusableValueBytes = new byte[valueLength];
      }
      reusableValueBuffer.wrap(reusableValueBytes, 0, valueLength);
      value.write(reusableValueBuffer, 0);
      apiRequestWriter.data(reusableValueBuffer);
    }
    if (request.authorizationMetadata() != null) {
      apiRequestWriter.authorizationMetadata(request.authorizationMetadata());
    }
    // 统一封装格式编码 byte[]
    final MutableDirectBuffer requestDataBuffer =
        new UnsafeBuffer(new byte[apiRequestWriter.getLength()]);
    apiRequestWriter.write(requestDataBuffer, 0);

    final RequestContent<Response> requestContent =
        new RequestContent<>(
            destination.apiAddress(),
            destination.topicName(),
            call.retry(),
            call.requestTimeout(),
            requestDataBuffer.byteArray(),
            call.responseFuture());
    // 记下发出时的目标 leader 身份，拓扑变化时据此核对；请求结束（成功、失败、超时）即摘除登记
    final BrokerPlane.LeaderKey leader = destination.leader();
    if (leader != null) {
      final CompletableFuture responseFuture = call.responseFuture();
      inFlightRequests.put(responseFuture, leader);
      responseFuture.whenComplete(
          (ignored, error) -> actor.run(() -> inFlightRequests.remove(responseFuture)));
    }
    sendInternal(call, requestContent);
  }

  /**
   * 拓扑变化后核对在途请求（actor 线程）：目标分区当前的 leader 或任期与发出时不同，说明原 leader 已不可能应答——它退了位、崩溃了，或者被隔离后 另选出了新
   * leader。立即以失败结束，不让调用方等到超时。命令可能已被新 leader 执行，交回的是"结果未知"。
   */
  private void failRequestsToFormerLeaders() {
    // 遍历副本：终止请求会同步触发摘除登记，直接遍历原表会被打断
    final Map<CompletableFuture, BrokerPlane.LeaderKey> snapshot = new HashMap<>(inFlightRequests);
    snapshot.forEach(
        (responseFuture, sentTo) -> {
          final PartitionLeaderInfo current =
              topologyService.getPartitionLeaderInfo(sentTo.partitionId());
          final boolean sameLeader =
              current != null
                  && current.term() == sentTo.term()
                  && current.memberId().equals(sentTo.memberId());
          if (!sameLeader) {
            responseFuture.completeExceptionally(new LeaderChangedException(sentTo.partitionId()));
          }
        });
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void sendInternal(
          final Call<Request, Response> call, final RequestContent<Response> requestContext) {
    messagingService
        .sendAndReceive(
            requestContext.apiAddress(),
            requestContext.topicName(),
            requestContext.data(),
            requestContext.requestTimeout())
        .whenComplete(
            (bytes, throwable) -> {
              if (throwable != null) {
                failOrRetry(call, throwable);
              } else {
                handleResponse(call, bytes);
              }
            });
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void handleResponse(final Call<Request, Response> call, final byte[] bytes) {
    try {
      // 统一响应格式解码 + 面别物化
      final MutableDirectBuffer responseBuffer = new UnsafeBuffer(bytes);
      final ApiResponseReader responseReader = new ApiResponseReaderImpl();
      responseReader.wrap(responseBuffer, 0, responseBuffer.capacity());
      // 转移窗口内的拒绝：命令没有进入日志，退避后重新寻址再发（新 leader 就绪后才会收下）
      if (call.retry()
          && !responseReader.success()
          && responseReader.code() == BrokerResponseCodes.NOT_LEADER) {
        scheduleRetry(call, responseReader.message());
        return;
      }
      final Response value = (Response) plane.materialize(responseReader);
      call.responseFuture().complete(new BrokerResponse<>(responseReader, value));
    } catch (final Exception e) {
      // 解码失败以异常完结 future；此处运行在消息回调线程，抛出会打断传输层回调循环
      LOG.error("Failed to handle response", e);
      call.responseFuture().completeExceptionally(e);
    }
  }

  @Override
  public void close() {
    if (isClosed) {
      return;
    }

    isClosed = true;
    topologyManager.removeTopologyListener(topologyListener);
    if (jobAvailableSubscription != null) {
      jobAvailableSubscription.close();
    }
    super.close();
  }

  @Override
  public void subscribeJobAvailableNotification(
      final String topic, final Consumer<String> handler) {
    jobAvailableSubscription =
        eventService
            .subscribe(
                topic,
                msg -> {
                  handler.accept((String) msg);
                  return CompletableFuture.completedFuture(null);
                })
            .join();
  }
}
