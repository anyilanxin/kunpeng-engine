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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.APPEND_DATA_RATE;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.APPEND_LATENCY;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.APPEND_RATE;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.JOURNAL_FLUSH_TIME;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.JOURNAL_OPEN_DURATION;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.JOURNAL_SIZE_BYTES;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEEK_LATENCY;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEGMENT_ALLOCATION_TIME;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEGMENT_COUNT;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEGMENT_CREATION_TIME;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEGMENT_FLUSH_TIME;
import static com.anyilanxin.kunpeng.cluster.raft.journal.file.JournalMetricsDoc.SEGMENT_TRUNCATE_TIME;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** 分段 journal（预写日志）相关指标采集，按分区名打标签 */
final class JournalMetrics extends RaftMetrics {

  /** 计时采样句柄，关闭时停止计时 */
  interface Measurement extends AutoCloseable {
    @Override
    void close();
  }

  private final MeterRegistry registry;
  private final Timer segmentCreationTime;
  private final Timer segmentTruncateTime;
  private final Timer segmentFlushTime;
  private final Timer journalFlushTime;
  private final Timer segmentAllocationTime;
  private final Timer appendLatency;
  private final Timer seekLatency;
  private final Counter appendRate;
  private final Counter appendDataRate;
  private final SettableGauge segmentCount;
  private final SettableGauge journalOpenDuration;
  private final SettableGauge journalSizeBytes;

  JournalMetrics(final String journalName, final MeterRegistry registry) {
    super(journalName);
    this.registry = registry;
    segmentCreationTime = makeTimer(SEGMENT_CREATION_TIME);
    segmentTruncateTime = makeTimer(SEGMENT_TRUNCATE_TIME);
    segmentFlushTime = makeTimer(SEGMENT_FLUSH_TIME);
    journalFlushTime = makeTimer(JOURNAL_FLUSH_TIME);
    segmentAllocationTime = makeTimer(SEGMENT_ALLOCATION_TIME);
    appendLatency = makeTimer(APPEND_LATENCY);
    seekLatency = makeTimer(SEEK_LATENCY);
    appendRate = Micrometers.counter(APPEND_RATE, registry, partitionTags());
    appendDataRate = Micrometers.counter(APPEND_DATA_RATE, registry, partitionTags());
    segmentCount = Micrometers.gauge(SEGMENT_COUNT, registry, partitionTags());
    journalOpenDuration = Micrometers.gauge(JOURNAL_OPEN_DURATION, registry, partitionTags());
    journalSizeBytes = Micrometers.gauge(JOURNAL_SIZE_BYTES, registry, partitionTags());
  }

  void observeSegmentCreation(final Runnable segmentCreation) {
    segmentCreationTime.record(segmentCreation);
  }

  void observeSegmentTruncation(final Runnable segmentTruncation) {
    segmentTruncateTime.record(segmentTruncation);
  }

  Measurement observeSegmentFlush() {
    return start(segmentFlushTime);
  }

  Measurement observeJournalFlush() {
    return start(journalFlushTime);
  }

  Measurement observeSegmentAllocation() {
    return start(segmentAllocationTime);
  }

  Measurement observeAppendLatency() {
    return start(appendLatency);
  }

  Measurement observeSeekLatency() {
    return start(seekLatency);
  }

  /** 打开耗时的采样句柄，关闭时把纳秒差存入 gauge */
  Measurement startJournalOpenDurationTimer() {
    final long start = registry.config().clock().monotonicTime();
    return () -> journalOpenDuration.set(registry.config().clock().monotonicTime() - start);
  }

  void observeAppend(final long appendedBytes) {
    appendRate.increment();
    appendDataRate.increment(appendedBytes / 1024f);
  }

  void incSegmentCount() {
    segmentCount.inc();
  }

  void decSegmentCount() {
    segmentCount.dec();
  }

  /** 记录 journal 磁盘占用（segment 文件总字节数） */
  void setJournalSize(final long bytes) {
    journalSizeBytes.set(bytes);
  }

  private Measurement start(final Timer timer) {
    final Timer.Sample sample = Timer.start(registry);
    return () -> sample.stop(timer);
  }

  private Timer makeTimer(final JournalMetricsDoc meter) {
    return Micrometers.timer(meter, registry, partitionTags());
  }

  private String[] partitionTags() {
    return new String[] {"partitionGroupName", partitionGroupName, "partition", partition};
  }
}
