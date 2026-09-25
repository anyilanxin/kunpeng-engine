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
package com.anyilanxin.kunpeng.utils.micrometer;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import io.micrometer.common.lang.Nullable;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自持值的可设值 Gauge：内部以 {@link AtomicLong} 持有当前值，适合"按 tag 维度缓存后重复 set"的场景。
 *
 * <p>与 {@link SettableGauge} 的差别在于提供 builder 形态的构造（便于作为 {@link BoundedMeterCache} 的指标工厂）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class StatefulGauge implements CloseableSilently {

  private final MeterRegistry registry;
  private final Gauge gauge;
  private final AtomicLong value = new AtomicLong();

  private StatefulGauge(
      final String name,
      @Nullable final String description,
      final MeterRegistry registry,
      final Iterable<Tag> tags) {
    this.registry = registry;
    final var builder =
        Gauge.builder(name, value, AtomicLong::get).tags(tags).strongReference(true);
    if (description != null) {
      builder.description(description);
    }
    this.gauge = builder.register(registry);
  }

  public static Builder builder(final String name) {
    return new Builder(name);
  }

  /** 设置当前值 */
  public void set(final long value) {
    this.value.set(value);
  }

  /** 读取当前值 */
  public long get() {
    return value.get();
  }

  @Override
  public void close() {
    registry.remove(gauge);
  }

  /** 指标构造器：name/description/tags 配置后经 {@link #withRegistry(MeterRegistry)} 注册 */
  public static final class Builder {

    private final String name;
    private final List<Tag> tags = new ArrayList<>();
    @Nullable private String description;

    private Builder(final String name) {
      this.name = name;
    }

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder tag(final String key, final String value) {
      tags.add(Tag.of(key, value));
      return this;
    }

    public StatefulGauge withRegistry(final MeterRegistry registry) {
      return new StatefulGauge(name, description, registry, tags);
    }
  }
}
