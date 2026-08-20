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
package com.anyilanxin.kunpeng.eventlog.impl;

import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.docs.MeterDocumentation;

/**
 * EventLog 指标定义（紧凑声明式; 前缀 eventlog.*）——单一事实源，实现类一律经此构建 meter。
 *
 * <p>问题域分组：追加（成功/拒绝/烧毁）、按写入上下文（context tag）、提交、水位 position、流控。
 */
public enum EventLogMetricsDoc implements MeterDocumentation {
  APPEND_COUNT("eventlog.append.count", Type.COUNTER, "成功定序的批数"),
  APPEND_REJECTED_WINDOW("eventlog.append.rejected.window", Type.COUNTER, "在途窗口拒绝的批数"),
  APPEND_REJECTED_RATE("eventlog.append.rejected.rate", Type.COUNTER, "写入速率拒绝的批数"),
  APPEND_REJECTED_INVALID("eventlog.append.rejected.invalid", Type.COUNTER, "参数非法拒绝的批数"),
  APPEND_BURNED_POSITIONS("eventlog.append.burned.positions", Type.COUNTER, "失败烧毁的 position 数"),
  COMMIT_COUNT("eventlog.commit.count", Type.COUNTER, "提交的批数"),
  APPEND_CONTEXT_COUNT("eventlog.append.context.count", Type.COUNTER, "按写入上下文的成功定序批数"),
  APPEND_CONTEXT_ENTRIES("eventlog.append.context.entries", Type.COUNTER, "按写入上下文的成功定序条目数"),
  APPEND_CONTEXT_REJECTED("eventlog.append.context.rejected", Type.COUNTER, "按写入上下文与原因的拒绝批数"),
  POSITION_WRITTEN("eventlog.position.written", Type.GAUGE, "最后写入 position"),
  POSITION_COMMITTED("eventlog.position.committed", Type.GAUGE, "最后提交 position"),
  POSITION_PROCESSED("eventlog.position.processed", Type.GAUGE, "最后处理 position"),
  FLOW_WINDOW("eventlog.flow.window", Type.GAUGE, "AIMD 在途窗口"),
  FLOW_INFLIGHT("eventlog.flow.inflight", Type.GAUGE, "在途请求数"),
  FLOW_WRITE_RATE("eventlog.flow.write.rate", Type.GAUGE, "当前写入速率上限（entry/s, ×1000 定点）");

  private final String name;
  private final Type type;
  private final String description;

  EventLogMetricsDoc(final String name, final Type type, final String description) {
    this.name = name;
    this.type = type;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }
}
