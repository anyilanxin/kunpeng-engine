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
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;

/**
 * 分区健康状态：广播与拓扑视图使用的健康枚举，与健康报告组件解耦。
 *
 * @author zxuanhong
 * @since
 */
public enum PartitionHealth {
  /** 健康 */
  HEALTHY(2),
  /** 不健康（可自行恢复） */
  UNHEALTHY(1),
  /** 不可恢复故障 */
  DEAD(0),
  /** 健康状态未知（尚未收到健康报告时的默认值，替代 null） */
  UNKNOWN(-1);

  /** 健康度数值编码：值越大越健康，供 Micrometer gauge 等数值场景使用 */
  private final int value;

  PartitionHealth(final int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  /** 将健康报告状态映射为分区健康枚举；null 归 UNKNOWN */
  public static PartitionHealth of(final HealthReport.Status status) {
    if (status == null) {
      return UNKNOWN;
    }
    return switch (status) {
      case HEALTHY -> HEALTHY;
      case UNHEALTHY -> UNHEALTHY;
      case DEAD -> DEAD;
    };
  }

  /**
   * 将 Raft 内部角色映射为分区健康：active 角色（FOLLOWER/CANDIDATE/LEADER）为 HEALTHY，无角色（null）为 DEAD，其余为 UNHEALTHY
   */
  public static PartitionHealth of(final RaftServer.Role role) {
    if (role == null) {
      return DEAD;
    }
    if (role.active()) {
      return HEALTHY;
    }
    return UNHEALTHY;
  }
}
