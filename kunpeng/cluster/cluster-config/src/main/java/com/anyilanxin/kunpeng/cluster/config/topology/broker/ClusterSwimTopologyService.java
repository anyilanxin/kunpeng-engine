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
package com.anyilanxin.kunpeng.cluster.config.topology.broker;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import java.util.List;
import java.util.Map;

/**
 * Broker 端 SWIM 拓扑服务：收集集群各成员经 SWIM 广播的分区拓扑供查询，同时发布本成员的分区拓扑广播（与只读收集的 {@link
 * com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService} 相对）。
 */
public interface ClusterSwimTopologyService {

  MemberId getPartitionLeader(PartitionId partitionId);

  List<PartitionMemberInfo> getPartitionMemberInfo(PartitionId partitionId);

  Map<PartitionId, List<PartitionMemberInfo>> getRaftGroup(String groupName);
}
