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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.webapp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Web 控制台装配门：配置键随宿主形态（内嵌网关 {@code kunpeng.broker.gateway.webapp.enabled}、独立网关 {@code
 * kunpeng.gateway.webapp.enabled}），任一存在即生效（内嵌优先），均未配置默认开启。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionalOnWebappEnabled.WebappEnabledCondition.class)
public @interface ConditionalOnWebappEnabled {

  /** 内嵌网关（broker 宿主）形态的总开关键 */
  String EMBEDDED_KEY = "kunpeng.broker.gateway.webapp.enabled";

  /** 独立网关形态的总开关键 */
  String STANDALONE_KEY = "kunpeng.gateway.webapp.enabled";

  /** 双键取值：内嵌键存在则以其为准，否则独立键，均未配置视为开启 */
  class WebappEnabledCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
      final Environment environment = context.getEnvironment();
      if (environment.containsProperty(EMBEDDED_KEY)) {
        return Boolean.parseBoolean(environment.getProperty(EMBEDDED_KEY));
      }
      if (environment.containsProperty(STANDALONE_KEY)) {
        return Boolean.parseBoolean(environment.getProperty(STANDALONE_KEY));
      }
      return true;
    }
  }
}
