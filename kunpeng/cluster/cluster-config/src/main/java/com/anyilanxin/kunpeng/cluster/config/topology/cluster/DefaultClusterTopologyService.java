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
package com.anyilanxin.kunpeng.cluster.config.topology.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.AbstractSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import java.util.List;
import java.util.Map;

/**
 * 网关端集群分区拓扑服务：只读汇聚各成员经 SWIM 广播的分区拓扑数据，供 broker-client 路由与查询使用。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultClusterTopologyService extends AbstractSwimTopologyService
    implements ClusterTopologyService {

  public DefaultClusterTopologyService(final ClusterMembershipService membershipService) {
    super(membershipService);
  }

  @Override
  public MemberId getPartitionLeader(final PartitionId partitionId) {
    return findPartitionLeader(partitionId);
  }

  @Override
  public List<PartitionMemberInfo> getPartitionMemberInfo(final PartitionId partitionId) {
    return partitionMemberInfoOf(partitionId);
  }

  @Override
  public Map<PartitionId, List<PartitionMemberInfo>> getRaftGroup(final String groupName) {
    return raftGroupOf(groupName);
  }

  @Override
  public Map<MemberId, List<PartitionMemberInfo>> getMemberPartitions() {
    return memberPartitionsView();
  }
}
