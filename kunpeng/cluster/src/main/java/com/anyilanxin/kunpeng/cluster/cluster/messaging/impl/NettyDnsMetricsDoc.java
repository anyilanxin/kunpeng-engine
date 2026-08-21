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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** Netty DNS 解析相关指标定义 */
public enum NettyDnsMetricsDoc implements CustomMeterDocumentation {
  /** DNS 查询发生错误的次数 */
  ERROR("zeebe_dns_error", "Counts how often DNS queries fail with an error", Type.COUNTER),
  /** DNS 查询返回失败应答的次数（按响应码打标签） */
  FAILED("zeebe_dns_failed", "Counts how often DNS queries return an unsuccessful answer", Type.COUNTER),
  /** DNS 查询发出的次数 */
  WRITTEN("zeebe_dns_written", "Counts how often DNS queries are written", Type.COUNTER),
  /** DNS 查询成功的次数 */
  SUCCESS("zeebe_dns_success", "Counts how often DNS queries are successful", Type.COUNTER);

  private final String name;
  private final String description;
  private final Type type;

  NettyDnsMetricsDoc(final String name, final String description, final Type type) {
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
