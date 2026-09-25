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
package com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup;

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandBatch;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandCollector;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerJob;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerScheduler;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.ImmutableHistoryCleanupRepository;
import java.time.Duration;
import java.time.InstantSource;
import org.agrona.collections.MutableInteger;

/**
 * 历史清理 TTL 检查器：查找已过期的清理 deadline，并为每条写入一个 EXPIRE 命令。
 *
 * <p>为避免过多 EXPIRE 命令塞满日志流，单次 {@link #run(CommandCollector)} 运行只写入有限数量的命令。
 *
 * <p>由它自行决定立即重排还是在配置的 {@link #executionInterval 间隔}之后重排： 立即重排时会从上次中断处继续；否则从能找到的第一条过期 deadline 重新开始。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class HistoryCleanupCheckerTask implements TimerJob {

  /** 决定 TTL 检查器一次执行结束后空闲的时长。 */
  private final Duration executionInterval;

  /** 决定单次结果中最多尝试填充的 EXPIRE 命令数。 */
  private final int batchLimit;

  private final TimerScheduler scheduleService;
  private final ImmutableHistoryCleanupRepository historyCleanup;

  /** 跟踪用于与清理 deadline 比对的时间戳。 */
  private long currentTimestamp = -1;

  private final InstantSource clock;
  private ImmutableHistoryCleanupRepository.Index lastIndex;

  public HistoryCleanupCheckerTask(
      final Duration executionInterval,
      final int batchLimit,
      final TimerScheduler scheduleService,
      final ImmutableHistoryCleanupRepository historyCleanup,
      final InstantSource clock) {
    this.executionInterval = executionInterval;
    this.batchLimit = batchLimit;
    this.historyCleanup = historyCleanup;
    this.scheduleService = scheduleService;
    this.clock = clock;
    lastIndex = null;
  }

  @Override
  public CommandBatch run(final CommandCollector output) {
    if (currentTimestamp == -1) {
      currentTimestamp = clock.millis();
    }

    final var counter = new MutableInteger(0);
    final boolean shouldContinueWhereLeftOff =
        historyCleanup.processHistoryCleanupWithDueDateBefore(
            currentTimestamp,
            lastIndex,
            (deadline) -> {
              final long dueDate = deadline.getDueDate();
              final long historyCleanupId = deadline.getHistoryCleanupId();
              final var newIndex =
                  new ImmutableHistoryCleanupRepository.Index(historyCleanupId, dueDate);
              final boolean wasIndexAlreadyVisitedLastTime = newIndex.equals(lastIndex);
              lastIndex = newIndex;
              if (wasIndexAlreadyVisitedLastTime) {
                // 跳过该条
                return true;
              }
              output.appendCommand(
                  deadline.getHistoryCleanupId(), HistoryCleanupLifeCycle.TRIGGER, deadline);
              return counter.incrementAndGet() < batchLimit;
            });
    if (shouldContinueWhereLeftOff) {
      reschedule(Duration.ZERO);
    } else {
      lastIndex = null;
      currentTimestamp = -1;
      reschedule(executionInterval);
    }
    return output.build();
  }

  private void reschedule(final Duration idleInterval) {
    final var timestamp = clock.millis() + idleInterval.toMillis();
    scheduleService.scheduleAt(timestamp, this);
  }
}
