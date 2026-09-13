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

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zxuanhong
 * @since
 */
public class PartitionInfoMetadata {
  private final PartitionMetadata metadata;
  private final Set<Integer> agentSourceIds;
  private final int sourceId;

  public PartitionInfoMetadata(final PartitionMetadata metadata, final int sourceId) {
    this.metadata = metadata;
    this.sourceId = sourceId;
    agentSourceIds = new HashSet<>();
  }

  public PartitionMetadata getMetadata() {
    return metadata;
  }

  public Set<Integer> getAgentSourceIds() {
    return agentSourceIds;
  }

  public int getSourceId() {
    return sourceId;
  }

  public PartitionInfoMetadata addAgentSourceId(final int agentSourceId) {
    agentSourceIds.add(agentSourceId);
    return this;
  }

  public PartitionInfoMetadata addAgentSourceIds(final Set<Integer> agentSourceIds) {
    this.agentSourceIds.clear();
    if (agentSourceIds != null && !agentSourceIds.isEmpty()) {
      this.agentSourceIds.addAll(agentSourceIds);
    }
    return this;
  }

  public PartitionId id() {
    return metadata.id();
  }
}
