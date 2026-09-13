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

import static com.anyilanxin.kunpeng.configuration.broker.ConfigurationUtil.checkPositive;
import static com.google.common.base.Preconditions.checkArgument;

import java.time.Duration;

public class AIMDCfg {

  private Duration requestTimeout = Duration.ofMillis(200);
  private int initialLimit = 100;
  private int minLimit = 1;
  private int maxLimit = 1000;
  private double backoffRatio = 0.9;

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(final Duration requestTimeout) {
    checkArgument(
        !requestTimeout.isNegative() && !requestTimeout.isZero(),
        "Expected requestTimeout to be > 0, but found %s",
        requestTimeout);
    this.requestTimeout = requestTimeout;
  }

  public int getInitialLimit() {
    return initialLimit;
  }

  public void setInitialLimit(final int initialLimit) {
    checkPositive(initialLimit, "initialLimit");
    this.initialLimit = initialLimit;
  }

  public int getMinLimit() {
    return minLimit;
  }

  public void setMinLimit(final int minLimit) {
    checkPositive(minLimit, "minLimit");
    this.minLimit = minLimit;
  }

  public int getMaxLimit() {
    return maxLimit;
  }

  public void setMaxLimit(final int maxLimit) {
    checkPositive(maxLimit, "maxLimit");
    this.maxLimit = maxLimit;
  }

  public double getBackoffRatio() {
    return backoffRatio;
  }

  public void setBackoffRatio(final double backoffRatio) {
    checkArgument(
        backoffRatio < 1.0 && backoffRatio >= 0.5,
        "Expected backoff ratio to be in the range [0.5, 1.0), but found %s",
        backoffRatio);
    this.backoffRatio = backoffRatio;
  }

  @Override
  public String toString() {
    return "AIMDCfg{"
        + "requestTimeout='"
        + requestTimeout
        + ", initialLimit="
        + initialLimit
        + ", minLimit="
        + minLimit
        + ", maxLimit="
        + maxLimit
        + ", backoffRatio="
        + backoffRatio
        + '}';
  }
}
