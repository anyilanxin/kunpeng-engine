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
package com.anyilanxin.kunpeng.broker.commandapi;

import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.docs.MeterDocumentation;

/**
 * 业务命令 API 指标定义（前缀 commandapi.*）——单一事实源，实现类一律经此构建 meter。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiMetricsDoc implements MeterDocumentation {
  BACKPRESSURE_LAG("commandapi.backpressure.lag", Type.GAUGE, "sink 滞后量（最后写入位 − sink 最低确认位）"),
  BACKPRESSURE_REJECTING(
      "commandapi.backpressure.rejecting", Type.GAUGE, "sink 背压闸门状态（1=拒绝中，0=放行）"),
  BACKPRESSURE_REJECTED("commandapi.backpressure.rejected", Type.COUNTER, "被 sink 背压闸门拒绝的业务命令数");

  private final String name;
  private final Type type;
  private final String description;

  CommandApiMetricsDoc(final String name, final Type type, final String description) {
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
