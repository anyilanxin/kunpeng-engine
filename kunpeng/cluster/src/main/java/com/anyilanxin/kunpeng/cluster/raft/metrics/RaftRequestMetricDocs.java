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
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** Raft 请求收发指标定义（标签：type、to、partitionGroupName、partition） */
public enum RaftRequestMetricDocs implements CustomMeterDocumentation {
  /** 收到的 Raft 请求数量 */
  RAFT_MESSAGES_RECEIVED("atomix_raft_messages_received", "Number of raft requests received", Type.COUNTER),
  /** 发出的 Raft 请求数量 */
  RAFT_MESSAGES_SEND("atomix_raft_messages_send", "Number of raft requests send", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  RaftRequestMetricDocs(final String name, final String description, final Type type) {
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
