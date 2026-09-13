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
package com.anyilanxin.kunpeng.modules.broker.health;

import static com.anyilanxin.kunpeng.modules.broker.health.BrokerHealthConfigurationInitializer.INDICATOR_BROKER_STATUS;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 状态探针
 *
 * @author zxuanhong
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @since 1.0.0
 */
@Component(value = INDICATOR_BROKER_STATUS)
public final class BrokerStatusHealthIndicator implements HealthIndicator {

  private static final Health HEALTHY = Health.up().build();
  private static final Health UNHEALTHY = Health.down().build();

  @Override
  public Health health() {
    return HEALTHY;
  }
}
