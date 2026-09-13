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
package com.anyilanxin.kunpeng.configuration.broker.backpressure.limit;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public record RateLimit(boolean enabled, int limit, Duration rampUp, Throttling throttling) {
  public RateLimit {
    Objects.requireNonNull(throttling, "throttling must not be null");
    Objects.requireNonNull(rampUp, "rampUp must not be null");
    if (enabled && limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than 0");
    }
    if (enabled && rampUp.isNegative()) {
      throw new IllegalArgumentException("rampUp cannot be negative");
    }
    if (enabled && throttling.enabled() && throttling.minRate() > limit) {
      throw new IllegalArgumentException(
          "minimum throttling rate must not be larger than the regular limit");
    }
  }

  public static RateLimit disabled() {
    return new RateLimit(false, 0, Duration.ZERO, Throttling.disabled());
  }

  public RateLimiter limiter() {
    if (!enabled) {
      return null;
    }
    if (rampUp.isZero()) {
      return RateLimiter.create(limit);
    }
    return RateLimiter.create(limit, rampUp);
  }

  public record Throttling(
      boolean enabled, long acceptableBacklog, long minRate, Duration resolution) {
    public Throttling {
      Objects.requireNonNull(resolution, "resolution must not be null");

      if (enabled && resolution.isZero()) {
        throw new IllegalArgumentException("resolution must be greater than 0");
      }
      if (enabled && acceptableBacklog < 0) {
        throw new IllegalArgumentException("acceptableBacklog must be greater than 0");
      }
      if (enabled && minRate < 0) {
        throw new IllegalArgumentException("minRate must be greater than 0");
      }
    }

    static Throttling disabled() {
      return new Throttling(false, 0, 0, Duration.ZERO);
    }
  }
}
