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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;

/**
 * 分区感知的拓扑监听器：在 Raft 角色/健康回调基础上携带分区标识。
 *
 * <p>Raft 原生回调不携带分区信息，由 {@code RaftTopologyStep} 注册时按分区适配转发， 供拓扑服务按分区维护广播状态。
 *
 * @author zxuanhong
 */
public interface PartitionTopologyListener extends FailureListener, RaftRoleChangeListener {

  /** 分区角色变更（含任期） */
  void onPartitionRoleChanged(PartitionId partitionId, RaftServer.Role newRole, long newTerm);

  /** 分区健康状态变更 */
  void onPartitionHealthChanged(PartitionId partitionId, HealthReport healthReport);

  @Override
  default void onNewRole(final RaftServer.Role newRole, final long newTerm) {}

  @Override
  default void onFailure(final HealthReport healthReport) {}

  @Override
  default void onRecovered(final HealthReport healthReport) {}

  @Override
  default void onUnrecoverableFailure(final HealthReport healthReport) {}
}
