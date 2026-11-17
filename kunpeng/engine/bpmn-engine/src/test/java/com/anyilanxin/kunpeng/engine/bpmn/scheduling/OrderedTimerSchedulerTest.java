/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import org.junit.jupiter.api.Test;

/** 调度命令 append 结果的重试分类测试。 */
class OrderedTimerSchedulerTest {

  @Test
  void shouldFailPermanentlyInvalidBatchInsteadOfRetryingForever() {
    final var rejected =
        new AppendResult.Rejected(AppendResult.RejectionReason.INVALID_ARGUMENT);

    assertThrows(
        IllegalArgumentException.class, () -> OrderedTimerScheduler.isAppendSuccessful(rejected));
  }

  @Test
  void shouldKeepTemporaryFailuresRetryable() {
    for (final var reason :
        new AppendResult.RejectionReason[] {
          AppendResult.RejectionReason.FROZEN,
          AppendResult.RejectionReason.REQUEST_WINDOW_EXHAUSTED,
          AppendResult.RejectionReason.STORAGE_FAILURE,
          AppendResult.RejectionReason.CLOSED
        }) {
      assertFalse(OrderedTimerScheduler.isAppendSuccessful(new AppendResult.Rejected(reason)));
    }
    assertTrue(OrderedTimerScheduler.isAppendSuccessful(new AppendResult.Appended(1, 1)));
  }

  @Test
  void shouldLetFirstRecordReachRealAppendWhenConservativeProbeWouldReject() {
    final var writer = mock(EventLogWriter.class);
    when(writer.canAppend(anyInt(), anyInt())).thenReturn(false);

    assertTrue(OrderedTimerScheduler.canAppendScheduledBatch(writer, 1, 100));
    assertFalse(OrderedTimerScheduler.canAppendScheduledBatch(writer, 2, 100));
  }
}
