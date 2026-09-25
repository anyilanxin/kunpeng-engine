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
package com.anyilanxin.kunpeng.broker.bootstrap.step.jobstream;

import com.anyilanxin.kunpeng.broker.bootstrap.AbstractBrokerStartupStep;
import com.anyilanxin.kunpeng.broker.bootstrap.BrokerStartupContext;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamCoordinator;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamSubjects;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamWireCodec;
import com.anyilanxin.kunpeng.broker.jobstream.CoordinatorJobStreamer;
import com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher;
import com.anyilanxin.kunpeng.broker.jobstream.PushFailureFallback;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job 流推送服务的 broker 层启动步骤：构建协调器（订阅快照对账 + 轮转选流 + 请求-应答推送）、引擎适配器与 WITHDRAW 失败回退处理器，作为 actor
 * 提交统一调度；注册集群成员移除监听摘除离线网关的订阅桶。
 *
 * <p>拉取/完成不经本步骤——走标准命令通道（commandapi），与部署、启动流程同路。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamBootstrapStep extends AbstractBrokerStartupStep {
  private static final Logger LOG = LoggerFactory.getLogger(JobStreamBootstrapStep.class);

  private com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEventListener membershipListener;

  @Override
  public String getName() {
    return "Job Stream Service";
  }

  @Override
  protected void startupInternal(
      final BrokerStartupContext brokerStartupContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> startupFuture) {
    final JobStreamCoordinator coordinator =
        new JobStreamCoordinator(brokerStartupContext.getAtomixCluster().getCommunicationService());
    final PushFailureFallback fallbackHandler = new PushFailureFallback();
    coordinator.setFailureHandler(fallbackHandler);
    final CoordinatorJobStreamer streamer =
        new CoordinatorJobStreamer(
            coordinator, brokerStartupContext.getAtomixCluster().getEventService());
    final JobStreamDispatcher service =
        new JobStreamDispatcher(coordinator, streamer, fallbackHandler);

    final var membershipListener =
        (com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEventListener)
            event -> {
              final var subject = event.subject();
              switch (event.type()) {
                case MEMBER_REMOVED -> coordinator.removeGateway(subject.id());
                case METADATA_CHANGED -> applySnapshotProperty(coordinator, subject);
                default -> {}
              }
            };
    this.membershipListener = membershipListener;
    brokerStartupContext.getAtomixCluster().getMembershipService().addListener(membershipListener);

    concurrencyControl.runOnCompletion(
        brokerStartupContext.getActorSchedulingService().submitActor(coordinator),
        proceed(
            () ->
                concurrencyControl.runOnCompletion(
                    brokerStartupContext.getActorSchedulingService().submitActor(fallbackHandler),
                    proceed(
                        () -> {
                          brokerStartupContext.setJobStreamDispatcher(service);
                          startupFuture.complete(brokerStartupContext);
                        },
                        startupFuture)),
            startupFuture));
  }

  /** 网关成员属性中的订阅快照解析（SWIM 元数据通道到达）——非法帧记日志丢弃，代次幂等由协调器保证 */
  private static void applySnapshotProperty(
      final JobStreamCoordinator coordinator, final Member gateway) {
    final String encoded = gateway.properties().getProperty(JobStreamSubjects.SNAPSHOT_PROPERTY);
    if (encoded == null || encoded.isEmpty()) {
      return;
    }
    try {
      coordinator.applySnapshot(
          gateway.id(), JobStreamWireCodec.decodeSnapshot(Base64.getDecoder().decode(encoded)));
    } catch (final RuntimeException e) {
      LOG.warn("网关 {} 订阅快照属性解析失败, 忽略", gateway.id(), e);
    }
  }

  @Override
  protected void shutdownInternal(
      final BrokerStartupContext brokerShutdownContext,
      final ConcurrencyControl concurrencyControl,
      final ActorFuture<BrokerStartupContext> shutdownFuture) {
    final JobStreamDispatcher service = brokerShutdownContext.getJobStreamDispatcher();
    if (service == null) {
      shutdownFuture.complete(brokerShutdownContext);
      return;
    }
    if (membershipListener != null) {
      brokerShutdownContext
          .getAtomixCluster()
          .getMembershipService()
          .removeListener(membershipListener);
    }
    concurrencyControl.runOnCompletion(
        service.fallbackHandler().closeAsync(),
        proceed(
            () ->
                concurrencyControl.runOnCompletion(
                    service.coordinator().closeAsync(),
                    proceed(
                        () -> {
                          brokerShutdownContext.setJobStreamDispatcher(null);
                          shutdownFuture.complete(brokerShutdownContext);
                        },
                        shutdownFuture)),
            shutdownFuture));
  }
}
