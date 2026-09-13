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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstream;

import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.FlowControlCfg;
import com.anyilanxin.kunpeng.configuration.broker.backpressure.AIMDCfg;
import com.anyilanxin.kunpeng.configuration.broker.backpressure.LimitCfg;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogBuilder;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Clock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 角色切换时装配分区 EventLog（Raft 存储桥 + 自研流控参数） */
public final class AdminLogStreamPartitionTransitionStep
    implements TransitionStep<AdminTransitionContent> {

  private static final Logger LOG =
      LoggerFactory.getLogger(AdminLogStreamPartitionTransitionStep.class);

  private final Supplier<EventLogBuilder> builderSupplier;

  public AdminLogStreamPartitionTransitionStep() {
    this(EventLog::builder);
  }

  AdminLogStreamPartitionTransitionStep(final Supplier<EventLogBuilder> builderSupplier) {
    this.builderSupplier = builderSupplier;
  }

  @Override
  public String getName() {
    return "Admin Log Stream Partition Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(final AdminTransitionContent context, final long currentTerm) {
    return buildEventLog(context);
  }

  @Override
  public ActorFuture<Void> onFollower(
      final AdminTransitionContent context, final long currentTerm) {
    return buildEventLog(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final AdminTransitionContent context, final long currentTerm) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              final var oldEventLog = context.getEventLog();
              if (oldEventLog != null) {
                oldEventLog.close();
                context.setEventLog(null);
              }
              future.complete(null);
            });
    return future;
  }

  private ActorFuture<Void> buildEventLog(final AdminTransitionContent context) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    concurrencyControl.run(
        () -> {
          if (context.getEventLog() == null) {
            try {
              final EventLog eventLog =
                  builderSupplier
                      .get()
                      .withEventStore(context.getEventStore())
                      .withLogName("eventLog-" + context.getRaftPartition().name())
                      .withPartitionId(context.getRaftPartitionId().id())
                      .withMaxBatchSize(context.getMaxFragmentSize())
                      .withClock(Clock.systemUTC())
                      .withFlowControl(toParams(context.getBrokerCfg()))
                      .withMeterRegistry(context.getMeterRegistry())
                      .build();
              context.setEventLog(eventLog);
              future.complete(null);
            } catch (final Exception e) {
              LOG.error("Failed to build event log", e);
              future.completeExceptionally(e);
            }
          } else {
            future.complete(null);
          }
        });
    return future;
  }

  /** 配置数值 → 自研流控参数（AIMD 在途窗口，唯一准入控制） */
  static FlowControlParams toParams(final BrokerCfg brokerCfg) {
    final FlowControlCfg flowControlCfg = brokerCfg.getFlowControl();
    final LimitCfg request = flowControlCfg == null ? null : flowControlCfg.getRequest();

    int initial = 100;
    int min = 10;
    int max = 1000;
    final boolean windowEnabled = request != null && request.isEnabled();
    if (windowEnabled) {
      if (request.getAlgorithm() == LimitCfg.LimitAlgorithm.FIXED) {
        final int fixed = request.getFixed().getLimit();
        initial = min = max = Math.max(1, fixed);
      } else {
        final AIMDCfg aimd = request.getAimd();
        initial = Math.max(1, aimd.getInitialLimit());
        min = Math.max(1, aimd.getMinLimit());
        max = Math.max(min, aimd.getMaxLimit());
      }
    } else {
      // 窗口关闭（与旧行为一致: 线上窗口拒绝实际禁用）
      initial = min = max = Integer.MAX_VALUE;
    }
    return new FlowControlParams(initial, Math.min(min, initial), max, 0.1);
  }
}
