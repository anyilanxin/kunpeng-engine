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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.parallel;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerScheduler;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.ImmutableDistributeParallelRepository;
import java.time.Duration;

/**
 * 并行分发检查器：全部分区 ACK 齐后驱动流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelChecker implements SchedulerCheckerAware {
  private final ImmutableDistributeParallelRepository distribute;
  private final LogEventWriter writer;
  public static final Duration COMMAND_REDISTRIBUTION_INTERVAL = Duration.ofSeconds(10);

  /** 是否继续执行周期任务；关闭/失败期间置否，恢复后重新置真 */
  private volatile boolean running = false;

  /** 两个周期重试链的句柄；关闭/失败时取消，阻止停排后继续触碰仓库 */
  private volatile TimerScheduler.TimerHandle retryCycle;

  private volatile TimerScheduler.TimerHandle retryAfterCycle;

  public DistributeParallelChecker(
      final ImmutableDistributeParallelRepository distribute, final LogEventWriter writer) {
    this.distribute = distribute;
    this.writer = writer;
  }

  @Override
  public void onRecovered(final SchedulerContext context) {
    running = true;
    retryCycle =
        context
            .scheduler()
            .scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, this::runRetryDistributionCycle);
    retryAfterCycle =
        context
            .scheduler()
            .scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, this::runRetryDistributionAfterCycle);
  }

  @Override
  public void onClose() {
    stop();
  }

  @Override
  public void onFailed() {
    stop();
  }

  private void stop() {
    running = false;
    cancelPending(retryCycle);
    cancelPending(retryAfterCycle);
    retryCycle = null;
    retryAfterCycle = null;
  }

  private static void cancelPending(final TimerScheduler.TimerHandle handle) {
    if (handle != null) {
      handle.cancel();
    }
  }

  public void runRetryDistributionCycle() {
    if (!running) {
      return;
    }
    distribute.foreachRetriableDistribution(
        (distributionKey, distributeRecord) -> {
          writer.addCommand(
              distributeRecord.getDistributeId(),
              DistributeParallelLifeCycle.DISTRIBUTE_START,
              -1,
              distributeRecord);
          return true;
        });
  }

  public void runRetryDistributionAfterCycle() {
    if (!running) {
      return;
    }
    distribute.foreachRetriableDistributionAfter(
        (distributionKey, distributeRecord) -> {
          writer.addCommand(
              distributeRecord.getDistributeId(),
              DistributeParallelLifeCycle.DISTRIBUTE_AFTER_START,
              -1,
              distributeRecord);
          return true;
        });
  }
}
