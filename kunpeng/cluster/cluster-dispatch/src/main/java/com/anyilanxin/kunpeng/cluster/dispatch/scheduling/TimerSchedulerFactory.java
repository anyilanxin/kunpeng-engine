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

import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import java.time.Duration;
import java.time.InstantSource;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 基于一组共享协作对象创建 {@link OrderedTimerScheduler} 实例。
 *
 * <p>工厂捕获所有 actor 车道间共享的调度器部件——阶段与中止信号、写入器来源、待发命令 注册表、时钟、扫描间隔与指标——使每条车道都能构建绑定自身 actor 的调度器。
 */
public final class TimerSchedulerFactory {

  private final Supplier<ExecutionPhase> phaseSupplier;
  private final BooleanSupplier abortCondition;
  private final Supplier<EventLogWriter> writerSupplier;
  private final PendingCommandRegistry.Stageable registry;
  private final InstantSource clock;
  private final Duration scanInterval;
  private final TimerMetrics metrics;

  public TimerSchedulerFactory(
      final Supplier<ExecutionPhase> phaseSupplier,
      final BooleanSupplier abortCondition,
      final Supplier<EventLogWriter> writerSupplier,
      final PendingCommandRegistry.Stageable registry,
      final InstantSource clock,
      final Duration scanInterval,
      final TimerMetrics metrics) {
    this.phaseSupplier = phaseSupplier;
    this.abortCondition = abortCondition;
    this.writerSupplier = writerSupplier;
    this.registry = registry;
    this.clock = clock;
    this.scanInterval = scanInterval;
    this.metrics = metrics;
  }

  /** 创建一个新的、未绑定的 {@link OrderedTimerScheduler}。 */
  public OrderedTimerScheduler create() {
    return new OrderedTimerScheduler(
        phaseSupplier, abortCondition, writerSupplier, registry, clock, scanInterval, metrics);
  }
}
