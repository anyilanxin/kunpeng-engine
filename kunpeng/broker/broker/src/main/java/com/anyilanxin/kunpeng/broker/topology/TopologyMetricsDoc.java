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
package com.anyilanxin.kunpeng.broker.topology;

import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.docs.MeterDocumentation;

/**
 * 拓扑指标定义（数据手术观测，前缀 kunpeng.engine.partition.*）——单一事实源，实现类一律经此构建 meter。
 *
 * <p>冻结状态与冻结时长构成数据手术窗口的可观测面：状态 gauge 供告警阈值（&gt;0 即有分区在手术中）， 时长 gauge 供手术窗口上限监控（超限交人工处置，不自动中断——见
 * cluster-dispatch-design 技能 03 号文档 §8）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum TopologyMetricsDoc implements MeterDocumentation {
  PARTITION_FROZEN("kunpeng.engine.partition.frozen", Type.GAUGE, "分区冻结状态（数据手术通告层：1=冻结中 0=活跃）"),
  PARTITION_FREEZE_DURATION(
      "kunpeng.engine.partition.freeze.duration", Type.GAUGE, "分区冻结持续毫秒数（未冻结为 0）");

  private final String name;
  private final Type type;
  private final String description;

  TopologyMetricsDoc(final String name, final Type type, final String description) {
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
