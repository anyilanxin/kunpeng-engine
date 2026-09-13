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
package com.anyilanxin.kunpeng.configuration.broker.partition;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 分区 Raft 配置，定义分区数量、副本因子与分区方案。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PartitioningRaftConfig {
  public static final int DEFAULT_PARTITIONS_COUNT = 1;
  public static final int DEFAULT_REPLICATION_FACTOR = 1;
  private final int partitionsCount = DEFAULT_PARTITIONS_COUNT;
  private final int replicationFactor = DEFAULT_REPLICATION_FACTOR;
  private final PartitionScheme scheme = PartitionScheme.ROUND_ROBIN;
  private List<FixedPartition> fixed;
  private String raftGroup;
}
