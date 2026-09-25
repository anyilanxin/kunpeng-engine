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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import java.util.Map;

/**
 * 业务元数据（busimeta）变更监听器：新元数据经 raft 提交并实际安装时触发，业务侧借此在变更点 暂停/恢复对元数据的使用，与 {@link RaftRoleStateListener}
 * 的角色三态互不复用。
 *
 * <p>{@link #onStarted(PartitionId, Map, long, Role)} 与 {@link #onCompleted(PartitionId, Map, long,
 * Role)} 在同一 Raft 线程回调内 成对触发（紧配对，两者之间无其他事件插入）： onStarted 表示旧元数据即将失效（业务应暂停使用并准备重载），onCompleted
 * 表示新元数据已安装完成（可恢复使用）。 所有 raft 角色（含 leader）均触发； 不修改真实 raft 角色，携带的 role 为回调时刻的真实角色。
 *
 * <p>监听器在 Raft 线程回调，不应执行耗时操作。
 */
public interface RaftBusinessMetaListener {

  /** 业务元数据变更开始：新元数据即将生效，旧值不再可靠。 */
  void onStarted(
      final PartitionId partitionId, Map<String, String> entries, long currentTerm, Role role);

  /** 业务元数据安装完成：可按携带的当时角色恢复使用。 */
  void onCompleted(
      final PartitionId partitionId, Map<String, String> entries, long currentTerm, Role role);
}
