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
 * 业务侧粗粒度分区状态监听器：把 Raft 角色（{@link RaftRoleChangeListener}）与快照复制事件
 * （{@link SnapshotReplicationListener}）聚合为 LEADER / FOLLOWER / INACTIVE 三态视图。
 *
 * <p>映射关系：角色变更到 LEADER 触发 {@link #onLeader(long)}；变更到 FOLLOWER 触发 {@link
 * #onFollower(long)}；其余角色（INACTIVE/PASSIVE/PROMOTABLE/CANDIDATE）触发 {@link
 * #onInactive(long)}。快照复制开始时确定触发 {@link #onInactive(long)}（日志将被重置、消费者需
 * 关闭，业务视角不可用）；快照复制结束时确定触发 {@link #onFollower(long)}（恢复跟随可用）。
 *
 * <p>监听器在 Raft 线程回调，不应执行耗时操作。
 */
public interface RaftRoleStateListener {

  /** 本节点成为该分区 leader。 */
  void onLeader(long currentTerm);

  /** 本节点处于跟随/追赶状态（FOLLOWER 或快照复制进行中）。 */
  void onFollower(long currentTerm);

  /** 本节点处于非活跃状态（FOLLOWER 与 LEADER 之外的其余角色）。 */
  void onInactive(long currentTerm);
}
