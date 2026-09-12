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

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可设值的 Micrometer Gauge 句柄：内部以 {@link AtomicLong} 持有当前值， 提供 set/inc/dec 语义（对应 Prometheus
 * simpleclient 的 Gauge 用法）。
 *
 * <p>Micrometer 的 {@link Gauge} 只能通过观测函数取值且为弱引用， 本类通过 {@code strongReference} 与自身持有的数值对象保证指标不被回收。
 * 关闭时从注册中心移除该指标。
 *
 * @author zxuanhong
 */
public final class SettableGauge implements CloseableSilently {

  private final MeterRegistry registry;
  private final Gauge gauge;
  private final AtomicLong value;

  SettableGauge(
      final String name,
      final String description,
      final MeterRegistry registry,
      final String... tags) {
    this.registry = registry;
    this.value = new AtomicLong();
    this.gauge =
        Gauge.builder(name, value, AtomicLong::get)
            .description(description)
            .tags(tags)
            .strongReference(true)
            .register(registry);
  }

  /** 设置当前值 */
  public void set(final long value) {
    this.value.set(value);
  }

  /** 当前值自增 1 */
  public void inc() {
    value.incrementAndGet();
  }

  /** 当前值自减 1 */
  public void dec() {
    value.decrementAndGet();
  }

  /** 读取当前值 */
  public long get() {
    return value.get();
  }

  @Override
  public void close() {
    registry.remove(gauge);
  }
}
