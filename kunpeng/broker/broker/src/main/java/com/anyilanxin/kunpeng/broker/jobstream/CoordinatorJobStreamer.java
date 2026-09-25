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
package com.anyilanxin.kunpeng.broker.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamCoordinator;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamCoordinator.PushPoint;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamSubjects;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.engine.bpmn.JobDeliveryPort;
import java.util.Optional;

/**
 * 引擎侧投递端口的 broker 实现：协调器两级轮转选点（跨网关聚合 + 聚合内下标）并投递；无通道时宣告该类型可消费（唤醒网关挂起的长轮询）。
 *
 * <p>无状态适配器，单实例跨分区共享；送达失败回退由协调器的 failureHandler 承担（见 {@link PushFailureFallback}），本类不感知。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CoordinatorJobStreamer implements JobDeliveryPort {
  private final JobStreamCoordinator coordinator;
  private final ClusterEventService eventService;

  public CoordinatorJobStreamer(
      final JobStreamCoordinator coordinator, final ClusterEventService eventService) {
    this.coordinator = coordinator;
    this.eventService = eventService;
  }

  @Override
  public void announceAvailable(final String jobType) {
    eventService.broadcast(JobStreamSubjects.READY, jobType);
  }

  @Override
  public Optional<DeliveryChannel> pickStream(final String jobType) {
    return coordinator.select(jobType).map(point -> new PointChannel(coordinator, point));
  }

  /** 绑定一次选定结果（网关 + 会话 ID + worker）：事件落 lockOwner 与推送载荷共用同一归属 */
  private record PointChannel(JobStreamCoordinator coordinator, PushPoint point)
      implements DeliveryChannel {

    @Override
    public String worker() {
      return point.worker();
    }

    @Override
    public void push(
        final long jobKey,
        final int partitionId,
        final long deadline,
        final com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord record) {
      coordinator.enqueue(
          point.gatewayMemberId(),
          new StreamPush(point.sessionId(), point.worker(), jobKey, partitionId, deadline, record));
    }
  }
}
