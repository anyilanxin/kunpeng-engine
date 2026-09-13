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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.PARTITION_SOURCE_KEY;

import com.anyilanxin.kunpeng.cluster.raft.metadata.PartitionBusinessMeta;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMeta;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessSourceMetaUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** 添加资源 */
  public static void addSourceInfo(final int sourceId, final RaftPartition raftPartition) {
    final PartitionSourceMeta newSourceMeta = new PartitionSourceMeta(sourceId, Set.of());
    updateSourceMeta(newSourceMeta, raftPartition);
  }

  /** 合并代理资源 */
  public static void mergeSourceInfo(
      final Set<Integer> agentSource, final RaftPartition raftPartition) {
    final PartitionSourceMeta sourceMeta = getSourceMeta(raftPartition);
    final Set<Integer> newAgentSourceIds = new HashSet<>(agentSource);
    final int sourceId = sourceMeta.sourceId();
    if (!sourceMeta.agentSourceIds().isEmpty()) {
      newAgentSourceIds.addAll(sourceMeta.agentSourceIds());
    }
    final PartitionSourceMeta newSourceMeta = new PartitionSourceMeta(sourceId, newAgentSourceIds);
    updateSourceMeta(newSourceMeta, raftPartition);
  }

  /** 移除代理资源 */
  public static void removeAgentSourceIds(
      final Set<Integer> agentSource, final RaftPartition raftPartition) {
    final PartitionSourceMeta sourceMeta = getSourceMeta(raftPartition);
    final Set<Integer> newAgentSourceIds = new HashSet<>();
    final int sourceId = sourceMeta.sourceId();
    if (!sourceMeta.agentSourceIds().isEmpty()) {
      newAgentSourceIds.addAll(sourceMeta.agentSourceIds());
    }
    newAgentSourceIds.removeAll(agentSource);
    final PartitionSourceMeta newSourceMeta = new PartitionSourceMeta(sourceId, newAgentSourceIds);
    updateSourceMeta(newSourceMeta, raftPartition);
  }

  /** 读取分区资源 */
  public static PartitionSourceMeta getSourceMeta(final Map<String, String> entries) {
    final String sourceMetaStr = entries.get(PARTITION_SOURCE_KEY);
    if (StringUtils.isBlank(sourceMetaStr)) {
      return PartitionSourceMeta.createDefault();
    }
    return MAPPER.readValue(sourceMetaStr, PartitionSourceMeta.class);
  }

  /** 读取分区资源 */
  public static PartitionSourceMeta getSourceMeta(final RaftPartition raftPartition) {
    final String sourceMetaStr = raftPartition.businessMeta().entries().get(PARTITION_SOURCE_KEY);
    if (StringUtils.isBlank(sourceMetaStr)) {
      return PartitionSourceMeta.createDefault();
    }
    return MAPPER.readValue(sourceMetaStr, PartitionSourceMeta.class);
  }

  /** 更新分区资源 */
  public static void updateSourceMeta(
      final PartitionSourceMeta partitionSourceMeta, final RaftPartition raftPartition) {
    final PartitionBusinessMeta partitionBusinessMeta = raftPartition.businessMeta();
    final Map<String, String> entries = new HashMap<>(partitionBusinessMeta.entries());
    entries.put(PARTITION_SOURCE_KEY, MAPPER.writeValueAsString(partitionSourceMeta));
    raftPartition.updateBusinessMeta(entries);
  }
}
