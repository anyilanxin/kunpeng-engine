/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

class JournalMetrics {

  private final Timer segmentCreationTime;
  private final Timer segmentTruncateTime;
  private final Timer segmentFlushTime;
  private final SettableGauge segmentCount;
  private final SettableGauge journalOpenDuration;

  JournalMetrics(final String logName, final MeterRegistry meterRegistry) {
    segmentCreationTime =
        Micrometers.timer(
            JournalMetricDocs.SEGMENT_CREATION_TIME, meterRegistry, "partition", logName);
    segmentTruncateTime =
        Micrometers.timer(
            JournalMetricDocs.SEGMENT_TRUNCATE_TIME, meterRegistry, "partition", logName);
    segmentFlushTime =
        Micrometers.timer(
            JournalMetricDocs.SEGMENT_FLUSH_TIME, meterRegistry, "partition", logName);
    segmentCount =
        Micrometers.gauge(JournalMetricDocs.SEGMENT_COUNT, meterRegistry, "partition", logName);
    journalOpenDuration =
        Micrometers.gauge(
            JournalMetricDocs.JOURNAL_OPEN_DURATION, meterRegistry, "partition", logName);
  }

  void observeSegmentCreation(final Runnable segmentCreation) {
    segmentCreationTime.record(segmentCreation);
  }

  void observeSegmentFlush(final Runnable segmentFlush) {
    segmentFlushTime.record(segmentFlush);
  }

  void observeSegmentTruncation(final Runnable segmentTruncation) {
    segmentTruncateTime.record(segmentTruncation);
  }

  /** 开始统计日志打开耗时，close 时将耗时（毫秒）记录到指标 */
  CloseableSilently startJournalOpenDurationTimer() {
    final long startNanos = System.nanoTime();
    return () ->
        journalOpenDuration.set(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
  }

  void incSegmentCount() {
    segmentCount.inc();
  }

  void decSegmentCount() {
    segmentCount.dec();
  }
}
