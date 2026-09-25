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
package com.anyilanxin.kunpeng.modules.common.metrics;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * prometheus 指标配置检查
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Component
public class PrometheusConfiguration implements BeanPostProcessor {

  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName)
      throws BeansException {
    if (bean instanceof PrometheusMeterRegistry) {
      // 如果注册的不是 prometheus 则抛出异常
      ((PrometheusMeterRegistry) bean).throwExceptionOnRegistrationFailure();
    }
    return bean;
  }
}
