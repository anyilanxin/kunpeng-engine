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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集群拓扑快照：按成员维度持有各成员广播的分区状态
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerClusterState {

  private final Map<MemberId, List<PartitionMemberInfo>> memberPartitions;

  public BrokerClusterState(final Map<MemberId, List<PartitionMemberInfo>> memberPartitions) {
    this.memberPartitions = Map.copyOf(memberPartitions);
  }

  public Map<MemberId, List<PartitionMemberInfo>> getMemberPartitions() {
    return memberPartitions;
  }

  /** 全集群出现过的分区集合（跨成员去重） */
  public Set<PartitionId> getPartitions() {
    final var partitions = new HashSet<PartitionId>();
    memberPartitions
        .values()
        .forEach(infos -> infos.forEach(info -> partitions.add(info.getPartitionId())));
    return partitions;
  }

  public int getPartitionsCount() {
    return getPartitions().size();
  }
}
