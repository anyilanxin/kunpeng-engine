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
package com.anyilanxin.kunpeng.broker.topology;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.PartitionTopologyListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;

/**
 * 拓扑管理服务：只负责监听本节点数据变化（分区角色/健康/source），随之把本进程唯一的 {@link BrokerInfo} 传播实例写入成员属性——是否传播由 SWIM
 * 底层自行决定。不进行集群收集；收集汇总由拓扑收集服务（ClusterTopologyService）承担。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface TopologyManager extends PartitionTopologyListener, RaftBusinessMetaListener {

  /** 本进程唯一的 broker 传播实体实例：其他子系统（如业务面端口）直接 set 该实例字段后调 {@link #publishBroadcast()} 发布。 */
  BrokerInfo localBroker();

  /** 节点离开分区分组后移除其分区条目并重写实体（分区不存在时无副作用）。 */
  void removePartition(PartitionId partitionId);

  /** 触发一次实体重写（外部线程安全入口，投递到 actor 线程执行）。 */
  void publishBroadcast();
}
