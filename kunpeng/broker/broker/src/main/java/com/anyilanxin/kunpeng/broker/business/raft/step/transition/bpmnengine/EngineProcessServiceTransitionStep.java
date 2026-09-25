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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.bpmnengine;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.engine.bpmn.EngineProcessService;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引擎分区 transition 步骤：leader 时构建引擎状态机（消费事件日志、驱动流程/job 状态迁移）并提交 actor 调度； 同时把 job
 * 流推送端口注入引擎（有流推送/无流广播，见 {@code JobDeliveryPort}），并向推送失败回退处理器登记本分区日志写入器 （WITHDRAW 回退命令直写本分区日志）。非
 * leader 角色关闭引擎并摘除写入器。
 *
 * <p>线程/一致性：引擎构建在 transition 流程内串行完成；分区写入器登记/摘除与 leader 角色同步，易主后旧写入器自然失效。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class EngineProcessServiceTransitionStep
    implements TransitionStep<BusinessTransitionContent> {

  private static final Logger LOG =
      LoggerFactory.getLogger(EngineProcessServiceTransitionStep.class);

  @Override
  public String getName() {
    return "Engine Process Service";
  }

  @Override
  public ActorFuture<Void> onLeader(
      final BusinessTransitionContent context, final long currentTerm) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          if (context.getEngineProcessService() != null) {
            future.complete(null);
            return;
          }
          final int partitionId = context.getRaftPartitionId().id();
          final JobStreamDispatcher jobStreamDispatcher = context.getJobStreamDispatcher();
          final var commandSender = context.getPartitionCommandSender();
          final var commandApiService = context.getCommandApiService();
          if (jobStreamDispatcher == null || commandSender == null || commandApiService == null) {
            future.completeExceptionally(
                new IllegalStateException(
                    "Job stream wiring missing for engine assembly [partition: %d]"
                        .formatted(partitionId)));
            return;
          }
          final EngineProcessService engineService =
              new EngineProcessService(
                  jobStreamDispatcher.streamer(),
                  context.getBeanFactory(),
                  context.getEventLog(),
                  context.getRepositoryFactory(),
                  commandApiService.gettCommandApiHandle(),
                  new PartitionSourceMetadata(partitionId, partitionId, ImmutableSet.of()),
                  commandSender,
                  context.getMeterRegistry(),
                  com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock.passthrough(
                      context.getTimerClock()),
                  context.getSchedulingService());
          context
              .getSchedulingService()
              .submitActor(engineService)
              .onComplete(
                  (ignored, throwable) -> {
                    if (throwable != null) {
                      future.completeExceptionally(throwable);
                      return;
                    }
                    context.setEngineProcessService(engineService);
                    // 推送失败回退：leader 期登记本分区日志写入器（WITHDRAW 命令直写）
                    jobStreamDispatcher
                        .fallbackHandler()
                        .registerWriter(partitionId, context.getEventLog().newWriter());
                    LOG.info(
                        "Engine process service started on leader [partition: {}]", partitionId);
                    future.complete(null);
                  });
        });
    return future;
  }

  @Override
  public ActorFuture<Void> onFollower(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  private ActorFuture<Void> close(final BusinessTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final int partitionId = context.getRaftPartitionId().id();
          final JobStreamDispatcher jobStreamDispatcher = context.getJobStreamDispatcher();
          if (jobStreamDispatcher != null) {
            jobStreamDispatcher.fallbackHandler().removeWriter(partitionId);
          }
          final EngineProcessService engineService = context.getEngineProcessService();
          if (engineService != null) {
            engineService.close();
            context.setEngineProcessService(null);
          }
          future.complete(null);
        });
    return future;
  }
}
