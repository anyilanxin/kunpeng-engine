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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.scheduler.SchedulingHints;

/**
 * 可承接异步定时任务的具名执行车道。
 *
 * <p>每条车道携带用于标识其执行上下文的 actor 标签，以及描述底层 actor 应如何托管的调度 提示。
 */
public enum ExecutionLane {
  COMPUTE("ComputeLaneActor", SchedulingHints.CPU_BOUND),
  BATCH("BatchLaneActor", SchedulingHints.IO_BOUND);

  private final String label;
  private final SchedulingHints schedulingHints;

  ExecutionLane(final String label, final SchedulingHints schedulingHints) {
    this.label = label;
    this.schedulingHints = schedulingHints;
  }

  /** 返回标识本车道执行上下文的 actor 标签。 */
  public String label() {
    return label;
  }

  /** 返回描述本车道 actor 托管方式的调度提示。 */
  public SchedulingHints schedulingHints() {
    return schedulingHints;
  }
}
