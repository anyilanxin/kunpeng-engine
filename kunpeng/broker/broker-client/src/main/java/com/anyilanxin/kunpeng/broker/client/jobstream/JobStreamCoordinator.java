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
package com.anyilanxin.kunpeng.broker.client.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.PushResult;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot.Session;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job 流协调器：按网关分桶持有订阅权威集，扁平化派生 jobType → 会话推送点列表（跨网关合并）只读视图，向引擎提供单游标轮转选点与非阻塞推送。
 *
 * <p>选点为单游标取模轮转——每个会话精确等份额；两级"先转网关再转会话"会向小网关系统性倾斜（网关间均分不随会话数加权），扁平索引一并消除该偏差。
 *
 * <p>订阅同步搭车 SWIM 元数据通道：网关把全量快照（携带单调递增代次）写入本地成员属性，集群按 metadataVersion 反熵扩散，broker 侧由成员监听方解析后调 {@link
 * #applySnapshot(MemberId, SubscriptionSnapshot)} 合并——旧代次迟到无害（忽略），合并=比代次取新，网关源权威无冲突；
 * 网关成员离线由集群成员监听方调 {@link #removeGateway(MemberId)} 摘桶。推送为请求-应答：应答即送达确认，失败/超时在本地经 {@code
 * failureHandler} 暴露给使用方（写 WITHDRAW 回退命令），不存在反向失败回报主题。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamCoordinator extends Actor {
  private static final Logger LOG = LoggerFactory.getLogger(JobStreamCoordinator.class);

  /** 推送出站队列深度上限：超限视为网关僵死，直接走失败回退（job 回 READY 可被其他消费者取走） */
  private static final int MAX_OUTBOUND_DEPTH = 8192;

  private static final Duration PUSH_TIMEOUT = Duration.ofSeconds(10);

  /** actor 线程独占：按网关分桶的订阅权威集 */
  private final Map<MemberId, GatewayBucket> buckets = new HashMap<>();

  /** 扁平化只读视图（actor 线程重建，引擎线程并发读）：jobType → 跨网关合并的会话推送点列表 */
  private volatile Map<String, List<PushPoint>> byType = Map.of();

  /** 每 jobType 轮转游标（引擎线程并发递增） */
  private final ConcurrentHashMap<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

  private final ManyToOneConcurrentLinkedQueue<PendingPush> outbound =
      new ManyToOneConcurrentLinkedQueue<>();
  private final AtomicInteger outboundDepth = new AtomicInteger();

  private final ClusterCommunicationService communication;

  private volatile BiConsumer<StreamPush, Throwable> failureHandler =
      (push, failure) -> LOG.warn("job {} 推送失败: {}", push.jobKey(), failure);

  public JobStreamCoordinator(final ClusterCommunicationService communication) {
    this.communication = communication;
  }

  public void setFailureHandler(final BiConsumer<StreamPush, Throwable> handler) {
    this.failureHandler = handler;
  }

  @Override
  public String getName() {
    return "job-stream-coordinator";
  }

  // ===== 订阅合并（actor 线程） =====

  /** 网关快照到达（SWIM 元数据通道解析后转入）：按代次幂等合并——旧代次忽略，新代次换桶重建视图。 */
  public void applySnapshot(final MemberId gateway, final SubscriptionSnapshot snapshot) {
    actor.run(
        () -> {
          final GatewayBucket existing = buckets.get(gateway);
          if (existing != null && snapshot.generation() <= existing.generation) {
            return; // 乱序迟到旧快照（反熵重拉等场景），无害忽略
          }

          final Map<String, List<Session>> sessionsByType = new HashMap<>();
          for (final SubscriptionSnapshot.Aggregate aggregate : snapshot.aggregates()) {
            final String jobType = aggregate.jobType();
            if (jobType == null || jobType.isEmpty()) {
              LOG.warn("网关 {} 聚合注册 jobType 为空, 忽略该聚合", gateway);
              continue;
            }
            if (aggregate.sessions() == null || aggregate.sessions().isEmpty()) {
              LOG.warn("网关 {} 聚合注册 sessions 为空, 忽略该聚合 [type: {}]", gateway, jobType);
              continue;
            }
            sessionsByType.put(jobType, List.copyOf(aggregate.sessions()));
          }

          buckets.put(gateway, new GatewayBucket(snapshot.generation(), sessionsByType));
          rebuildView();
        });
  }

  /** 网关成员离线：删桶重建视图（由集群成员监听方转发） */
  public void removeGateway(final MemberId gateway) {
    actor.run(
        () -> {
          if (buckets.remove(gateway) != null) {
            LOG.info("网关 {} 离开, 移除其订阅桶", gateway);
            rebuildView();
          }
        });
  }

  private void rebuildView() {
    final Map<String, List<PushPoint>> merged = new HashMap<>();
    for (final Map.Entry<MemberId, GatewayBucket> entry : buckets.entrySet()) {
      final String gateway = entry.getKey().id();
      entry
          .getValue()
          .sessionsByType()
          .forEach(
              (jobType, sessions) -> {
                final List<PushPoint> points =
                    merged.computeIfAbsent(jobType, key -> new ArrayList<>(sessions.size()));
                for (final Session session : sessions) {
                  points.add(new PushPoint(gateway, session.sessionId(), session.worker()));
                }
              });
    }
    byType = Map.copyOf(merged);
  }

  // ===== 引擎线程调用（无锁读 + 非阻塞投递） =====

  /**
   * 单游标轮转选点：jobType 直接索引到跨网关合并的扁平会话列表，一次取模定位。 选定即绑定归属（会话的 worker）——引擎侧按它落 lockOwner，推送时随载荷下发同一会话 ID
   * 供网关 map O(1) 定位。
   */
  public Optional<PushPoint> select(final String jobType) {
    final List<PushPoint> points = byType.get(jobType);
    if (points == null || points.isEmpty()) {
      return Optional.empty();
    }
    final int index =
        Math.floorMod(
            cursors.computeIfAbsent(jobType, key -> new AtomicInteger()).getAndIncrement(),
            points.size());
    return Optional.of(points.get(index));
  }

  /** 非阻塞投递（引擎副作用线程）；出站队列超限直接走失败处理 */
  public void enqueue(final String gatewayMemberId, final StreamPush push) {
    if (outboundDepth.incrementAndGet() > MAX_OUTBOUND_DEPTH) {
      outboundDepth.decrementAndGet();
      failureHandler.accept(
          push, new IllegalStateException("推送出站队列已满(> " + MAX_OUTBOUND_DEPTH + ")"));
      return;
    }
    outbound.offer(new PendingPush(gatewayMemberId, push));
    actor.run(this::drainOutbound);
  }

  // ===== actor 线程 =====

  private void drainOutbound() {
    PendingPush pending;
    while ((pending = outbound.poll()) != null) {
      outboundDepth.decrementAndGet();
      send(pending);
    }
  }

  private void send(final PendingPush pending) {
    final CompletableFuture<PushResult> reply =
        communication.send(
            JobStreamSubjects.PUSH,
            pending.push(),
            JobStreamWireCodec::encodePush,
            JobStreamWireCodec::decodeResult,
            MemberId.from(pending.gatewayMemberId()),
            PUSH_TIMEOUT);
    reply.whenComplete(
        (result, error) -> {
          if (error != null) {
            fail(pending, error);
          } else if (result == null || !result.delivered()) {
            fail(pending, new IllegalStateException(result == null ? "无应答" : result.reason()));
          }
        });
  }

  private void fail(final PendingPush pending, final Throwable failure) {
    LOG.debug(
        "job {} 推送未送达 [gateway: {}, worker: {}]",
        pending.push().jobKey(),
        pending.gatewayMemberId(),
        pending.push().worker());
    failureHandler.accept(pending.push(), failure);
  }

  /** 推送点（也是 byType 扁平索引的条目形态）：目标网关 + 稳定会话 ID + worker（引擎落 lockOwner 的归属） */
  public record PushPoint(String gatewayMemberId, long sessionId, String worker) {}

  /** 单条在途推送（目标网关 + 原始载荷） */
  private record PendingPush(String gatewayMemberId, StreamPush push) {}

  /** 网关订阅桶（actor 线程独占）：每 jobType 一条会话列表 */
  private record GatewayBucket(long generation, Map<String, List<Session>> sessionsByType) {}
}
