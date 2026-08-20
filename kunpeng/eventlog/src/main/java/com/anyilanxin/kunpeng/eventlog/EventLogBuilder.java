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
package com.anyilanxin.kunpeng.eventlog;

import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;

/** {@link EventLog} 装配器 */
public interface EventLogBuilder {

  /** 存储实现（必填，通常为 broker 侧 Raft 桥） */
  EventLogBuilder withEventStore(EventStore store);

  EventLogBuilder withLogName(String logName);

  EventLogBuilder withPartitionId(int partitionId);

  /** 单批最大字节数（超限拒绝 INVALID_ARGUMENT） */
  EventLogBuilder withMaxBatchSize(int maxBatchSize);

  /** 批时间戳来源（默认系统时钟；测试可注入固定时钟） */
  EventLogBuilder withClock(Clock clock);

  /** 流控参数；null = {@link FlowControlParams#defaults()} */
  EventLogBuilder withFlowControl(FlowControlParams params);

  EventLogBuilder withMeterRegistry(MeterRegistry registry);

  /** 有序提交链槽数（并发追加上限，默认 64） */
  EventLogBuilder withMaxConcurrentAppends(int slots);

  EventLog build();
}
