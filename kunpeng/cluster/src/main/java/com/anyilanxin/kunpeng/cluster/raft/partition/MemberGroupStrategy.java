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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.NodeMemberGroup;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Member group strategy.
 *
 * <p>Member group strategies are default implementations of {@link MemberGroupProvider} for
 * built-in node attributes.
 */
public enum MemberGroupStrategy implements MemberGroupProvider {

  /**
   * Zone aware member group strategy.
   *
   * <p>This strategy will create a member group for each unique zone in the cluster.
   */
  ZONE_AWARE {
    @Override
    public Collection<MemberGroup> getMemberGroups(final Collection<Member> members) {
      return groupNodes(members, node -> node.zone() != null ? node.zone() : node.id().id());
    }
  },

  /**
   * Rack aware member group strategy.
   *
   * <p>This strategy will create a member group for each unique rack in the cluster.
   */
  RACK_AWARE {
    @Override
    public Collection<MemberGroup> getMemberGroups(final Collection<Member> members) {
      return groupNodes(members, node -> node.rack() != null ? node.rack() : node.id().id());
    }
  },

  /**
   * Host aware member group strategy.
   *
   * <p>This strategy will create a member group for each unique host in the cluster.
   */
  HOST_AWARE {
    @Override
    public Collection<MemberGroup> getMemberGroups(final Collection<Member> members) {
      return groupNodes(members, node -> node.host() != null ? node.host() : node.id().id());
    }
  },

  /**
   * Node aware member group strategy (the default).
   *
   * <p>This strategy will create a member group for each node in the cluster, effectively behaving
   * the same as if no member groups were defined.
   */
  NODE_AWARE {
    @Override
    public Collection<MemberGroup> getMemberGroups(final Collection<Member> members) {
      return groupNodes(members, node -> node.id().id());
    }
  };

  /**
   * Groups nodes by the given key function.
   *
   * @param members the nodes to group
   * @param keyFunction the key function to apply to nodes to extract a key
   * @return a collection of node member groups
   */
  protected Collection<MemberGroup> groupNodes(
      final Collection<Member> members, final Function<Member, String> keyFunction) {
    final Map<String, Set<Member>> groups = new HashMap<>();
    for (final Member member : members) {
      groups.computeIfAbsent(keyFunction.apply(member), k -> new HashSet<>()).add(member);
    }

    return groups.entrySet().stream()
        .map(entry -> new NodeMemberGroup(MemberGroupId.from(entry.getKey()), entry.getValue()))
        .collect(Collectors.toList());
  }
}
