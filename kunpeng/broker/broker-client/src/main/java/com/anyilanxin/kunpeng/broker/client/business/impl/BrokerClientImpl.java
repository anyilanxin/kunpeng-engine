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
package com.anyilanxin.kunpeng.broker.client.business.impl;

import static com.anyilanxin.kunpeng.broker.client.business.ClientRequest.NOT_SPECIFIED_PARTITION;
import static com.anyilanxin.kunpeng.broker.client.business.ClientRequest.RANDOM_PARTITION;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_GROUP;

import com.anyilanxin.kunpeng.broker.client.business.*;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.BusinessMessaging;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.Subscription;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端实现
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BrokerClientImpl extends Actor implements BrokerClient {
  private static final Logger LOG = LoggerFactory.getLogger(BrokerClientImpl.class);
  private final MessagingService messagingService;
  private final ClusterTopologyService topologyService;
  private final ClusterMembershipService membershipService;
  private final ClusterLeaderFoundService leaderFoundService;
  private final BrokerTopologyManager topologyManager;
  private static final Duration RETRY_DELAY = Duration.ofMillis(10);

  /** 远端成员业务地址未就绪时的重解析次数与间隔(有界防打转); 覆盖成员发现与元数据拉取(默认抖动 500ms)间的启动窗口。 */
  private static final int ADDRESS_RESOLVE_ATTEMPTS = 20;

  private static final Duration ADDRESS_RESOLVE_RETRY_DELAY = Duration.ofMillis(100);
  private final Duration requestTimeout;
  private Subscription jobAvailableSubscription;
  private boolean isClosed;
  private final ClusterEventService eventService;

  /**
   * Reusable work buffers. Safe to reuse because {@link BrokerClientImpl} is an {@link Actor}
   * (single-threaded execution) and the buffer is fully consumed during the synchronous {@code
   * value.write(...)} call before the request is dispatched asynchronously.
   */
  private byte[] reusableValueBytes = new byte[0];

  private final UnsafeBuffer reusableValueBuffer = new UnsafeBuffer(0, 0);

  public BrokerClientImpl(
      final MessagingService messagingService,
      final ClusterEventService eventService,
      final ClusterTopologyService topologyService,
      final ClusterMembershipService membershipService,
      final ClusterLeaderFoundService leaderFoundService,
      final Duration requestTimeout) {
    this.messagingService = messagingService;
    this.eventService = eventService;
    this.topologyService = topologyService;
    this.membershipService = membershipService;
    this.leaderFoundService = leaderFoundService;
    this.requestTimeout = requestTimeout;
    topologyManager = new BrokerTopologyManager(topologyService);
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
  public ClusterTopologyService getTopologyService() {
    return topologyService;
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
    final RecordType requestType = request.requestType();
    // 寻址与编码都必须在 actor 线程内执行：sendRequest 由多个 gRPC worker 线程并发调用，
    // 而 reusableValueBytes/Buffer 是实例字段。在 actor.run 内串行化，
    // 既消除竞态，又能让池化跨请求真正安全生效；地址暂不可得(元数据未拉取)时可在 actor 上延迟重解析。
    actor.run(
        () ->
            resolveAddressAndSend(
                request,
                requestType,
                retry,
                requestTimeout,
                responseFuture,
                ADDRESS_RESOLVE_ATTEMPTS));
  }

  /**
   * 在 actor 线程上解析目标地址并派发请求。
   *
   * <p>分区 leader 为远端成员且其业务端口属性尚未随全量元数据拉取落定时，{@link #partitionAddress} 返回 null 且不完结 future——此处按
   * {@link #ADDRESS_RESOLVE_RETRY_DELAY} 有界重解析，拉取落定即恢复派发， 避免成员发现与元数据拉取之间的启动窗口把请求直接打失败。
   */
  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void resolveAddressAndSend(
          final BrokerRequest<Request> request,
          final RecordType requestType,
          final boolean retry,
          final Duration requestTimeout,
          final CompletableFuture<BrokerResponse<Response>> responseFuture,
          final int attemptsLeft) {
    final SendAddress address;
    // 命令相关接口
    switch (requestType) {
      case COMMAND_API -> address = getCommandApiAddress(request, responseFuture);
      case QUERY -> address = getQueryAddress(request, responseFuture);
      default -> {
        responseFuture.completeExceptionally(new BrokerException(10, "Unexpected request type"));
        return;
      }
    }
    if (address == null) {
      // future 已完结说明上游已给出明确失败(无分区/无 leader 等)；未完结则只是地址暂不可得, 有界重解析
      if (responseFuture.isDone()) {
        return;
      }
      if (attemptsLeft > 0) {
        actor.schedule(
            ADDRESS_RESOLVE_RETRY_DELAY,
            () ->
                resolveAddressAndSend(
                    request, requestType, retry, requestTimeout, responseFuture, attemptsLeft - 1));
        return;
      }
      responseFuture.completeExceptionally(
          new BrokerException(
              10,
              "Business messaging address of partition leader not resolvable within retry budget"
                  + " (member metadata not pulled); retry later"));
      return;
    }
    encodeAndSend(request, requestType, address, retry, requestTimeout, responseFuture);
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      void encodeAndSend(
          final BrokerRequest<Request> request,
          final RecordType requestType,
          final SendAddress address,
          final boolean retry,
          final Duration requestTimeout,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    // 统一请求格式
    final ApiRequestWriterImpl apiRequestWriter = new ApiRequestWriterImpl();
    apiRequestWriter.requestType(request.requestType());
    apiRequestWriter.key(request.key());
    apiRequestWriter.valueType(request.valueType());
    apiRequestWriter.lifeCycle(request.lifeCycle());
    final Request value = request.getValue();
    if (value != null) {
      final int valueLength = value.getLength();
      // Reuse the work buffer — grows on demand, never shrinks (high-water mark).
      // Safe: this method runs only on the actor thread (single-threaded).
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

    final RequestContent<Response> requestContent =
        new RequestContent<>(
            requestType,
            address.requestTypeResourceId(),
            address.apiAddress(),
            TopicUtils.getTopicName(requestType, address.requestTypeResourceId()),
            retry,
            requestTimeout,
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
    if (jobAvailableSubscription != null) {
      jobAvailableSubscription.close();
    }
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

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      SendAddress getCommandApiAddress(
          final BrokerRequest<Request> request,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    final int partitionId = request.partitionId();
    if (partitionId == RANDOM_PARTITION) {
      final PartitionId partition = randomPartition();
      if (partition == null) {
        responseFuture.completeExceptionally(
            new BrokerException(10, "No business partition with a leader is available"));
        return null;
      }
      return partitionAddress(partition, responseFuture);
    } else if (partitionId == NOT_SPECIFIED_PARTITION) {
      final long key = request.key();
      if (key == -1) {
        responseFuture.completeExceptionally(
            new BrokerException(10, "Invalid key or no target partition specified"));
        return null;
      }
      final int sourceId = Protocol.decodeResourceId(key);
      final PartitionId partition = partitionBySource(sourceId);
      if (partition == null) {
        responseFuture.completeExceptionally(
            new BrokerException(10, "No partition found for source " + sourceId));
        return null;
      }
      return partitionAddress(partition, responseFuture);
    }
    return partitionAddress(PartitionId.from(BUSINESS_RAFT_GROUP, partitionId), responseFuture);
  }

  private <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      SendAddress getQueryAddress(
          final BrokerRequest<Request> request,
          final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    final int partitionId = request.partitionId();
    if (partitionId <= 0) {
      responseFuture.completeExceptionally(new BrokerException(10, "No valid partition specified"));
      return null;
    }
    return partitionAddress(PartitionId.from(BUSINESS_RAFT_GROUP, partitionId), responseFuture);
  }

  private <Response extends ResponseRecordValue> SendAddress partitionAddress(
      final PartitionId partitionId,
      final CompletableFuture<BrokerResponse<Response>> responseFuture) {
    final MemberId leader = topologyService.getPartitionLeader(partitionId);
    if (leader == null) {
      responseFuture.completeExceptionally(
          new BrokerException(10, "No leader found for partition " + partitionId));
      return null;
    }
    // 本节点即 leader: 直读本地成员活属性(业务端口由本进程启动步骤写入, 成员表与其共享同一张属性表),
    // 不依赖 SWIM 全量元数据二次拉取——本节点数据本地即权威
    if (leader.equals(membershipService.getLocalMember().id())) {
      final Address localAddress =
          BusinessMessaging.businessAddressOf(membershipService.getLocalMember());
      if (localAddress == null) {
        responseFuture.completeExceptionally(
            new BrokerException(
                10,
                "Local business messaging port not advertised for partition "
                    + partitionId
                    + "; check business messaging startup"));
        return null;
      }
      return new SendAddress(String.valueOf(partitionId.id()), localAddress);
    }
    final Member member = membershipService.getMember(leader);
    if (member == null) {
      responseFuture.completeExceptionally(
          new BrokerException(10, "No address found for partition leader " + leader.id()));
      return null;
    }
    // 业务面寻址: 成员主机 + 其广播的业务端口(随 SWIM 全量元数据二次拉取补全);
    // 拉取未落定时返回 null 且不完结 future, 由 resolveAddressAndSend 有界重解析
    final Address businessAddress = BusinessMessaging.businessAddressOf(member);
    if (businessAddress == null) {
      return null;
    }
    return new SendAddress(String.valueOf(partitionId.id()), businessAddress);
  }

  private PartitionId partitionBySource(final int sourceId) {
    for (final var entry : topologyService.getRaftGroup(BUSINESS_RAFT_GROUP).entrySet()) {
      for (final var memberInfo : entry.getValue()) {
        if (memberInfo.getSourceId() == sourceId) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  private PartitionId randomPartition() {
    final List<PartitionId> candidates = new ArrayList<>();
    for (final PartitionId partitionId :
        topologyService.getRaftGroup(BUSINESS_RAFT_GROUP).keySet()) {
      if (topologyService.getPartitionLeader(partitionId) != null) {
        candidates.add(partitionId);
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
  }

  record SendAddress(String requestTypeResourceId, Address apiAddress) {}
}
