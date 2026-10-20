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

import com.anyilanxin.kunpeng.engine.bpmn.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandBatch;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandCollector;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerJob;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerScheduler;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.ImmutableDistributeParallelRepository;
import java.time.Duration;

/**
 * 并行分发检查器：全部分区 ACK 齐后驱动流转。 两个周期均以 {@link TimerJob} 形态执行——仓库遍历与命令追加发生在调度载体线程上， 命令经 {@link
 * CommandCollector} 暂存后统一提交（与 JobTimeoutCheckerTask 同一线程封闭模式）， 不在定时器线程直闯引擎状态。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeParallelChecker implements SchedulerCheckerAware {
  private final ImmutableDistributeParallelRepository distribute;
  public static final Duration COMMAND_REDISTRIBUTION_INTERVAL = Duration.ofSeconds(10);

  /** 是否继续执行周期任务；关闭/失败期间置否，恢复后重新置真 */
  private volatile boolean running = false;

  /** 两个周期重试链的句柄；关闭/失败时取消，阻止停排后继续触碰仓库 */
  private volatile TimerScheduler.TimerHandle retryCycle;

  private volatile TimerScheduler.TimerHandle retryAfterCycle;

  /** 冻结恢复用：onRecovered 时留存的调度上下文 */
  private volatile SchedulerContext schedulerContext;

  public DistributeParallelChecker(final ImmutableDistributeParallelRepository distribute) {
    this.distribute = distribute;
  }

  @Override
  public void onRecovered(final SchedulerContext context) {
    schedulerContext = context;
    running = true;
    retryCycle =
        context.scheduler().scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, new RetryDistribution());
    retryAfterCycle =
        context
            .scheduler()
            .scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, new RetryDistributionAfter());
  }

  @Override
  public void onClose() {
    stop();
  }

  @Override
  public void onFailed() {
    stop();
  }

  @Override
  public void onPaused() {
    stop();
  }

  @Override
  public void onResumed() {
    final SchedulerContext context = schedulerContext;
    if (context != null) {
      onRecovered(context);
    }
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

  /** 待重试的首轮分发改标 DISTRIBUTE_START */
  private final class RetryDistribution implements TimerJob {
    @Override
    public CommandBatch run(final CommandCollector output) {
      if (!running) {
        return output.build();
      }
      distribute.foreachRetriableDistribution(
          (distributionKey, distributeRecord) ->
              output.appendCommand(
                  distributeRecord.getDistributeId(),
                  DistributeParallelLifeCycle.DISTRIBUTE_START,
                  distributeRecord));
      return output.build();
    }
  }

  /** 待重试的 ACK 后流转改标 DISTRIBUTE_AFTER_START */
  private final class RetryDistributionAfter implements TimerJob {
    @Override
    public CommandBatch run(final CommandCollector output) {
      if (!running) {
        return output.build();
      }
      distribute.foreachRetriableDistributionAfter(
          (distributionKey, distributeRecord) ->
              output.appendCommand(
                  distributeRecord.getDistributeId(),
                  DistributeParallelLifeCycle.DISTRIBUTE_AFTER_START,
                  distributeRecord));
      return output.build();
    }
  }
}
