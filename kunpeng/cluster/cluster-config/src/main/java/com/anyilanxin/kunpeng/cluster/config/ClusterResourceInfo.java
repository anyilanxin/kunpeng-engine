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
package com.anyilanxin.kunpeng.cluster.config;

import com.anyilanxin.kunpeng.cluster.config.topology.PartitionHealth;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionRole;
import java.util.HashSet;
import java.util.Set;

/**
 * 集群资源视图：面向 gRPC/REST 管理面的成员×分区拓扑投影
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record ClusterResourceInfo(Set<MemberInfo> brokers) {

  /** 从拓扑快照投影资源视图 */
  public static ClusterResourceInfo to(final BrokerClusterState topology) {
    final var members = topology.getMemberPartitions();
    final var brokerSet = new HashSet<MemberInfo>(members.size());
    for (final var entry : members.entrySet()) {
      final var partitions =
          entry.getValue().stream()
              .map(
                  info ->
                      new PartitionInfo(
                          info.getPartitionId().id(),
                          Role.of(info.getRole()),
                          State.of(info.getHealth())))
              .toList();
      brokerSet.add(new MemberInfo(entry.getKey().id(), partitions));
    }
    return new ClusterResourceInfo(brokerSet);
  }

  public record MemberInfo(String nodeId, Iterable<PartitionInfo> partitions) {}

  public record PartitionInfo(int partitionId, Role role, State state) {}

  /** 与 gRPC PartitionBrokerRole 枚举值对齐 */
  public enum Role {
    LEADER(0),
    FOLLOWER(1),
    INACTIVE(2);

    private final int value;

    Role(final int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    static Role of(final PartitionRole role) {
      return switch (role) {
        case LEADER -> LEADER;
        case FOLLOWER -> FOLLOWER;
        default -> INACTIVE;
      };
    }
  }

  /** 与 gRPC PartitionBrokerHealth 枚举值对齐 */
  public enum State {
    HEALTHY(0),
    UNHEALTHY(1),
    DEAD(2);

    private final int value;

    State(final int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    static State of(final PartitionHealth health) {
      return switch (health) {
        case HEALTHY -> HEALTHY;
        case DEAD -> DEAD;
        default -> UNHEALTHY;
      };
    }
  }
}
