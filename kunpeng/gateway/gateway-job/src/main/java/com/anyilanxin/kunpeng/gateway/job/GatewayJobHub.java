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
package com.anyilanxin.kunpeng.gateway.job;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobBatchActivateRequest;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.JobDelivery;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.PullRequest;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.PullResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate.JobInfoRecordValue;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.util.DocumentUtil;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;

/**
 * gateway 本地 job 中枢：客户端流注册表（订阅快照的唯一事实源）+ 长轮询挂起表。
 *
 * <p>拉取（PullJobs 长轮询）经标准命令通道（{@link BrokerClient} + JobBatchActivateRequest）向全部分区探测，
 * 首个非空响应胜出；全空则挂起，被 job-ready 广播唤醒后重试，挂起超时返回空批。完成/失败不经本中枢——client 直接调 CompleteJob/FailJob 命令 rpc。
 *
 * <p>推送（broker job-stream-push 到达）按稳定会话 ID map O(1) 定位流、同 worker 兜底、跨 worker 不凑合（返回未送达，broker 侧写
 * WITHDRAW 回退命令）；订阅以聚合形态对账（每 jobType 一条，workers 数组即水位线），broker 视图与消费者数量解耦。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayJobHub {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_JOB;
  private static final long DEFAULT_LONG_POLL_MILLIS = 30_000;
  private static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 60_000;

  private final ConcurrentHashMap<Long, DeliveryStream> streams = new ConcurrentHashMap<>();

  /** jobType → 会话表（稳定会话 ID → 投递流）：流下线只删自身项，其他会话寻址不受影响 */
  private final ConcurrentHashMap<String, ConcurrentHashMap<Long, DeliveryStream>> streamsByType =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingPull>> pendingPulls =
      new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;
  private final AtomicLong streamIdGenerator = new AtomicLong();
  private final AtomicLong generation = new AtomicLong();
  private final long longPollDefaultMillis;

  private volatile BrokerClient brokerClient;

  /** 流注册变化回调（由传输层注册：立即重发订阅快照） */
  private volatile Runnable snapshotChanged = () -> {};

  public GatewayJobHub(final ScheduledExecutorService scheduler, final long longPollDefaultMillis) {
    this.scheduler = scheduler;
    this.longPollDefaultMillis = longPollDefaultMillis;
  }

  public void setBrokerClient(final BrokerClient client) {
    brokerClient = client;
  }

  public void setSnapshotChanged(final Runnable callback) {
    snapshotChanged = callback;
  }

  // ———————— STREAM（聚合订阅快照事实源） ————————

  /**
   * OpenJobStream 首个请求：注册为一条流（分配网关内稳定会话 ID）。 worker 空则兜底生成 `stream-{streamId}`（防御裸 gRPC/非 Java
   * client），保证落库 lockOwner 永不为空。
   *
   * @return 分配的 streamId（gateway 内部编号，deregister 寻址用，对 broker 透明）
   */
  public long registerStream(
      final String jobType,
      final String worker,
      final int capacity,
      final StreamObserver<JobDelivery> observer) {
    final long streamId = streamIdGenerator.incrementAndGet();
    final String workerName = (worker == null || worker.isEmpty()) ? "stream-" + streamId : worker;
    final DeliveryStream handle =
        new DeliveryStream(streamId, jobType, workerName, capacity, observer);
    streams.put(streamId, handle);
    streamsByType.computeIfAbsent(jobType, key -> new ConcurrentHashMap<>()).put(streamId, handle);
    generation.incrementAndGet();
    snapshotChanged.run();
    LOG.info(
        "Job stream registered [type: {}, worker: {}, stream: {}]", jobType, workerName, streamId);
    return streamId;
  }

  /** 流关闭（client 断开/完成/出错）：摘除会话（只删自身项、快照重发），未送达的 job 由 broker WITHDRAW 回退重派 */
  public void deregisterStream(final long streamId, final String cause) {
    final DeliveryStream handle = streams.remove(streamId);
    if (handle == null) {
      return;
    }
    handle.close();
    final ConcurrentHashMap<Long, DeliveryStream> sessions = streamsByType.get(handle.jobType());
    if (sessions != null) {
      sessions.remove(streamId);
    }
    generation.incrementAndGet();
    snapshotChanged.run();
    LOG.info(
        "Job stream deregistered [type: {}, stream: {}, cause: {}]",
        handle.jobType(),
        streamId,
        cause);
  }

  /**
   * 当前聚合订阅快照：每 jobType 一条聚合，sessions 为稳定会话 ID → worker 映射（broker 视图与推送扇出为
   * O(type)，与消费者数量解耦）。代次仅在流集变化时推进——周期重发携带相同代次，broker 幂等忽略。
   */
  public SubscriptionSnapshot snapshot() {
    final List<SubscriptionSnapshot.Aggregate> aggregates = new ArrayList<>();
    streamsByType.forEach(
        (jobType, sessions) -> {
          if (sessions.isEmpty()) {
            return;
          }
          final List<SubscriptionSnapshot.Session> members = new ArrayList<>(sessions.size());
          sessions.forEach(
              (sessionId, handle) ->
                  members.add(new SubscriptionSnapshot.Session(sessionId, handle.worker())));
          aggregates.add(new SubscriptionSnapshot.Aggregate(jobType, members));
        });
    return new SubscriptionSnapshot(generation.get(), aggregates);
  }

  /**
   * broker 推送到达，三规则分发（归属精确性：引擎已按 {@code push.worker()} 落 lockOwner，只投同 worker 的流）： ①会话直达——稳定会话 ID
   * map O(1) 定位（对账窗口内会话已下线则查无）；②同 worker 兜底——遍历该类型下同 worker 的其他流；③跨 worker 不凑合——宁可返回 false（broker
   * WITHDRAW 重派）也不错投归属。
   */
  public boolean deliverPush(final StreamPush push) {
    final ConcurrentHashMap<Long, DeliveryStream> sessions =
        streamsByType.get(push.record().getJobType());
    if (sessions == null || sessions.isEmpty()) {
      return false;
    }
    final DeliveryStream handle = sessions.get(push.sessionId());
    if (handle != null
        && handle.worker().equals(push.worker())
        && handle.tryDeliver(toDelivery(push, handle))) {
      return true;
    }
    for (final DeliveryStream candidate : sessions.values()) {
      if (candidate.worker().equals(push.worker())
          && candidate.tryDeliver(toDelivery(push, candidate))) {
        return true;
      }
    }
    LOG.debug(
        "Push not deliverable, no eligible stream [type: {}, worker: {}, session: {}]",
        push.record().getJobType(),
        push.worker(),
        push.sessionId());
    return false;
  }

  // ———————— PULL（长轮询，标准命令通道） ————————

  /** gRPC PullJobs：立即探测全部分区，全空挂起等待唤醒；挂起超时返回空批（client 立即重挂）。 */
  public void pullJobs(final PullRequest request, final StreamObserver<PullResponse> observer) {
    final long deadlineMillis =
        request.getDeadlineMs() > 0 ? request.getDeadlineMs() : longPollDefaultMillis;
    final PendingPull pending = new PendingPull(request, observer);
    pending.beginProbe();
    probe(pending);
    scheduler.schedule(
        () -> {
          if (pending.setDone()) {
            respond(pending, List.of());
          }
        },
        deadlineMillis,
        TimeUnit.MILLISECONDS);
  }

  /** job-ready 广播到达：唤醒该类型队首挂起请求（探测落空自动挂回） */
  public void onJobsAvailable(final String jobType) {
    final ConcurrentLinkedQueue<PendingPull> queue = pendingPulls.get(jobType);
    if (queue == null) {
      return;
    }
    PendingPull pending;
    while ((pending = queue.poll()) != null) {
      if (pending.beginProbe()) {
        probe(pending);
        return;
      }
    }
  }

  /** 向全部分区发激活命令；首个非空响应胜出，全空挂回队列 */
  private void probe(final PendingPull pending) {
    final BrokerClient client = brokerClient;
    if (client == null) {
      repark(pending);
      return;
    }
    final var topology = client.getTopologyManager().getTopology();
    final var partitions =
        topology == null
            ? List.<com.anyilanxin.kunpeng.cluster.cluster.PartitionId>of()
            : List.copyOf(topology.getPartitions());
    if (partitions.isEmpty()) {
      repark(pending);
      return;
    }

    final PullRequest request = pending.request();
    final long lockTimeout =
        request.getDeadlineMs() > 0 ? request.getDeadlineMs() : DEFAULT_LOCK_TIMEOUT_MILLIS;
    final var pendingReplies = new java.util.concurrent.atomic.AtomicInteger(partitions.size());
    for (final var partitionId : partitions) {
      final JobBatchActivateRequest activate =
          new JobBatchActivateRequest(request.getType())
              .setWorker(request.getWorker())
              .setMaxJobsToActivate(
                  request.getMaxBatch() <= 0 ? Integer.MAX_VALUE : request.getMaxBatch())
              .setTimeout(lockTimeout);
      activate.setPartitionId(partitionId.id());
      if (!request.getTenantIdsList().isEmpty()) {
        activate.setTenantIds(request.getTenantIdsList());
      }
      client
          .<com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate
                  .JobBatchActivateRequestRecord,
              com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate
                  .JobBatchActivateResponseRecord>
              sendRequest(activate)
          .whenComplete(
              (response, error) -> {
                final List<JobInfoRecordValue> jobs =
                    (error == null && response != null && response.isSuccess())
                        ? response.getValue().getJobs()
                        : List.of();
                final List<Long> jobKeys =
                    (error == null && response != null && response.isSuccess())
                        ? response.getValue().getJobKeys()
                        : List.of();
                if (!jobs.isEmpty()) {
                  deliverProbeResult(pending.request().getType(), pending, jobs, jobKeys);
                }
                if (pendingReplies.decrementAndGet() == 0) {
                  // 全部分区落空（或仅空响应）：挂回等待下一次唤醒/超时
                  repark(pending);
                }
              });
    }
  }

  /** 首个非空响应胜出；请求已超时则把 job 转交同类型其他挂起请求，无人可接则丢弃（分区到期自愈） */
  private void deliverProbeResult(
      final String jobType,
      final PendingPull source,
      final List<JobInfoRecordValue> jobs,
      final List<Long> jobKeys) {
    if (source.setDone()) {
      respond(source, deliveries(jobs, jobKeys));
      return;
    }
    final ConcurrentLinkedQueue<PendingPull> queue = pendingPulls.get(jobType);
    PendingPull next = queue == null ? null : queue.poll();
    while (next != null && !next.setDone()) {
      next = queue.poll();
    }
    if (next != null) {
      LOG.debug(
          "Probe result handed to next parked request [type: {}, jobs: {}]", jobType, jobs.size());
      respond(next, deliveries(jobs, jobKeys));
    } else {
      LOG.warn(
          "Probe result arrived with no waiting request, jobs await deadline disposal [type: {}, jobs: {}]",
          jobType,
          jobs.size());
    }
  }

  /** 挂回队列（保持 QUEUED 状态，等待唤醒/超时）；状态已被取走则丢弃 */
  private void repark(final PendingPull pending) {
    if (pending.repark()) {
      pendingPulls
          .computeIfAbsent(pending.request().getType(), k -> new ConcurrentLinkedQueue<>())
          .add(pending);
    }
  }

  private void respond(final PendingPull pending, final List<JobDelivery> jobs) {
    final PullResponse.Builder builder = PullResponse.newBuilder();
    builder.addAllJobs(jobs);
    try {
      pending.observer().onNext(builder.build());
      pending.observer().onCompleted();
    } catch (final Exception e) {
      LOG.debug("Long-poll response failed (client gone?)", e);
    }
  }

  public int streamCount() {
    return streams.size();
  }

  public int pendingPullCount() {
    return pendingPulls.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
  }

  // ———————— gRPC 投递映射 ————————

  private static List<JobDelivery> deliveries(
      final List<JobInfoRecordValue> jobs, final List<Long> jobKeys) {
    final List<JobDelivery> out = new ArrayList<>(jobs.size());
    for (int i = 0; i < jobs.size(); i++) {
      out.add(toDelivery(jobs.get(i), jobKeys.get(i)));
    }
    return out;
  }

  /** 拉取响应 → 投递 */
  static JobDelivery toDelivery(final JobInfoRecordValue info, final long jobKey) {
    return JobDelivery.newBuilder()
        .setJobKey(jobKey)
        .setType(orEmpty(info.getJobType()))
        .setTenantId(orEmpty(info.getTenantId()))
        .setProcessInstanceId(info.getProcessInstanceId())
        .setElementId(orEmpty(info.getActivityDefinitionKey()))
        .setWorker(orEmpty(info.getWorker()))
        .setRetries(info.getRetries())
        .setDeadline(info.getDeadline())
        .setActivityInstanceId(info.getActivityInstanceId())
        .setJobKind(info.getJobKind() == null ? -1 : info.getJobKind().ordinal())
        .setVariables(jsonBytes(info.getVariablesBuffer()))
        .build();
  }

  /** 流推送 → 投递（完整 JobRecord 随行） */
  static JobDelivery toDelivery(final StreamPush push, final DeliveryStream handle) {
    final JobRecord record = push.record();
    return JobDelivery.newBuilder()
        .setJobKey(push.jobKey())
        .setType(orEmpty(record.getJobType()))
        .setTenantId(orEmpty(BufferUtil.bufferAsString(record.getTenantIdBuffer())))
        .setProcessInstanceId(record.getProcessInstanceId())
        .setElementId(orEmpty(BufferUtil.bufferAsString(record.getActivityDefinitionKeyBuffer())))
        // 归属权威：引擎选定并落 lockOwner 的 worker（与实际投递流一致——分发三规则保证只投同 worker）
        .setWorker(orEmpty(push.worker()))
        .setRetries(record.getRetries())
        .setDeadline(push.deadline())
        .setActivityInstanceId(record.getActivityInstanceId())
        .setJobKind(record.getJobKind() == null ? -1 : record.getJobKind().ordinal())
        .setVariables(jsonBytes(record.getVariablesBuffer()))
        .build();
  }

  private static String orEmpty(final String s) {
    return s == null ? "" : s;
  }

  private static ByteString jsonBytes(final DirectBuffer msgpack) {
    if (msgpack == null) {
      return ByteString.EMPTY;
    }
    final String json = DocumentUtil.convertToJson(msgpack);
    return json == null || json.isEmpty()
        ? ByteString.EMPTY
        : ByteString.copyFrom(json.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 单个挂起的长轮询请求。状态机：QUEUED（在队列中）→ PROBING（探测在途）→ QUEUED（落空挂回）/ DONE（已响应）； {@code
   * setDone/beginProbe/repark} 的 CAS 保证一次响应（唤醒、超时、探测结果三方竞态下唯一胜出）。
   */
  private static final class PendingPull {
    private final PullRequest request;
    private final StreamObserver<PullResponse> observer;
    private final AtomicReference<PullState> state = new AtomicReference<>(PullState.QUEUED);

    private PendingPull(final PullRequest request, final StreamObserver<PullResponse> observer) {
      this.request = request;
      this.observer = observer;
    }

    PullRequest request() {
      return request;
    }

    StreamObserver<PullResponse> observer() {
      return observer;
    }

    /** 响应胜出（QUEUED/PROBING → DONE） */
    boolean setDone() {
      return state.getAndSet(PullState.DONE) != PullState.DONE;
    }

    /** 开始探测（QUEUED → PROBING）；非 QUEUED（已完成）返回 false */
    boolean beginProbe() {
      return state.compareAndSet(PullState.QUEUED, PullState.PROBING);
    }

    /** 落空挂回（PROBING → QUEUED）；已完成返回 false */
    boolean repark() {
      return state.compareAndSet(PullState.PROBING, PullState.QUEUED);
    }
  }

  private enum PullState {
    QUEUED,
    PROBING,
    DONE
  }
}
