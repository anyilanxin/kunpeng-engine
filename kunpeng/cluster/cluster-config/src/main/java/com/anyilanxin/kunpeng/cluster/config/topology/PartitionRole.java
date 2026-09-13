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
package com.anyilanxin.kunpeng.cluster.config.topology;

import com.anyilanxin.kunpeng.cluster.raft.RaftServer;

/**
 * 分区业务视角角色：广播与拓扑视图使用的三态角色，与 Raft 内部细粒度状态解耦。
 *
 * @author zxuanhong
 * @since
 */
public enum PartitionRole {
  /** 分区主成员（Leader） */
  LEADER,
  /** 有投票权的跟随成员（含候选态） */
  FOLLOWER,
  /** 未参与投票（追赶、待晋升或未激活） */
  INACTIVE,
  /** 角色未知（尚未收到角色回调时的默认值，替代 null） */
  UNKNOWN;

  /** 将 Raft 内部角色映射为业务视角角色：LEADER 保留，有投票权归 FOLLOWER，其余归 INACTIVE；null 归 UNKNOWN */
  public static PartitionRole of(final RaftServer.Role role) {
    if (role == null) {
      return UNKNOWN;
    }
    return switch (role) {
      case LEADER -> LEADER;
      case FOLLOWER, CANDIDATE -> FOLLOWER;
      default -> INACTIVE;
    };
  }
}
