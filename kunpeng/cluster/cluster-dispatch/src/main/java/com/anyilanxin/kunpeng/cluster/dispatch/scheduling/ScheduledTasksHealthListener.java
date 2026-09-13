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

/** 定时任务健康上报：车道 actor 失败/恢复时由引擎回调，实现方映射到分区健康面。 */
@FunctionalInterface
public interface ScheduledTasksHealthListener {

  /**
   * @param healthy true 表示定时调度能力已恢复
   * @param reason 状态变更原因（诊断用）
   */
  void onScheduledTasksHealth(boolean healthy, String reason);
}
