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
package com.anyilanxin.kunpeng.utils.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 指标构建工厂：统一以 {@link CustomMeterDocumentation} 为元数据源
 * （名称、描述、SLO 桶）构建 Micrometer 指标，标签以 key/value 变参传入。
 *
 * <p>TIMER 使用文档定义的 SLO 桶（{@link CustomMeterDocumentation#getTimerSLOs()}），
 * 以便直方图桶在各模块间保持一致。
 *
 * @author zxuanhong
 */
public final class Micrometers {

  private Micrometers() {}

  /** 构建带文档 SLO 桶的计时器 */
  public static Timer timer(final CustomMeterDocumentation doc, final MeterRegistry registry,
      final String... tags) {
    return Timer.builder(doc.getName())
        .description(doc.getDescription())
        .tags(tags)
        .serviceLevelObjectives(doc.getTimerSLOs())
        .register(registry);
  }

  /** 构建计数器 */
  public static Counter counter(final CustomMeterDocumentation doc, final MeterRegistry registry,
      final String... tags) {
    return Counter.builder(doc.getName())
        .description(doc.getDescription())
        .tags(tags)
        .register(registry);
  }

  /** 构建可设值 Gauge */
  public static SettableGauge gauge(final CustomMeterDocumentation doc,
      final MeterRegistry registry, final String... tags) {
    return new SettableGauge(doc.getName(), doc.getDescription(), registry, tags);
  }
}
