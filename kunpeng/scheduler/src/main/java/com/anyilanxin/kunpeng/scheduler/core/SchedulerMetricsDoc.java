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
package com.anyilanxin.kunpeng.scheduler.core;

import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.docs.MeterDocumentation;

/**
 * 调度器指标定义（紧凑声明式; 前缀 scheduler.*）——单一事实源，实现类一律经此构建 meter。
 *
 * <p>问题域分组：actor 生命周期、job（按 pool 打标）、定时器、阻塞外包、池深度。
 */
public enum SchedulerMetricsDoc implements MeterDocumentation {
  ACTOR_SUBMITTED("scheduler.actor.submitted", Type.COUNTER, "提交调度的 actor 数"),
  ACTOR_STARTED("scheduler.actor.started", Type.COUNTER, "启动完成（STARTED）的 actor 数"),
  ACTOR_CLOSED("scheduler.actor.closed", Type.COUNTER, "关闭完成（CLOSED）的 actor 数"),
  ACTOR_FAILED("scheduler.actor.failed", Type.COUNTER, "失败（FAILED）的 actor 数"),
  TIMER_SCHEDULED("scheduler.timer.scheduled", Type.COUNTER, "定时器调度数"),
  TIMER_FIRED("scheduler.timer.fired", Type.COUNTER, "定时器触发数"),
  TIMER_CANCELLED("scheduler.timer.cancelled", Type.COUNTER, "定时器取消数"),
  BLOCKING_SUBMITTED("scheduler.blocking.submitted", Type.COUNTER, "阻塞任务外包提交数"),
  BLOCKING_COMPLETED("scheduler.blocking.completed", Type.COUNTER, "阻塞任务完成数"),
  JOB_SUBMITTED("scheduler.job.submitted", Type.COUNTER, "job 提交数"),
  JOB_EXECUTED("scheduler.job.executed", Type.COUNTER, "job 执行数"),
  JOB_REJECTED("scheduler.job.rejected", Type.COUNTER, "job 拒绝数"),
  QUEUE_DEPTH("scheduler.queue.depth", Type.GAUGE, "池内待执行 cell 深度");

  private final String name;
  private final Type type;
  private final String description;

  SchedulerMetricsDoc(final String name, final Type type, final String description) {
    this.name = name;
    this.type = type;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }
}
