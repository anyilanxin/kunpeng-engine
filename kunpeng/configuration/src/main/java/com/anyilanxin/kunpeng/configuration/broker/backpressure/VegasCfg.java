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

public class VegasCfg {

  private int alpha = 3;
  private int beta = 6;
  private int initialLimit = 20;

  public int getAlpha() {
    return alpha;
  }

  public void setAlpha(final int alpha) {
    checkPositive(alpha, "alpha");
    this.alpha = alpha;
  }

  public int getBeta() {
    return beta;
  }

  public void setBeta(final int beta) {
    checkPositive(beta, "beta");
    this.beta = beta;
  }

  public int getInitialLimit() {
    return initialLimit;
  }

  public void setInitialLimit(final int initialLimit) {
    checkPositive(initialLimit, "initialLimit");
    this.initialLimit = initialLimit;
  }

  @Override
  public String toString() {
    return "VegasCfg{"
        + "alpha="
        + alpha
        + ", beta="
        + beta
        + ", initialLimit="
        + initialLimit
        + '}';
  }
}
