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
package com.anyilanxin.kunpeng.configuration.broker;

import java.time.Duration;
import java.util.Set;

/**
 * Sinking component configuration. This configuration pertains to configurations that are common to
 * all sinks.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record SinkingCfg(Set<Long> skipRecords, Duration distributionInterval) {
  public static final Duration DEFAULT_DISTRIBUTION_INTERVAL = Duration.ofSeconds(15);

  public SinkingCfg(final Set<Long> skipRecords, final Duration distributionInterval) {
    this.skipRecords = skipRecords == null ? Set.of() : skipRecords;
    this.distributionInterval =
        distributionInterval == null ? DEFAULT_DISTRIBUTION_INTERVAL : distributionInterval;
  }

  public static SinkingCfg defaultSinkingCfg() {
    return new SinkingCfg(null, null);
  }
}
