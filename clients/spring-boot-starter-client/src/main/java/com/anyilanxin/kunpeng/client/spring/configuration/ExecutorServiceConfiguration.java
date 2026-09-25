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
package com.anyilanxin.kunpeng.client.spring.configuration;

import com.anyilanxin.kunpeng.client.spring.jobhandling.KunpengClientExecutorService;
import com.anyilanxin.kunpeng.client.spring.properties.KunpengClientProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 执行服务装配配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnMissingBean(KunpengClientExecutorService.class)
public class ExecutorServiceConfiguration {

  private final KunpengClientProperties kunpengClientProperties;

  public ExecutorServiceConfiguration(final KunpengClientProperties kunpengClientProperties) {
    this.kunpengClientProperties = kunpengClientProperties;
  }

  @Bean
  public KunpengClientExecutorService kunpengClientThreadPool(
      @Autowired(required = false) final MeterRegistry meterRegistry) {
    final int executionThreads = kunpengClientProperties.getExecutionThreads();
    final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(executionThreads);
    if (meterRegistry != null) {
      final MeterBinder threadPoolMetrics =
          new ExecutorServiceMetrics(
              threadPool,
              "kunpengClientExecutor",
              Collections.singleton(Tag.of("name", "kunpeng_client_thread_pool")));
      threadPoolMetrics.bindTo(meterRegistry);
    }
    return new KunpengClientExecutorService(threadPool, true);
  }
}
