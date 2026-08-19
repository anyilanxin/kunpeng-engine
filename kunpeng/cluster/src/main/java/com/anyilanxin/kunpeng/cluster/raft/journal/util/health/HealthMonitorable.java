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
package com.anyilanxin.kunpeng.cluster.raft.journal.util.health;

/** 可监控健康状态的组件 */
public interface HealthMonitorable {

  /** 组件名称 */
  String getName();

  /** @return 当前健康报告 */
  HealthReport getHealthReport();

  /** 注册故障监听器 */
  void addFailureListener(FailureListener listener);

  /** 移除故障监听器 */
  void removeFailureListener(FailureListener listener);
}
