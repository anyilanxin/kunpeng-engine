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

import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;

/** 分区组事件监听器（由工厂返回列表，启动完成后注册） */
public interface PartitionEventListener {

  /** 分区角色变更（LEADER / FOLLOWER / CANDIDATE 等） */
  default void onRoleChange(String groupName, int partitionId, Role oldRole, Role newRole) {}

  /** 快照拍摄/接收完成 */
  default void onSnapshotTaken(String groupName, int partitionId, PersistedSnapshot snapshot) {}

  /** 分区提交索引推进 */
  default void onCommitAdvanced(String groupName, int partitionId, long commitIndex) {}

  /** 分区进入不健康状态 */
  default void onPartitionFailed(String groupName, int partitionId, Throwable error) {}

  /** 分区从故障中恢复 */
  default void onPartitionRecovered(String groupName, int partitionId) {}
}
