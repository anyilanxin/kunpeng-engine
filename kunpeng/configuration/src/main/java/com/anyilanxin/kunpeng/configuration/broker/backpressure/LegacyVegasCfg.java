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

/** A Vegas configuration that matches our old default values for the log storage appender. */
public final class LegacyVegasCfg {

  private int initialLimit = 1024;
  private int maxConcurrency = 1024 * 32;
  private double alphaLimit = 0.7;
  private double betaLimit = 0.95;

  public void setInitialLimit(final int initialLimit) {
    this.initialLimit = initialLimit;
  }

  public void setAlphaLimit(final double alphaLimit) {
    this.alphaLimit = alphaLimit;
  }

  public void setBetaLimit(final double betaLimit) {
    this.betaLimit = betaLimit;
  }

  public double alphaLimit() {
    return alphaLimit;
  }

  public double betaLimit() {
    return betaLimit;
  }

  public int initialLimit() {
    return initialLimit;
  }

  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  public void setMaxConcurrency(final int maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  public String toString() {
    return "LegacyVegasCfg{"
        + "initialLimit="
        + initialLimit
        + ", maxConcurrency="
        + maxConcurrency
        + ", alphaLimit="
        + alphaLimit
        + ", betaLimit="
        + betaLimit
        + '}';
  }
}
