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
import com.anyilanxin.kunpeng.broker.client.core.RequestContent;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.Subscription;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
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
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerClientImpl extends Actor implements BrokerClient {
  private static final Logger LOG = LoggerFactory.getLogger(BrokerClientImpl.class);
  private static final Duration RETRY_DELAY = Duration.ofMillis(10);

  private final MessagingService messagingService;
  private final ClusterEventService eventService;
  private final BrokerPlane plane;
  private final BrokerTopologyManager topologyManager;
  private Subscription jobAvailableSubscription;
  private boolean isClosed;

  /**
   * 可复用的工作缓冲区。由于 {@link BrokerClientImpl} 是一个 {@link Actor}（单线程执行），且缓冲区在 请求被异步分发之前，已于同步的 {@code
   * value.write(...)} 调用中被完整消费，因此可以安全复用。
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
    actor.run(() -> resolveAndSend(request, retry, requestTimeout, deadlineNs, responseFuture));
  }

  /**
   * 在 actor 线程上解析目标并派发请求；解析失败在重试语义下随截止预算重试（leader 未选出/地址未广播是暂态）， 非重试或预算耗尽时以异常完结
   * future（明确失败语义，不空等超时）。
   */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void resolveAndSend(
          final BrokerRequest<Request> request,
          final boolean retry,
          final Duration requestTimeout,
          final long deadlineNs,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    final BrokerPlane.Destination destination;
    try {
      destination = plane.resolve(request);
    } catch (final BrokerException e) {
      retryOrFail(request, retry, requestTimeout, deadlineNs, responseFuture, e);
      return;
    }
    encodeAndSend(request, destination, retry, requestTimeout, deadlineNs, responseFuture);
  }

  /** 重试公共出口：预算内调度重试（重新寻址），否则失败完结 future。 */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void retryOrFail(
          final BrokerRequest<Request> request,
          final boolean retry,
          final Duration requestTimeout,
          final long deadlineNs,
          final CompletableFuture<BrokerResponse<Response>> responseFuture,
          final Throwable failure) {
    if (!retry) {
      responseFuture.completeExceptionally(failure);
      return;
    }
    if (System.nanoTime() >= deadlineNs) {
      final var timeout =
          new TimeoutException(
              "Retry budget of "
                  + requestTimeout.toMillis()
                  + " ms exhausted, last failure: "
                  + failure.getMessage());
      timeout.initCause(failure);
      responseFuture.completeExceptionally(timeout);
      return;
    }
    // 重试前重新寻址（leader 可能已切换）
    actor.schedule(
        RETRY_DELAY,
        () -> resolveAndSend(request, retry, requestTimeout, deadlineNs, responseFuture));
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void encodeAndSend(
          final BrokerRequest<Request> request,
          final BrokerPlane.Destination destination,
          final boolean retry,
          final Duration requestTimeout,
          final long deadlineNs,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
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
    // 统一封装格式编码 byte[]
    final MutableDirectBuffer requestDataBuffer =
        new UnsafeBuffer(new byte[apiRequestWriter.getLength()]);
    apiRequestWriter.write(requestDataBuffer, 0);

    final RequestContent<Response> requestContent =
        new RequestContent<>(
            destination.apiAddress(),
            destination.topicName(),
            retry,
            requestTimeout,
            requestDataBuffer.byteArray(),
            responseFuture);
    sendInternal(request, retry, requestTimeout, deadlineNs, requestContent);
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void sendInternal(
          final BrokerRequest<Request> request,
          final boolean retry,
          final Duration requestTimeout,
          final long deadlineNs,
          final RequestContent<Response> requestContext) {
    messagingService
        .sendAndReceive(
            requestContext.apiAddress(),
            requestContext.topicName(),
            requestContext.data(),
            requestContext.requestTimeout())
        .whenComplete(
            (bytes, throwable) -> {
              if (throwable != null) {
                retryOrFail(
                    request,
                    retry,
                    requestTimeout,
                    deadlineNs,
                    requestContext.responseFuture(),
                    throwable);
              } else {
                handleResponse(requestContext.responseFuture(), bytes);
              }
            });
  }

  private <Response extends ResponseRecordValue> void handleResponse(
      final CompletableFuture<BrokerResponse<Response>> responseFuture, final byte[] bytes) {
    try {
      // 统一封装格式解码 + 面别物化
      final MutableDirectBuffer responseBuffer = new UnsafeBuffer(bytes);
      final ApiResponseReader responseReader = new ApiResponseReaderImpl();
      responseReader.wrap(responseBuffer, 0, responseBuffer.capacity());
      final Response value = (Response) plane.materialize(responseReader);
      responseFuture.complete(new BrokerResponse<>(responseReader, value));
    } catch (final Exception e) {
      // 解码失败以异常完结 future；此处运行在消息回调线程，抛出会打断传输层回调循环
      LOG.error("Failed to handle response", e);
      responseFuture.completeExceptionally(e);
    }
  }

  @Override
  public void close() {
    if (isClosed) {
      return;
    }

    isClosed = true;
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
