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

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** journal（预写日志）导出的全部指标定义 */
public enum JournalMetricsDoc implements CustomMeterDocumentation {
  /** 创建新 segment 的耗时 */
  SEGMENT_CREATION_TIME("atomix_segment_creation_time", "Time spent creating a new segment", Type.TIMER),
  /** 截断 segment 的耗时 */
  SEGMENT_TRUNCATE_TIME("atomix_segment_truncate_time", "Time spent truncating a segment", Type.TIMER),
  /** segment 落盘的耗时 */
  SEGMENT_FLUSH_TIME("atomix_segment_flush_time", "Time spent flushing a segment to disk", Type.TIMER),
  /** 全部脏 segment 落盘的耗时 */
  JOURNAL_FLUSH_TIME("atomix_journal_flush_time", "Time spent flushing all dirty segments", Type.TIMER),
  /** segment 数量 */
  SEGMENT_COUNT("atomix_segment_count", "Number of segments", Type.GAUGE),
  /** 打开 journal 的耗时 */
  JOURNAL_OPEN_DURATION("atomix_journal_open_time", "Time taken to open the journal", Type.GAUGE),
  /** 分配新 segment 的耗时 */
  SEGMENT_ALLOCATION_TIME("atomix_segment_allocation_time", "Time spent allocating a new segment", Type.TIMER),
  /** 追加到 journal 的数据速率（KiB） */
  APPEND_DATA_RATE("atomix_journal_append_data_rate", "The rate in KiB at which data is appended to the journal", Type.COUNTER),
  /** 追加到 journal 的条目速率（按条数） */
  APPEND_RATE("atomix_journal_append_rate", "The rate at which entries are appended to the journal, by entry count", Type.COUNTER),
  /** 追加 journal 记录的耗时分布（不含落盘） */
  APPEND_LATENCY("atomix_journal_append_latency", "Distribution of time spent appending journal records, excluding flushing", Type.TIMER),
  /** 定位到指定索引的耗时分布 */
  SEEK_LATENCY("atomix_journal_seek_latency", "Distribution of time spent seeking to a specific index", Type.TIMER),
  /** journal 磁盘占用（segment 文件总大小，字节） */
  JOURNAL_SIZE_BYTES("atomix_journal_size_bytes", "Total size of the journal segment files on disk in bytes", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  JournalMetricsDoc(final String name, final String description, final Type type) {
    this.name = name;
    this.description = description;
    this.type = type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Type getType() {
    return type;
  }
}
