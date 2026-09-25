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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.job;

import com.anyilanxin.kunpeng.engine.bpmn.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.repository.business.modules.job.ImmutableJobRepository;
import java.time.Duration;
import java.time.InstantSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job 到期扫描器的调度生命周期壳：恢复/恢复运行时启动扫描任务，暂停/失败/关闭时停排。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobTimeoutChecker implements SchedulerCheckerAware {
  private static final Logger LOG = LoggerFactory.getLogger(JobTimeoutChecker.class);
  private final Duration idleInterval;
  private final JobTimeoutCheckerTask scanTask;

  public JobTimeoutChecker(
      final ImmutableJobRepository jobRepository,
      final Duration idleInterval,
      final int scanBatchSize,
      final InstantSource clock) {
    this.idleInterval = idleInterval;
    scanTask = new JobTimeoutCheckerTask(jobRepository, idleInterval, scanBatchSize, clock);
  }

  @Override
  public void onRecovered(final SchedulerContext schedulerContext) {
    scanTask.bind(schedulerContext);
    startScan();
    LOG.debug("job 到期扫描器随分区恢复启动");
  }

  @Override
  public void onClose() {
    stopScan();
  }

  @Override
  public void onFailed() {
    stopScan();
  }

  @Override
  public void onPaused() {
    stopScan();
  }

  @Override
  public void onResumed() {
    startScan();
  }

  private void startScan() {
    scanTask.markSchedulable();
    scanTask.kickoff(idleInterval);
  }

  private void stopScan() {
    scanTask.markUnschedulable();
    LOG.trace("job 到期扫描已停排");
  }
}
