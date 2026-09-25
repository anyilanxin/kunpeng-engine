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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import java.util.Set;

/**
 * 指标工具集：统一以 {@link CustomMeterDocumentation} 为元数据源 （名称、描述、SLO 桶）构建 Micrometer 指标，标签以 key/value
 * 变参传入；另提供注册表的包装与丢弃操作。
 *
 * <p>TIMER 使用文档定义的 SLO 桶（{@link CustomMeterDocumentation#getTimerSLOs()}）， 以便直方图桶在各模块间保持一致。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Micrometers {

  private Micrometers() {}

  /** 构建带文档 SLO 桶的计时器 */
  public static Timer timer(
      final CustomMeterDocumentation doc, final MeterRegistry registry, final String... tags) {
    return Timer.builder(doc.getName())
        .description(doc.getDescription())
        .tags(tags)
        .serviceLevelObjectives(doc.getTimerSLOs())
        .register(registry);
  }

  /** 构建计数器 */
  public static Counter counter(
      final CustomMeterDocumentation doc, final MeterRegistry registry, final String... tags) {
    return Counter.builder(doc.getName())
        .description(doc.getDescription())
        .tags(tags)
        .register(registry);
  }

  /** 构建可设值 Gauge */
  public static SettableGauge gauge(
      final CustomMeterDocumentation doc, final MeterRegistry registry, final String... tags) {
    return new SettableGauge(doc.getName(), doc.getDescription(), registry, tags);
  }

  /** 构建带文档 SLO 桶的分布摘要 */
  public static DistributionSummary summary(
      final CustomMeterDocumentation doc, final MeterRegistry registry, final String... tags) {
    return DistributionSummary.builder(doc.getName())
        .description(doc.getDescription())
        .tags(tags)
        .serviceLevelObjectives(doc.getDistributionSLOs())
        .register(registry);
  }

  /**
   * 基于给定注册表构造一个带公共标签的组合注册表：注册到返回值的指标都会带上 {@code tags} 并转发到 {@code wrapped}，且不改动 {@code wrapped}
   * 自身的配置。
   *
   * @param wrapped 实际承载指标的父注册表
   * @param tags 附加到所有指标的公共标签
   * @return 组合注册表
   */
  public static CompositeMeterRegistry wrap(final MeterRegistry wrapped, final Tags tags) {
    final var registry = new CompositeMeterRegistry(wrapped.config().clock(), Set.of(wrapped));
    registry.config().commonTags(tags);
    return registry;
  }

  /**
   * 丢弃该注册表：清空其指标、从父注册表摘除并关闭；不会连带关闭被包装的父注册表。传 {@code null} 安全返回。
   *
   * @param registry 待丢弃的注册表，允许为 {@code null}
   */
  public static void close(final MeterRegistry registry) {
    if (registry == null) {
      return;
    }
    registry.clear();
    if (registry instanceof final CompositeMeterRegistry composite) {
      composite.getRegistries().forEach(composite::remove);
    }
    registry.close();
  }
}
