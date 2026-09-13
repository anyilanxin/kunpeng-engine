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
package com.anyilanxin.kunpeng.cluster.dispatch.api;

import static com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer.SERIALIZER;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;
import static com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType.getTopic;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.CLUSTER_DISPATCH_TOPIC_ACK;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.NODE_SOURCE_TOPIC;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionExecutionAckRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class DefaultClusterDispatchClient extends Actor implements ClusterDispatchClient {
  /** 发送重试次数上限，目标成员重启或网络抖动时兜底 */
  private static final int SEND_RETRY_LIMIT = 5;

  /** 发送重试间隔 */
  private static final Duration SEND_RETRY_DELAY = Duration.ofSeconds(2);

  private final ClusterLeaderFoundService leaderFoundService;
  private final ClusterMembershipService membershipService;
  private final MessagingService messagingService;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public DefaultClusterDispatchClient(
      final ClusterLeaderFoundService leaderFoundService,
      final ClusterMembershipService membershipService,
      final MessagingService messagingService) {
    this.leaderFoundService = leaderFoundService;
    this.membershipService = membershipService;
    this.messagingService = messagingService;
  }

  @Override
  public ActorFuture<Void> send(
      final PartitionExecutionRecordValue recordValue, final byte[] encode) {
    final String topic = getTopic(recordValue.getPartitionType(), recordValue.getExecutionType());
    final MemberId memberId = MemberId.from(recordValue.executionMemberId());
    LOGGER.info(
        "send dispatch memberId:{},partition type:{},execution type:{}",
        memberId,
        recordValue.getPartitionType(),
        recordValue.getExecutionType());
    final ActorFuture<Void> future = actor.createFuture();
    sendWithRetry(memberId, topic, encode, 0, future);
    return future;
  }

  @Override
  public ActorFuture<Void> send(final MemberId memberId, final int nodeUniqueId) {
    final ActorFuture<Void> future = actor.createFuture();
    sendWithRetry(memberId, NODE_SOURCE_TOPIC, SERIALIZER.encode(nodeUniqueId), 0, future);
    return future;
  }

  @Override
  public ActorFuture<Void> ack(final PartitionExecutionAckRecord data) {
    final byte[] encode = encode(data);
    final ActorFuture<Void> future = actor.createFuture();
    ackWithRetry(encode, 0, future);
    return future;
  }

  /** 目标成员可能暂时离线（重启/网络抖动），带退避重试，超限后放弃并保留错误日志 */
  private void sendWithRetry(
      final MemberId memberId,
      final String topic,
      final byte[] payload,
      final int attempt,
      final ActorFuture<Void> result) {
    final Member member = membershipService.getMember(memberId);
    if (member == null) {
      retryOrGiveUp(
          attempt,
          result,
          "Send dispatch to member {} failed, member not in cluster",
          memberId,
          "send dispatch to member " + memberId,
          () -> sendWithRetry(memberId, topic, payload, attempt + 1, result));
      return;
    }
    messagingService
        .sendAsync(member.address(), topic, payload)
        .whenComplete(
            (_, throwable) -> {
              if (throwable == null) {
                result.complete(null);
                return;
              }
              retryOrGiveUp(
                  attempt,
                  result,
                  "Send dispatch to member {} failed",
                  memberId,
                  "send dispatch to member " + memberId,
                  () -> sendWithRetry(memberId, topic, payload, attempt + 1, result));
            });
  }

  /** Leader 可能尚未选出或正在切换，带退避重试 */
  private void ackWithRetry(
      final byte[] payload, final int attempt, final ActorFuture<Void> result) {
    final Address leaderAddress = leaderFoundService.getLeaderAddress();
    if (leaderAddress == null) {
      retryOrGiveUp(
          attempt,
          result,
          "Ack dispatch failed, leader address not found",
          null,
          "ack dispatch",
          () -> ackWithRetry(payload, attempt + 1, result));
      return;
    }
    messagingService
        .sendAsync(leaderAddress, CLUSTER_DISPATCH_TOPIC_ACK, payload)
        .whenComplete(
            (_, throwable) -> {
              if (throwable == null) {
                result.complete(null);
                return;
              }
              retryOrGiveUp(
                  attempt,
                  result,
                  "Ack dispatch failed",
                  null,
                  "ack dispatch",
                  () -> ackWithRetry(payload, attempt + 1, result));
            });
  }

  /** 未达重试上限则延迟重试（重新进入带空值守卫的重试方法），否则放弃并完成 future */
  private void retryOrGiveUp(
      final int attempt,
      final ActorFuture<Void> result,
      final String warnFormat,
      final MemberId memberId,
      final String action,
      final Runnable resend) {
    if (attempt + 1 >= SEND_RETRY_LIMIT) {
      if (memberId != null) {
        LOGGER.error(
            warnFormat + " after {} attempts, giving up",
            memberId,
            SEND_RETRY_LIMIT,
            new IllegalStateException("retry limit reached: " + action));
      } else {
        LOGGER.error(
            warnFormat + " after {} attempts, giving up",
            SEND_RETRY_LIMIT,
            new IllegalStateException("retry limit reached: " + action));
      }
      result.completeExceptionally(new IllegalStateException("Failed after retries: " + action));
      return;
    }
    if (memberId != null) {
      LOGGER.warn(
          warnFormat + ", retrying in {}ms (attempt {}/{})",
          memberId,
          SEND_RETRY_DELAY.toMillis(),
          attempt + 1,
          SEND_RETRY_LIMIT);
    } else {
      LOGGER.warn(
          warnFormat + ", retrying in {}ms (attempt {}/{})",
          SEND_RETRY_DELAY.toMillis(),
          attempt + 1,
          SEND_RETRY_LIMIT);
    }
    actor.schedule(Duration.ofMillis(SEND_RETRY_DELAY.toMillis()), resend);
  }
}
