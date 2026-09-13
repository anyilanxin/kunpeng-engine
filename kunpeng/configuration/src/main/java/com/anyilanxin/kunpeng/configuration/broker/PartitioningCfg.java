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
package com.anyilanxin.kunpeng.configuration.broker;

import com.anyilanxin.kunpeng.configuration.ConfigurationLoggers;
import com.anyilanxin.kunpeng.configuration.broker.partition.FixedPartition;
import com.anyilanxin.kunpeng.configuration.broker.partition.PartitionScheme;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;

/**
 * 分区配置用于设置与分区相关的实验性配置。
 *
 * <p>目前，它允许用户配置分区方案，即分区如何在各 broker 之间分布。默认方案为 {@link PartitionScheme#ROUND_ROBIN}。
 *
 * <p>当使用 {@link PartitionScheme#FIXED} 时，需要在 {@link #fixed} 中指定 broker 到分区列表的映射。 该映射以 broker 节点 ID
 * 为键，值为分区 ID 列表。映射必须是完备的，即所有 broker 都应出现， 且所有分区都应按相应的副本因子进行指定。
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public final class PartitioningCfg implements ConfigurationEntry {
  public static final Logger LOG = ConfigurationLoggers.CONFIGURATION_LOGGER;
  public static final int DEFAULT_PARTITIONS_COUNT = 1;
  public static final int DEFAULT_REPLICATION_FACTOR = 1;
  private static final String REPLICATION_FACTOR_ERROR_MSG =
      "Replication factor %s needs to be larger then zero";
  private static final String REPLICATION_FACTOR_WARN_MSG =
      "Expected to have odd replication factor, but was even ({}). Even replication factor has no benefit over "
          + "the previous odd value and is weaker than next odd. Quorum is calculated as:"
          + " quorum = floor(replication factor / 2) + 1. In this current case the quorum will be"
          + " quorum = {}. If you want to ensure high fault-tolerance and availability,"
          + " make sure to use an odd replication factor.";

  /** 默认分区方案为 {@link PartitionScheme#ROUND_ROBIN}。这是向后兼容所必需的，且当前也是不错的默认值， 因为在大多数情况下没有更好的替代方案。 */
  private static final PartitionScheme DEFAULT_SCHEME = PartitionScheme.ROUND_ROBIN;

  private List<Integer> partitionIds;
  private PartitionScheme scheme = DEFAULT_SCHEME;
  private List<FixedPartition> fixed = new ArrayList<>();

  private int partitionsCount = DEFAULT_PARTITIONS_COUNT;
  private int replicationFactor = DEFAULT_REPLICATION_FACTOR;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    if (replicationFactor < 1) {
      throw new IllegalArgumentException(
          String.format(REPLICATION_FACTOR_ERROR_MSG, replicationFactor));
    }
    if (partitionsCount < 1) {
      throw new IllegalArgumentException("Partition count must not be smaller than 1.");
    }
    if (replicationFactor % 2 == 0) {
      LOG.warn(REPLICATION_FACTOR_WARN_MSG, replicationFactor, (replicationFactor / 2) + 1);
    }
    initPartitionIds();
  }

  private void initPartitionIds() {
    partitionIds = IntStream.range(1, 1 + partitionsCount).boxed().collect(Collectors.toList());
  }
}
