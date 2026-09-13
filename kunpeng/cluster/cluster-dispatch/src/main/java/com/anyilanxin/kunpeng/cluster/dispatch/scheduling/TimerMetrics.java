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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 记录定时调度器的运行计数与耗时指标。
 *
 * <p>队列深度以单调调整的计数器跟踪；任务截止时间与实际执行之间的滞后、以及执行任务 所耗时间，则以分布计时器记录。
 */
public interface TimerMetrics {

  /** 记录一个已调度任务进入队列。 */
  void onEnqueue();

  /** 记录一个已调度任务离开队列开始执行。 */
  void onDequeue();

  /** 记录任务截止时间与其实际执行之间的滞后（毫秒）。 */
  void observeLag(long lagMillis);

  /** 记录执行任务所耗的墙钟时间（毫秒）。 */
  void observeRunTime(long tookMillis);

  /** 返回丢弃所有观测的指标实现。 */
  static TimerMetrics noop() {
    return new TimerMetrics() {
      @Override
      public void onEnqueue() {}

      @Override
      public void onDequeue() {}

      @Override
      public void observeLag(final long lagMillis) {}

      @Override
      public void observeRunTime(final long tookMillis) {}
    };
  }

  /** 返回向给定注册表发布指标的实现。 */
  static TimerMetrics of(final MeterRegistry registry) {
    return new Impl(registry);
  }

  /** 基于 Micrometer 的 {@link TimerMetrics}。 */
  final class Impl implements TimerMetrics {

    private final LongAdder queued = new LongAdder();
    private final Timer lag;
    private final Timer duration;

    public Impl(final MeterRegistry registry) {
      Gauge.builder("kunpeng.engine.timers.queued", queued, LongAdder::sum).register(registry);
      lag =
          Timer.builder("kunpeng.engine.timers.lag")
              .publishPercentileHistogram()
              .register(registry);
      duration =
          Timer.builder("kunpeng.engine.timers.duration")
              .publishPercentileHistogram()
              .register(registry);
    }

    @Override
    public void onEnqueue() {
      queued.increment();
    }

    @Override
    public void onDequeue() {
      queued.decrement();
    }

    @Override
    public void observeLag(final long lagMillis) {
      lag.record(lagMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void observeRunTime(final long tookMillis) {
      duration.record(tookMillis, TimeUnit.MILLISECONDS);
    }
  }
}
