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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling.cache;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * 为有界待发命令注册表提供按 lifeCycle 划分的大小上报器。
 *
 * <p>每个上报器是一个 {@link IntConsumer}，注册表在每次变更后以当前大小调用它，指标实现 借此将这些回调转化为实时 gauge。
 */
public interface RegistryMetrics {

  /** 返回 {@code lifeCycle} 的注册表应以自身大小调用的消费者。 */
  IntConsumer forLifeCycle(AdminValueLifeCycle lifeCycle);

  /** 将每个 lifeCycle 的集合大小以 gauge 上报，并以 lifeCycle 的类名与名称打标签。 */
  class BoundedRegistryMetrics implements RegistryMetrics {

    private final Map<AdminValueLifeCycle, AtomicInteger> sizes = new HashMap<>();
    private final MeterRegistry registry;

    public BoundedRegistryMetrics(final MeterRegistry registry) {
      this.registry = Objects.requireNonNull(registry, "must specify a meter registry");
    }

    @Override
    public IntConsumer forLifeCycle(final AdminValueLifeCycle lifeCycle) {
      return sizes.computeIfAbsent(lifeCycle, this::registerSizeReporter)::set;
    }

    private AtomicInteger registerSizeReporter(final AdminValueLifeCycle lifeCycle) {
      final var intentLabel = lifeCycle.getClass().getSimpleName() + "." + lifeCycle.name();
      final var doc = RegistryMetricsDoc.SIZE;
      final var tracker = new AtomicInteger();
      Gauge.builder(doc.getName(), tracker, AtomicInteger::intValue)
          .description(doc.getDescription())
          .tag(LifeCycleKey.LIFE_CYCLE.asString(), intentLabel)
          .register(registry);
      return tracker;
    }
  }

  /** 记录 {@link BoundedRegistryMetrics} 发布的 gauge。 */
  @SuppressWarnings("NullableProblems")
  enum RegistryMetricsDoc implements CustomMeterDocumentation {
    /** 上报每个 lifeCycle 去重后的在途命令 key 数量。 */
    SIZE {
      @Override
      public String getName() {
        return "kunpeng.engine.timers.dedup.size";
      }

      @Override
      public Meter.Type getType() {
        return Meter.Type.GAUGE;
      }

      @Override
      public String getDescription() {
        return "Current size of the in-flight command dedup set, per intent";
      }

      @Override
      public KeyName[] getKeyNames() {
        return LifeCycleKey.values();
      }
    }
  }

  /** {@link RegistryMetricsDoc} 中 gauge 暴露的标签 key。 */
  @SuppressWarnings("NullableProblems")
  enum LifeCycleKey implements KeyName {
    /** 以命令 lifeCycle 区分 gauge。 */
    LIFE_CYCLE {
      @Override
      public String asString() {
        return "intent";
      }
    }
  }
}
