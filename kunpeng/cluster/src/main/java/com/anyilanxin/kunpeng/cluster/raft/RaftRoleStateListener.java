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
package com.anyilanxin.kunpeng.cluster.raft;

/**
 * 业务角色状态监听器：面向业务层的粗粒度角色状态通知。
 *
 * <p>与 {@link RaftRoleChangeListener}（完整角色粒度）不同，本监听器只区分三种状态：
 * <ul>
 *   <li>{@link #toLeader}——正常角色转换为 leader</li>
 *   <li>{@link #toFollower}——角色转换为 follower，或快照复制
 *       （{@code SnapshotReplicationListener#onSnapshotReplicationCompleted}）完成</li>
 *   <li>{@link #toInactive}——leader/follower 之外的角色（INACTIVE/PASSIVE/PROMOTABLE/
 *       CANDIDATE）转换，或快照复制
 *       （{@code SnapshotReplicationListener#onSnapshotReplicationStarted}）开始——
 *       此时本节点状态正在被快照替换，业务应暂停处理</li>
 * </ul>
 */
public interface RaftRoleStateListener {

  /** 转为 leader */
  void toLeader(long term);

  /** 转为 follower（角色转换或快照复制完成） */
  void toFollower(long term);

  /** 转为 inactive（leader/follower 之外的角色转换，或快照复制开始） */
  void toInactive(long term);
}
