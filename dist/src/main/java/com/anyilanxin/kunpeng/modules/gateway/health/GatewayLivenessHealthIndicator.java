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
package com.anyilanxin.kunpeng.modules.gateway.health;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.health.application.LivenessStateHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * 存活探针
 *
 * @author zxuanhong
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @since 1.0.0
 */
@Component
public final class GatewayLivenessHealthIndicator extends LivenessStateHealthIndicator {
  private static final Health STARTED = Health.up().build();
  private static final Health NOT_STARTED = Health.down().build();

  public GatewayLivenessHealthIndicator(final ApplicationAvailability availability) {
    super(availability);
  }

  @Override
  protected AvailabilityState getState(final ApplicationAvailability applicationAvailability) {
    return super.getState(applicationAvailability);
  }
}
