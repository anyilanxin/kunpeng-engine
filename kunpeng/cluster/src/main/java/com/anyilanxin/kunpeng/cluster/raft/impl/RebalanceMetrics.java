/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.impl;

import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;

/** rebalance 指标：领导权转移耗时与结果分布 */
public final class RebalanceMetrics {

  private final MeterRegistry registry;

  public RebalanceMetrics() {
    this(null);
  }

  public RebalanceMetrics(final MeterRegistry registry) {
    this.registry = registry;
  }

  /** 记录一次转移尝试的耗时与结果 */
  public void observeTransferDuration(final LeadershipTransferResult result, final Duration duration) {
    if (registry == null) {
      return;
    }
    Timer.builder("atomix_leadership_transfer_duration")
        .description("Leadership transfer duration")
        .tag("result", result.name())
        .register(registry)
        .record(duration);
  }
}
