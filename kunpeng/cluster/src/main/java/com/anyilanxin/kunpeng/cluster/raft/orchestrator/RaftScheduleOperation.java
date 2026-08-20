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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

/** 跨节点调度操作类型 */
public enum RaftScheduleOperation {

  /** 在目标节点上启动指定分区组 */
  START_GROUP,

  /** 在目标节点上停止指定分区组 */
  STOP_GROUP,

  /** 将目标节点加入某个分区组 */
  JOIN,

  /** 将目标节点从某个分区组移除 */
  LEAVE,

  /** 变更分区组配置（addPeer/removePeer） */
  RECONFIGURE,

  /** 再平衡（领导权转移） */
  REBALANCE,

  /** 查询目标节点的分区组状态 */
  QUERY_STATUS
}
