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
package io.atomix.cluster.messaging.impl;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.core.instrument.Meter.Type;

/** 集群消息通信相关指标定义 */
public enum MessagingMetricsDoc implements CustomMeterDocumentation {
  /** 请求-响应的往返耗时 */
  REQUEST_RESPONSE_LATENCY("zeebe_messaging_request_response_latency", "The time how long it takes to retrieve a response for a request", Type.TIMER),
  /** 已发送请求的大小（KB，按地址与主题打标签） */
  REQUEST_SIZE_IN_KB("zeebe_messaging_request_size_kb", "The size of the request, which has been sent", Type.DISTRIBUTION_SUMMARY),
  /** 发往某地址的请求数（含单播消息与请求-响应两类） */
  REQUEST_COUNT("zeebe_messaging_request_count", "Number of requests which has been sent to a certain address", Type.COUNTER),
  /** 发往某地址的响应数（按结果打标签） */
  RESPONSE_COUNT("zeebe_messaging_response_count", "Number of responses which has been sent to a certain address", Type.COUNTER),
  /** 进行中的请求数 */
  IN_FLIGHT_REQUESTS("zeebe_messaging_inflight_requests", "The count of inflight requests", Type.GAUGE);

  private final String name;
  private final String description;
  private final Type type;

  MessagingMetricsDoc(final String name, final String description, final Type type) {
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

  /** 请求大小分布的自定义 SLO 桶（KB） */
  @Override
  public double[] getDistributionSLOs() {
    return new double[] {.01, .1, .250, 1, 10, 100, 500, 1_000, 2_000, 4_000};
  }
}
