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
package com.anyilanxin.kunpeng.modules.common.actor;

import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class ActorSchedulerConfiguration {

  @Bean(destroyMethod = "close")
  public ActorScheduler scheduler(
      final SchedulerConfiguration configuration, final MeterRegistry meterRegistry) {
    final var scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(configuration.cpuThreads)
            .setIoBoundActorThreadCount(configuration.ioThreads)
            .setSchedulerName(configuration.nodeId)
            .setMeterRegistry(meterRegistry)
            .build();
    scheduler.start();
    return scheduler;
  }

  public record SchedulerConfiguration(
      int cpuThreads, int ioThreads, String schedulerPrefix, String nodeId) {}
}
