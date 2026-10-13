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
package com.anyilanxin.kunpeng.broker.commandapi;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.broker.monitoring.DiskSpaceUsageListener;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.FlowControlCfg;
import com.anyilanxin.kunpeng.configuration.broker.backpressure.SinkBackpressureCfg;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.LongSupplier;
import org.agrona.concurrent.IdGenerator;

/**
 * 业务面命令 API 服务：actor 化的命令接收与处理器分发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandApiServiceImpl extends Actor
    implements CommandApiService, DiskSpaceUsageListener {
  private final CommandApiHandleImpl handle;
  private final BrokerCfg brokerCfg;
  private final MeterRegistry meterRegistry;

  public CommandApiServiceImpl(
      final MessagingService messagingService,
      final IdGenerator idGenerator,
      final BrokerCfg brokerCfg,
      final MeterRegistry meterRegistry) {
    handle = new CommandApiHandleImpl(messagingService, idGenerator, actor);
    this.brokerCfg = brokerCfg;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void onRecovered(final int partitionId) {}

  @Override
  public void onPaused(final int partitionId) {}

  @Override
  public void onRecoveredResource(final int resourceId) {}

  @Override
  public void onPausedResource(final int resourceId) {}

  @Override
  public void onDiskSpaceNotAvailable() {
    actor.run(handle::onDiskSpaceNotAvailable);
  }

  @Override
  public void onDiskSpaceAvailable() {
    actor.run(handle::onDiskSpaceAvailable);
  }

  @Override
  public CommandApiHandle gettCommandApiHandle() {
    return handle;
  }

  @Override
  public ActorFuture<Void> registerHandlers(
      final RaftPartitionSource partitionSource, final EventLog logStream) {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          final var logStreamWriter = logStream.newWriter();
          handle.registerHandlers(
              partitionSource, logStreamWriter, buildBackpressureGate(partitionSource, logStream));
          future.complete(null);
        });
    return future;
  }

  @Override
  public void bindSinkPositionSupplier(
      final int partitionId, final LongSupplier lowestSinkPosition) {
    actor.run(() -> handle.bindSinkPositionSupplier(partitionId, lowestSinkPosition));
  }

  /**
   * sink 滞后背压闸门（每分区一个）：配置禁用或缺失时不创建（闸门不存在）；写入位 supplier 取本分区事件日志流控， sink 最低确认位 supplier 由后续
   * transition 步骤晚绑定（commandapi 步骤先于 repository 步骤执行）。
   */
  private SinkBackpressureGate buildBackpressureGate(
      final RaftPartitionSource partitionSource, final EventLog logStream) {
    final FlowControlCfg flowControlCfg = brokerCfg == null ? null : brokerCfg.getFlowControl();
    final SinkBackpressureCfg sinkCfg = flowControlCfg == null ? null : flowControlCfg.getSink();
    if (sinkCfg == null || !sinkCfg.isEnabled()) {
      return null;
    }
    return new SinkBackpressureGate(
        partitionSource.getPartitionId().id(),
        logStream.getFlowControl()::lastWrittenPosition,
        sinkCfg.getRejectBacklog(),
        sinkCfg.getResumeBacklog(),
        meterRegistry);
  }

  @Override
  public ActorFuture<Void> unregisterHandlers(final RaftPartitionSource partitionSource) {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          handle.unregisterHandlers(partitionSource);
          future.complete(null);
        });
    return future;
  }

  @Override
  protected void onActorClosing() {
    // 关闭句柄：注销消息处理器并失败在途请求，避免关闭后请求悬挂
    handle.close();
  }
}
