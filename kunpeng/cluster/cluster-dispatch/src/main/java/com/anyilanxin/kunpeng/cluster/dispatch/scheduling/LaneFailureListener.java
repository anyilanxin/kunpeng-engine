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

/** 车道 actor 失败/重建回调：供上层做健康上报与 checker 重振。 */
public interface LaneFailureListener {

  /** 车道 actor 已失败；LanePool 会以新实例替换并重新提交。 */
  void onLaneFailed(ExecutionLane lane);

  /** 失败车道的替换实例已成功启动，可恢复向其排程。 */
  void onLaneRecovered(ExecutionLane lane);
}
