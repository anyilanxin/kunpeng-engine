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

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandBatch;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandCollector;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerJob;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.job.ImmutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.record.DeadlineIndex;
import java.time.Duration;
import java.time.InstantSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 到期扫描任务：遍历到期索引，为每个已到 deadline 的 job 追加 TIME_OUT 命令。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class JobTimeoutCheckerTask implements TimerJob {
  private static final Logger LOG = LoggerFactory.getLogger(JobTimeoutCheckerTask.class);

  /** 是否继续排期；暂停/关闭/失败期间置否，恢复后重新置真 */
  private boolean schedulable = false;

  /** 本轮扫描的截止水位；跨多次调度扫完同一轮期间保持不变，扫完复位 */
  private long cutoffMillis = -1;

  /** 上一批因数量上限截断时的续扫游标；null 表示下一轮从头开始 */
  private DeadlineIndex resumeFrom = null;

  private final ImmutableJobRepository jobRepository;
  private SchedulerContext schedulerContext;
  private final Duration idleInterval;
  private final int scanBatchSize;
  private final InstantSource clock;

  JobTimeoutCheckerTask(
      final ImmutableJobRepository jobRepository,
      final Duration idleInterval,
      final int scanBatchSize,
      final InstantSource clock) {
    this.jobRepository = jobRepository;
    this.idleInterval = idleInterval;
    this.scanBatchSize = scanBatchSize;
    this.clock = clock;
  }

  @Override
  public CommandBatch run(final CommandCollector output) {
    if (cutoffMillis == -1) {
      cutoffMillis = clock.millis();
    }

    final int[] appended = {0};
    final DeadlineIndex truncatedAt =
        jobRepository.forEachTimedOutEntry(
            cutoffMillis,
            resumeFrom,
            (key, record) -> {
              if (appended[0] >= scanBatchSize) {
                return false;
              }
              appended[0]++;
              return output.appendCommand(key, JobLifeCycle.TIME_OUT, record);
            });

    if (truncatedAt != null) {
      // 批量上限截断：保留游标立即续扫，避免大积压时一次命令批次过重
      LOG.trace("到期扫描触发批量上限，将从 {} 立即续扫", truncatedAt);
      resumeFrom = truncatedAt;
      kickoff(Duration.ZERO);
    } else {
      cutoffMillis = -1;
      resumeFrom = null;
      kickoff(idleInterval);
    }

    LOG.trace("本轮追加 {} 条超时命令", appended[0]);
    return output.build();
  }

  void bind(final SchedulerContext schedulerContext) {
    this.schedulerContext = schedulerContext;
  }

  void markSchedulable() {
    schedulable = true;
  }

  void markUnschedulable() {
    schedulable = false;
  }

  void kickoff(final Duration delay) {
    if (schedulable) {
      schedulerContext.scheduler().scheduleAt(clock.millis() + delay.toMillis(), this);
    }
  }
}
