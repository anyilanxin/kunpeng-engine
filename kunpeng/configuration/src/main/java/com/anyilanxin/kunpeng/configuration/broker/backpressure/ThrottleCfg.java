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

import java.time.Duration;
import java.util.Objects;

public class ThrottleCfg {
  private boolean enabled = false;
  private int acceptableBacklog = 100_000;
  private int minimumLimit = 100;
  private Duration resolution = Duration.ofSeconds(15);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getMinimumLimit() {
    return minimumLimit;
  }

  public void setMinimumLimit(final int minimumLimit) {
    this.minimumLimit = minimumLimit;
  }

  public Duration getResolution() {
    return resolution;
  }

  public void setResolution(final Duration resolution) {
    this.resolution = resolution;
  }

  public int getAcceptableBacklog() {
    return acceptableBacklog;
  }

  public void setAcceptableBacklog(final int acceptableBacklog) {
    this.acceptableBacklog = acceptableBacklog;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, acceptableBacklog, minimumLimit, resolution);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final ThrottleCfg that)) {
      return false;
    }
    return enabled == that.enabled
        && acceptableBacklog == that.acceptableBacklog
        && minimumLimit == that.minimumLimit
        && Objects.equals(resolution, that.resolution);
  }

  @Override
  public String toString() {
    return "ThrottleCfg{"
        + "enabled="
        + enabled
        + ", acceptableBacklog="
        + acceptableBacklog
        + ", minimumLimit="
        + minimumLimit
        + ", resolution="
        + resolution
        + '}';
  }
}
