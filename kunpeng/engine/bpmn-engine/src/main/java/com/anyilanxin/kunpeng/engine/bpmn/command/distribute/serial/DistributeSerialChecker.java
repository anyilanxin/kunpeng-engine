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
package com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.ImmutableDistributeSerialRepository;
import java.time.Duration;

/**
 * 串行分发检查器：按序逐分区 ACK 后驱动流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DistributeSerialChecker implements SchedulerCheckerAware {
  private final ImmutableDistributeSerialRepository distribute;
  private final LogEventWriter writer;
  public static final Duration COMMAND_REDISTRIBUTION_INTERVAL = Duration.ofSeconds(10);

  public DistributeSerialChecker(
      final ImmutableDistributeSerialRepository distribute, final LogEventWriter writer) {
    this.distribute = distribute;
    this.writer = writer;
  }

  @Override
  public void onRecovered(final SchedulerContext context) {
    context
        .scheduler()
        .scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, this::runRetryDistributionCycle);

    context
        .scheduler()
        .scheduleEvery(COMMAND_REDISTRIBUTION_INTERVAL, this::runRetryDistributionAfterCycle);
  }

  public void runRetryDistributionCycle() {
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
