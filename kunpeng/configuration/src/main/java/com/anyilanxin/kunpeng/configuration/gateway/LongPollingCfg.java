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
package com.anyilanxin.kunpeng.configuration.gateway;

import java.util.Objects;

/** 长轮询配置，定义是否启用长轮询、超时时间与空响应阈值等。 */
public final class LongPollingCfg {

  private boolean enabled = ConfigurationDefaults.DEFAULT_LONG_POLLING_ENABLED;
  private long timeout = ConfigurationDefaults.DEFAULT_LONG_POLLING_TIMEOUT;
  private long probeTimeout = ConfigurationDefaults.DEFAULT_PROBE_TIMEOUT;
  private int minEmptyResponses =
      ConfigurationDefaults.DEFAULT_LONG_POLLING_EMPTY_RESPONSE_THRESHOLD;

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(final long timeout) {
    this.timeout = timeout;
  }

  public long getProbeTimeout() {
    return probeTimeout;
  }

  public void setProbeTimeout(final long probeTimeout) {
    this.probeTimeout = probeTimeout;
  }

  public int getMinEmptyResponses() {
    return minEmptyResponses;
  }

  public void setMinEmptyResponses(final int minEmptyResponses) {
    this.minEmptyResponses = minEmptyResponses;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public LongPollingCfg setEnabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, timeout, probeTimeout, minEmptyResponses);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final LongPollingCfg that = (LongPollingCfg) o;
    return enabled == that.enabled
        && timeout == that.timeout
        && probeTimeout == that.probeTimeout
        && minEmptyResponses == that.minEmptyResponses;
  }

  @Override
  public String toString() {
    return "LongPollingCfg{"
        + "enabled="
        + enabled
        + ", timeout="
        + timeout
        + ", probeTimeout="
        + probeTimeout
        + ", minEmptyResponses="
        + minEmptyResponses
        + '}';
  }
}
