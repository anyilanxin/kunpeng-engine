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

/** 组件故障/恢复回调 */
public interface FailureListener {

  /** 组件进入不健康状态 */
  void onFailure(HealthReport report);

  /** 组件进入不可恢复状态（进程应退出） */
  default void onUnrecoverableFailure(HealthReport report) {
    onFailure(report);
  }

  /** 组件从故障中恢复 */
  void onRecovered();
}
