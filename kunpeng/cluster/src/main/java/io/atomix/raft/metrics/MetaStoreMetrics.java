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
package io.atomix.raft.metrics;

import static io.atomix.raft.metrics.MetaStoreMetricsDoc.LAST_FLUSHED_INDEX;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.CloseableTime;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;

/** Raft 元数据存储相关指标采集 */
public final class MetaStoreMetrics extends RaftMetrics {
  private final Timer lastFlushedIndexUpdate;
  private final MeterRegistry registry;

  public MetaStoreMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);
    Objects.requireNonNull(registry, "MeterRegistry cannot be null");
    lastFlushedIndexUpdate =
        Micrometers.timer(LAST_FLUSHED_INDEX, registry, "partitionGroupName", partitionGroupName);
    this.registry = registry;
  }

  /** 最后落盘索引更新计时句柄，关闭时记录一次耗时 */
  public CloseableSilently observeLastFlushedIndexUpdate() {
    return new CloseableTime(lastFlushedIndexUpdate, registry).start();
  }
}
