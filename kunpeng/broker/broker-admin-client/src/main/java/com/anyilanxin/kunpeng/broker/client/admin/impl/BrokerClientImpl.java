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
package com.anyilanxin.kunpeng.broker.client.admin.impl;

import com.anyilanxin.kunpeng.broker.client.admin.*;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端实现
 *
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerClientImpl extends Actor implements BrokerClient {
  private static final Logger LOG = LoggerFactory.getLogger(BrokerClientImpl.class);
  private final MessagingService messagingService;
  private final ClusterLeaderFoundService leaderFoundService;
  private static final Duration RETRY_DELAY = Duration.ofMillis(10);
  private final Duration requestTimeout;
  private boolean isClosed;

  /**
   * 可复用的工作缓冲区。由于 {@link BrokerClientImpl} 是 {@link Actor}（单线程执行），且缓冲区在 请求被异步分发之前，已于同步的 {@code
   * value.write(...)} 调用中被完整消费，因此可以安全复用。
   */
  private byte[] reusableValueBytes = new byte[0];

  private final UnsafeBuffer reusableValueBuffer = new UnsafeBuffer(0, 0);

  public BrokerClientImpl(
      final ClusterLeaderFoundService leaderFoundService,
      final MessagingService messagingService,
      final Duration requestTimeout) {
    this.messagingService = messagingService;
    this.requestTimeout = requestTimeout;
    this.leaderFoundService = leaderFoundService;
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

  public <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void sendRequest(
          final BrokerRequest<Request> request,
          final Duration requestTimeout,
          final boolean retry,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    // 命令相关接口
    final Address address = leaderFoundService.getLeaderAddress();
    if (address == null) {
      responseFuture.completeExceptionally(new BrokerException(10, "not found leader"));
      return;
    }
    actor.run(() -> encodeAndSend(request, address, retry, requestTimeout, responseFuture));
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void encodeAndSend(
          final BrokerRequest<Request> request,
          final Address address,
          final boolean retry,
          final Duration requestTimeout,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    // 统一请求格式
    final ApiRequestWriterImpl apiRequestWriter = new ApiRequestWriterImpl();
    apiRequestWriter.key(request.key());
    apiRequestWriter.valueType(request.valueType());
    apiRequestWriter.lifeCycle(request.lifeCycle());
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
    // 统一请求格式编码 byte[]
    final MutableDirectBuffer requestDataBuffer =
        new UnsafeBuffer(new byte[apiRequestWriter.getLength()]);
    apiRequestWriter.write(requestDataBuffer, 0);
    Duration currentRequestTimeout = requestTimeout;
    if (currentRequestTimeout == null) {
      currentRequestTimeout = this.requestTimeout;
    }
    final RequestContent<Response> requestContent =
        new RequestContent<>(
            address,
            TopicUtils.getTopicName(),
            retry,
            currentRequestTimeout,
            requestDataBuffer.byteArray(),
            responseFuture);
    sendInternal(requestContent);
  }

  private <Response extends ResponseRecordValue> void sendInternal(
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
                if (requestContext.shouldRetry()) {
                  actor.schedule(RETRY_DELAY, () -> sendInternal(requestContext));
                } else {
                  requestContext.responseFuture().completeExceptionally(throwable);
                }
              } else {
                handleResponse(requestContext.responseFuture(), bytes);
              }
            });
  }

  private <Response extends ResponseRecordValue> void handleResponse(
      final CompletableFuture<BrokerResponse<Response>> responseFuture, final byte[] bytes) {
    try {
      // 统一响应格式解码
      final MutableDirectBuffer responseBuffer = new UnsafeBuffer(bytes);
      final ApiResponseReaderImpl responseReader = new ApiResponseReaderImpl();
      responseReader.wrap(responseBuffer, 0, responseBuffer.capacity());
      responseFuture.complete(new BrokerResponse<>(responseReader));
    } catch (final Exception e) {
      LOG.error("Failed to handle response", e);
      throw new RuntimeException("Failed to handle response", e);
    }
  }

  @Override
  public void close() {
    if (isClosed) {
      return;
    }
    isClosed = true;
  }
}
