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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.service.vo;

import java.util.List;

/**
 * 集群拓扑快照。
 *
 * @param clusterSize broker 成员数
 * @param gatewayVersion 网关版本
 * @param partitionsCount 分区总数
 * @param replicationFactor 副本因子
 * @param brokers broker 成员及其分区角色
 * @author zxuanhong
 * @since 2026.9.0
 */
public record ClusterTopologyVo(
    int clusterSize,
    String gatewayVersion,
    int partitionsCount,
    int replicationFactor,
    List<BrokerVo> brokers) {

  /** broker 成员。 */
  public record BrokerVo(List<PartitionVo> partitions) {}

  /** 成员上的单个分区角色。 */
  public record PartitionVo(int partitionId, int role, int health) {}
}
