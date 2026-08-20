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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 分区拓扑服务：查询集群全部分区的角色与健康信息。
 *
 * <p>数据来源：各节点将本地分区角色写入 member property 广播，
 * 服务监听集群成员元数据变动合并为全局拓扑。
 *
 * <p>用途：根据 {@link PartitionId} 查询分区 leader 节点，用于引导快照等跨节点操作。
 */
public interface PartitionTopologyService {

  /** 查询指定分区的 leader 节点（优先返回健康的 leader） */
  Optional<MemberId> findLeader(String groupName, int partitionId);

  /** 查询指定分区的 leader 节点 */
  Optional<MemberId> findLeader(PartitionId partitionId);

  /** 获取指定分区在所有节点上的信息 */
  List<PartitionInfo> getPartitionMembers(String groupName, int partitionId);

  /** 获取分区组下的全部分区信息 */
  List<PartitionInfo> getGroupTopology(String groupName);

  /** 获取全部分区组名 */
  Collection<String> getGroupNames();

  /** 获取指定分区组的全部分区号 */
  Collection<Integer> getPartitionIds(String groupName);
}
