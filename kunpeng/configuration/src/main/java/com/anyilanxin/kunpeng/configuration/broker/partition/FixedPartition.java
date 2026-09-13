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

import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 固定分区配置，定义分区 ID 及其包含的 raft 节点列表。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class FixedPartition {
  /** 默认分区 ID 为 1，取自开发中常用的单节点、单分区部署方式。 */
  private static final int DEFAULT_PARTITION_ID = 1;

  private final int partitionId = DEFAULT_PARTITION_ID;

  private final List<RaftNode> nodes = new ArrayList<>();
}
