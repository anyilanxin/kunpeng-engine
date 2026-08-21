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
package com.anyilanxin.kunpeng.cluster.cluster.protocol;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** SWIM 成员协议相关指标定义 */
public enum SwimMembershipProtocolMetricsDoc implements CustomMeterDocumentation {
  /** 各成员的 incarnation 编号（观察成员状态传播） */
  MEMBERS_INCARNATION_NUMBER("zeebe_smp_members_incarnation_number", "Member's incarnation number, useful to observe state propagation of each member information", Type.GAUGE),
  /** 当前成员总数 */
  MEMBER_COUNT("zeebe_smp_member_count", "Number of members currently known by the SWIM protocol", Type.GAUGE),
  /** 成员加入事件次数 */
  MEMBER_ADDED("zeebe_smp_member_added_count", "Number of member added events", Type.COUNTER),
  /** 成员离开/移除事件次数 */
  MEMBER_REMOVED("zeebe_smp_member_removed_count", "Number of member removed events", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  SwimMembershipProtocolMetricsDoc(final String name, final String description, final Type type) {
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
