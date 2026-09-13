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

public class GradientCfg {

  private int minLimit = 10;
  private int initialLimit = 20;
  private double rttTolerance = 2.0;

  public int getMinLimit() {
    return minLimit;
  }

  public void setMinLimit(final int minLimit) {
    checkPositive(minLimit, "minLimit");
    this.minLimit = minLimit;
  }

  public int getInitialLimit() {
    return initialLimit;
  }

  public void setInitialLimit(final int initialLimit) {
    checkPositive(initialLimit, "initialLimit");
    this.initialLimit = initialLimit;
  }

  public double getRttTolerance() {
    return rttTolerance;
  }

  public void setRttTolerance(final double rttTolerance) {
    checkArgument(
        rttTolerance >= 1.0, "Expected rttTolerance to be >= 1.0, but found %s", rttTolerance);
    this.rttTolerance = rttTolerance;
  }

  @Override
  public String toString() {
    return "GradientCfg{"
        + "minLimit="
        + minLimit
        + ", initialLimit="
        + initialLimit
        + ", rttTolerance="
        + rttTolerance
        + '}';
  }
}
