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
package com.anyilanxin.kunpeng.broker.admin.raft.step;

import static com.anyilanxin.kunpeng.cluster.config.BusinessSourceMetaUtils.getSourceMeta;

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.PartitionSourceMeta;
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.agrona.collections.IntHashSet;

/**
 * 管理分区资源
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdminRaftPartitionSource implements RaftPartitionSource, RaftBusinessMetaListener {
  private int sourceId = 0;
  private final AtomicBoolean enable = new AtomicBoolean(false);
  private final Supplier<PartitionId> partitionId;

  public AdminRaftPartitionSource(final Supplier<PartitionId> partitionId) {
    this.partitionId = partitionId;
  }

  @Override
  public void onStarted(
      final PartitionId partitionId,
      final Map<String, String> entries,
      final long currentTerm,
      final RaftServer.Role role) {
    sourceId = 0;
  }

  @Override
  public void onCompleted(
      final PartitionId partitionId,
      final Map<String, String> entries,
      final long currentTerm,
      final RaftServer.Role role) {
    final PartitionSourceMeta sourceMeta = getSourceMeta(entries);
    sourceId = sourceMeta.sourceId();
  }

  @Override
  public int getSource() {
    if (!enable.get()) {
      throw new RuntimeException("当前资源不可用");
    }
    if (sourceId == 0) {
      throw new RuntimeException("当前资源不可用");
    }
    return sourceId;
  }

  @Override
  public IntHashSet getAgentSources() {
    return new IntHashSet();
  }

  @Override
  public PartitionId getPartitionId() {
    return partitionId.get();
  }
}
