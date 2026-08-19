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

/** 分段日志指标定义（标签：partition） */
public enum JournalMetricDocs implements CustomMeterDocumentation {
  /** 创建日志段的耗时 */
  SEGMENT_CREATION_TIME("atomix_segment_creation_time", "Time spend to create a new segment", Type.TIMER),
  /** 截断日志段的耗时 */
  SEGMENT_TRUNCATE_TIME("atomix_segment_truncate_time", "Time spend to truncate a segment", Type.TIMER),
  /** 日志段刷盘耗时 */
  SEGMENT_FLUSH_TIME("atomix_segment_flush_time", "Time spend to flush segment to disk", Type.TIMER),
  /** 当前日志段数量 */
  SEGMENT_COUNT("atomix_segment_count", "Number of segments", Type.GAUGE),
  /** 最近一次打开日志的耗时（毫秒） */
  JOURNAL_OPEN_DURATION("atomix_journal_open_time", "Time taken to open the journal", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  JournalMetricDocs(final String name, final String description, final Type type) {
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
