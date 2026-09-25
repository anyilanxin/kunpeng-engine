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

import com.anyilanxin.kunpeng.engine.bpmn.SchedulerCheckerAware;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerScheduler;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.ImmutableHistoryCleanupRepository;
import java.time.Duration;
import java.time.InstantSource;

/**
 * 历史清理到期检查器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class HistoryCleanupDueDateChecker implements SchedulerCheckerAware {
  public static final Duration DEFAULT_MESSAGES_TTL_CHECKER_INTERVAL = Duration.ofMinutes(1);
  public static final int DEFAULT_MESSAGES_TTL_CHECKER_BATCH_LIMIT = Integer.MAX_VALUE;
  private final InstantSource clock;
  private final ImmutableHistoryCleanupRepository historyCleanup;

  public HistoryCleanupDueDateChecker(
      final ImmutableHistoryCleanupRepository historyCleanup, final InstantSource clock) {
    this.historyCleanup = historyCleanup;
    this.clock = clock;
  }

  @Override
  public void onRecovered(final SchedulerContext context) {
    scheduleMessageTtlChecker(context);
  }

  private void scheduleMessageTtlChecker(final SchedulerContext context) {
    final TimerScheduler scheduleService = context.scheduler();
    final var timestamp = clock.millis() + DEFAULT_MESSAGES_TTL_CHECKER_INTERVAL.toMillis();
    final var timeToLiveChecker =
        new HistoryCleanupCheckerTask(
            DEFAULT_MESSAGES_TTL_CHECKER_INTERVAL,
            DEFAULT_MESSAGES_TTL_CHECKER_BATCH_LIMIT,
            scheduleService,
            historyCleanup,
            context.clock());
    scheduleService.scheduleAt(timestamp, timeToLiveChecker);
  }
}
