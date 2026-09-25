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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl;

/**
 * 分区路由服务：维护资源到分区领导者的路由表。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class PartitionRouteService {
  private PartitionRouteInfo[] routeInfos = new PartitionRouteInfo[] {};

  public synchronized PartitionRouteService add(
      final int resourceId, final int partitionId, final String currentLeader) {
    final int length = routeInfos.length;
    if (resourceId >= length) {
      final PartitionRouteInfo[] newRouteInfos = new PartitionRouteInfo[resourceId + 10];
      System.arraycopy(routeInfos, 0, newRouteInfos, 0, length);
      routeInfos = newRouteInfos;
    }
    routeInfos[resourceId] = new PartitionRouteInfo(resourceId, partitionId, currentLeader);
    return this;
  }

  public PartitionRouteInfo getRouteInfo(final int resourceId) {
    if (resourceId < 0 || resourceId >= routeInfos.length) {
      return null;
    }
    return routeInfos[resourceId];
  }
}
