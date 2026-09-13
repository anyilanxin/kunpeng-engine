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
package com.anyilanxin.kunpeng.configuration.broker.backpressure;

import com.anyilanxin.kunpeng.configuration.broker.backpressure.limit.RateLimit;
import java.time.Duration;
import java.util.Objects;

public class RateLimitCfg {
  private boolean enabled = false;
  private int limit;
  private Duration rampUp = Duration.ZERO;
  private ThrottleCfg throttling = new ThrottleCfg();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(final int limit) {
    this.limit = limit;
  }

  public RateLimit buildLimit() {
    return new RateLimit(
        enabled,
        limit,
        rampUp,
        new RateLimit.Throttling(
            throttling.isEnabled(),
            throttling.getAcceptableBacklog(),
            throttling.getMinimumLimit(),
            throttling.getResolution()));
  }

  public Duration getRampUp() {
    return rampUp;
  }

  public void setRampUp(final Duration rampUp) {
    this.rampUp = rampUp;
  }

  public ThrottleCfg getThrottling() {
    return throttling;
  }

  public void setThrottling(final ThrottleCfg throttling) {
    this.throttling = throttling;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, limit, rampUp, throttling);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final RateLimitCfg that)) {
      return false;
    }
    return enabled == that.enabled
        && limit == that.limit
        && Objects.equals(rampUp, that.rampUp)
        && Objects.equals(throttling, that.throttling);
  }

  @Override
  public String toString() {
    return "RateLimitCfg{"
        + "enabled="
        + enabled
        + ", limit="
        + limit
        + ", rampUp="
        + rampUp
        + ", throttling="
        + throttling
        + '}';
  }
}
