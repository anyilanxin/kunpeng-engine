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

import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.MessagingMetricsDoc.IN_FLIGHT_REQUESTS;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.MessagingMetricsDoc.REQUEST_COUNT;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.MessagingMetricsDoc.REQUEST_RESPONSE_LATENCY;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.MessagingMetricsDoc.REQUEST_SIZE_IN_KB;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.MessagingMetricsDoc.RESPONSE_COUNT;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.micrometer.CloseableTime;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.ThreadSafe;

/** 集群消息通信相关指标采集 */
@ThreadSafe
final class MessagingMetricsImpl implements MessagingMetrics {

  private final MeterRegistry registry;
  private final Map<String, SettableGauge> inFlightRequests = new ConcurrentHashMap<>();

  MessagingMetricsImpl(final MeterRegistry registry) {
    this.registry = registry;
  }

  /** 请求-响应计时句柄，关闭时记录一次往返耗时 */
  @Override
  public CloseableSilently startRequestTimer(final String name) {
    final var timer = Micrometers.timer(REQUEST_RESPONSE_LATENCY, registry, "topic", name);
    return new CloseableTime(timer, registry).start();
  }

  /** 记录一次发送的请求大小 */
  @Override
  public void observeRequestSize(final String to, final String name, final int requestSizeInBytes) {
    Micrometers.summary(REQUEST_SIZE_IN_KB, registry, "address", to, "topic", name)
        .record(requestSizeInBytes / 1_000f);
  }

  /** 记录一条单播消息 */
  @Override
  public void countMessage(final String to, final String name) {
    Micrometers.counter(
            REQUEST_COUNT, registry, "type", "MESSAGE", "address", to, "topic", name)
        .increment();
  }

  /** 记录一次请求-响应调用 */
  @Override
  public void countRequestResponse(final String to, final String name) {
    Micrometers.counter(
            REQUEST_COUNT, registry, "type", "REQ_RESP", "address", to, "topic", name)
        .increment();
  }

  /** 记录一次成功响应 */
  @Override
  public void countSuccessResponse(final String address, final String name) {
    countResponse(address, name, "SUCCESS");
  }

  /** 记录一次失败响应 */
  @Override
  public void countFailureResponse(final String address, final String name, final String error) {
    countResponse(address, name, error);
  }

  private void countResponse(final String address, final String name, final String outcome) {
    Micrometers.counter(
            RESPONSE_COUNT, registry, "outcome", outcome, "address", address, "topic", name)
        .increment();
  }

  @Override
  public void incInFlightRequests(final String address, final String topic) {
    inFlight(address, topic).inc();
  }

  @Override
  public void decInFlightRequests(final String address, final String topic) {
    inFlight(address, topic).dec();
  }

  private SettableGauge inFlight(final String address, final String topic) {
    return inFlightRequests.computeIfAbsent(
        address + "#" + topic,
        ignored -> Micrometers.gauge(IN_FLIGHT_REQUESTS, registry, "address", address, "topic", topic));
  }
}
