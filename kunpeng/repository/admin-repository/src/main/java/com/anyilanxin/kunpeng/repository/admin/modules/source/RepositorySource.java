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
package com.anyilanxin.kunpeng.repository.admin.modules.source;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_RAFT_GROUP;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.IntType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.modules.source.record.NodeSourceEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.source.record.NodeSourceMetaEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.source.record.PartitionSourceEntity;
import com.anyilanxin.kunpeng.repository.admin.modules.source.record.PartitionSourceMetaEntity;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author zxuanhong
 * @since
 */
public class RepositorySource implements MutableRepositorySource {
  private static final int NODE_SOURCE_META_KEY = 1;
  private static final int PARTITION_SOURCE_META_KEY = 2;
  private final IntType nodeSourceMetaDbKey;
  private final NodeSourceMetaEntity nodeSourceMetaValueType;
  private final NodeSourceMetaRecord nodeSourceMetaBuffer;
  private final ColumnFamily<IntType, NodeSourceMetaEntity> nodeSourceMetaColumnFamily;

  private final StringType nodeSourceDbKey;
  private final NodeSourceEntity nodeSourceValueType;
  private final NodeSourceRecord nodeSourceBuffer;
  private final ColumnFamily<StringType, NodeSourceEntity> nodeSourceColumnFamily;

  private final IntType partitionSourceMetaDbKey;
  private final PartitionSourceMetaEntity partitionSourceMetaValueType;
  private final PartitionSourceMetaRecord partitionSourceMetaBuffer;
  private final ColumnFamily<IntType, PartitionSourceMetaEntity> partitionSourceMetaColumnFamily;

  private final StringType partitionGroupDbKey;
  private final IntType partitionIdDbKey;
  private final PartitionSourceEntity partitionSourceValueType;
  private final PartitionSourceRecord partitionSourceBuffer;
  private final CompositeKeyType<StringType, IntType> partitionGroupIdCompositeKey;
  private final ColumnFamily<CompositeKeyType<StringType, IntType>, PartitionSourceEntity>
      partitionSourceColumnFamily;

  public RepositorySource(
      final KvStore<AdminRepositoryColumnFamilies> db, final TransactionContext transaction) {
    nodeSourceMetaDbKey = new IntType();
    nodeSourceMetaDbKey.wrapInt(NODE_SOURCE_META_KEY);
    nodeSourceMetaValueType = new NodeSourceMetaEntity();
    nodeSourceMetaBuffer = new NodeSourceMetaRecord();
    nodeSourceMetaColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.NODE_SOURCE_META,
            transaction,
            nodeSourceMetaDbKey,
            nodeSourceMetaValueType);

    nodeSourceDbKey = new StringType();
    nodeSourceValueType = new NodeSourceEntity();
    nodeSourceBuffer = new NodeSourceRecord();
    nodeSourceColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.NODE_SOURCE,
            transaction,
            nodeSourceDbKey,
            nodeSourceValueType);

    partitionSourceMetaDbKey = new IntType();
    partitionSourceMetaDbKey.wrapInt(PARTITION_SOURCE_META_KEY);
    partitionSourceMetaValueType = new PartitionSourceMetaEntity();
    partitionSourceMetaBuffer = new PartitionSourceMetaRecord();
    partitionSourceMetaColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.PARTITION_SOURCE_META,
            transaction,
            partitionSourceMetaDbKey,
            partitionSourceMetaValueType);

    partitionGroupDbKey = new StringType();
    partitionIdDbKey = new IntType();
    partitionSourceValueType = new PartitionSourceEntity();
    partitionSourceBuffer = new PartitionSourceRecord();
    partitionGroupIdCompositeKey = new CompositeKeyType<>(partitionGroupDbKey, partitionIdDbKey);
    partitionSourceColumnFamily =
        db.createColumnFamily(
            AdminRepositoryColumnFamilies.PARTITION_SOURCE,
            transaction,
            partitionGroupIdCompositeKey,
            partitionSourceValueType);
  }

  @Override
  public void save(final NodeSourceMetaRecord record) {
    nodeSourceMetaValueType.wrap(record);
    nodeSourceMetaColumnFamily.put(nodeSourceMetaDbKey, nodeSourceMetaValueType);
  }

  @Override
  public void update(final NodeSourceMetaRecord record) {
    nodeSourceMetaValueType.wrap(record);
    nodeSourceMetaColumnFamily.put(nodeSourceMetaDbKey, nodeSourceMetaValueType);
  }

  @Override
  public void applied(final NodeSourceRecord record) {
    nodeSourceDbKey.wrapString(record.getMemberId());
    nodeSourceValueType.wrap(record);
    nodeSourceColumnFamily.put(nodeSourceDbKey, nodeSourceValueType);
  }

  @Override
  public void save(final PartitionSourceMetaRecord record) {
    partitionSourceMetaValueType.wrap(record);
    partitionSourceMetaColumnFamily.put(partitionSourceMetaDbKey, partitionSourceMetaValueType);
  }

  @Override
  public void update(final PartitionSourceMetaRecord record) {
    partitionSourceMetaValueType.wrap(record);
    partitionSourceMetaColumnFamily.put(partitionSourceMetaDbKey, partitionSourceMetaValueType);
  }

  @Override
  public void applied(final PartitionSourceRecord record) {
    partitionGroupDbKey.wrapString(record.getPartitionGroup());
    partitionIdDbKey.wrapInt(record.getPartitionId());
    partitionSourceValueType.wrap(record);
    partitionSourceColumnFamily.put(partitionGroupIdCompositeKey, partitionSourceValueType);
  }

  @Override
  public void transferred(final PartitionSourceRecord record) {
    // 目标分区合并转移资源标识符（含被缩分区降格的自身 sourceId）； 执行期与 ack 期会各应用一次，已持有的标识不重复追加
    partitionGroupDbKey.wrapString(record.getPartitionGroup());
    partitionIdDbKey.wrapInt(record.getPartitionId());
    if (partitionSourceColumnFamily.get(partitionGroupIdCompositeKey) != null) {
      final Set<Integer> existing = partitionSourceValueType.getAgentSourceIds();
      for (final IntegerValue agentSourceId : record.agentSourceIds()) {
        if (!existing.contains(agentSourceId.getValue())) {
          partitionSourceValueType.agentSourceIds().add().setValue(agentSourceId.getValue());
        }
      }
      partitionSourceColumnFamily.put(partitionGroupIdCompositeKey, partitionSourceValueType);
    }

    // 原分区删除资源标识符
    partitionGroupDbKey.wrapString(record.getSourcePartitionGroup());
    partitionIdDbKey.wrapInt(record.getSourcePartitionId());
    partitionSourceColumnFamily.delete(partitionGroupIdCompositeKey);
  }

  @Override
  public void transferredRemove(final PartitionSourceRecord record) {
    final PartitionSourceRecord stored =
        getPartitionSource(record.getPartitionGroup(), record.getPartitionId());
    if (stored == null) {
      return;
    }
    // 代理资源集合无单条删除能力，移除已转移标识后整记录重写
    final PartitionSourceRecord updated =
        new PartitionSourceRecord()
            .setPartitionGroup(stored.getPartitionGroup())
            .setPartitionId(stored.getPartitionId())
            .setSourceId(stored.getSourceId())
            .setSourcePartitionGroup(stored.getSourcePartitionGroup())
            .setSourcePartitionId(stored.getSourcePartitionId());
    for (final Integer agentSourceId : stored.getAgentSourceIds()) {
      if (agentSourceId != record.getSourceId()) {
        updated.agentSourceIds().add().setValue(agentSourceId);
      }
    }
    applied(updated);
  }

  @Override
  public NodeSourceRecord getNodeSource(final String memberId) {
    nodeSourceDbKey.wrapString(memberId);
    if (nodeSourceColumnFamily.get(nodeSourceDbKey) != null) {
      return nodeSourceValueType.unwrap(nodeSourceBuffer);
    }
    return null;
  }

  @Override
  public NodeSourceMetaRecord getNodeSourceMeta() {
    if (nodeSourceMetaColumnFamily.get(nodeSourceMetaDbKey) != null) {
      return nodeSourceMetaValueType.unwrap(nodeSourceMetaBuffer);
    }
    return null;
  }

  @Override
  public PartitionSourceRecord getPartitionSource(
      final String partitionGroup, final int partitionId) {
    partitionGroupDbKey.wrapString(partitionGroup);
    partitionIdDbKey.wrapInt(partitionId);
    if (partitionSourceColumnFamily.get(partitionGroupIdCompositeKey) != null) {
      return partitionSourceValueType.unwrap(partitionSourceBuffer);
    }
    return null;
  }

  @Override
  public List<PartitionSourceRecord> getPartitionSources() {
    final List<PartitionSourceRecord> result = new ArrayList<>();
    partitionSourceColumnFamily.forEach(
        _ -> result.add(partitionSourceValueType.unwrap(new PartitionSourceRecord())));
    return result;
  }

  @Override
  public List<PartitionSourceRecord> getPartitionAgentSources() {
    final List<PartitionSourceRecord> partitionSources = getPartitionSources();
    return partitionSources.stream()
        .filter(
            v ->
                !v.getAgentSourceIds().isEmpty()
                    && !ADMIN_RAFT_GROUP.equalsIgnoreCase(v.getPartitionGroup()))
        .toList();
  }

  @Override
  public PartitionSourceMetaRecord getPartitionSourceMeta() {
    if (partitionSourceMetaColumnFamily.get(partitionSourceMetaDbKey) != null) {
      return partitionSourceMetaValueType.unwrap(partitionSourceMetaBuffer);
    }
    return null;
  }
}
