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

import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 拓扑管理器：基于 {@link ClusterTopologyService} 提供拓扑快照查询与变更监听注册
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerTopologyManager {

  private final ClusterTopologyService topologyService;
  private final List<TopologyListener> listeners = new CopyOnWriteArrayList<>();

  /** 快照缓存：拓扑版本未变时直接复用，避免每次查询深拷贝全量视图 */
  private volatile long cachedVersion = -1;

  private volatile BrokerClusterState cachedTopology;

  public BrokerTopologyManager(final ClusterTopologyService topologyService) {
    this.topologyService = topologyService;
  }

  public BrokerClusterState getTopology() {
    final long version = topologyService.getTopologyVersion();
    if (version != cachedVersion) {
      cachedTopology = new BrokerClusterState(topologyService.getMemberPartitions());
      cachedVersion = version;
    }
    return cachedTopology;
  }

  public void addTopologyListener(final TopologyListener listener) {
    listeners.add(listener);
  }

  public void removeTopologyListener(final TopologyListener listener) {
    listeners.remove(listener);
  }
}
