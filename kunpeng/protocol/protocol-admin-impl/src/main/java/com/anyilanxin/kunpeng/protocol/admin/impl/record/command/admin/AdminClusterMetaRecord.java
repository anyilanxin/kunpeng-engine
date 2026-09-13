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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;

/**
 * 集群元数据实体，记录集群配置版本号、创建与更新时间，以及当前与上一版的分区组拓扑信息。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminClusterMetaRecord extends UnifiedRecordValue<AdminClusterMetaRecord>
    implements AdminClusterMetaRecordValue {
  // structpack-ids[AdminClusterMetaRecord]: 1,2,3,4,5,6,7
  private final IntegerProperty versionProp = new IntegerProperty(1, "VERSION", 0);
  private final IntegerProperty replicationFactorProp =
      new IntegerProperty(2, "REPLICATION_FACTOR", 0);
  private final IntegerProperty currentReplicationFactorProp =
      new IntegerProperty(3, "CURRENT_REPLICATION_FACTOR", 0);
  private final LongProperty createTimeProp = new LongProperty(4, "CREATE_TIME", -1);
  private final LongProperty updateTimeProp = new LongProperty(5, "UPDATE_TIME", -1);
  private final ObjectProperty<PartitionInfoMetaRecord> metaProp =
      new ObjectProperty<>(6, "META", new PartitionInfoMetaRecord());
  private final ObjectProperty<PartitionInfoMetaRecord> lastMetaProp =
      new ObjectProperty<>(7, "LAST_META", new PartitionInfoMetaRecord());

  public AdminClusterMetaRecord() {
    super(7);
    // formatting:off
    declareProperty(versionProp)
      .declareProperty(replicationFactorProp)
      .declareProperty(currentReplicationFactorProp)
      .declareProperty(createTimeProp)
      .declareProperty(updateTimeProp)
      .declareProperty(metaProp)
      .declareProperty(lastMetaProp);
    // formatting:on
  }

  @Override
  public int getVersion() {
    return versionProp.getValue();
  }

  public AdminClusterMetaRecord setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  /** 期望的副本因子 */
  @Override
  public int getReplicationFactor() {
    return replicationFactorProp.getValue();
  }

  public AdminClusterMetaRecord setReplicationFactor(final int replicationFactor) {
    replicationFactorProp.setValue(replicationFactor);
    return this;
  }

  /** 当前实际的副本因子 */
  @Override
  public int getCurrentReplicationFactor() {
    return currentReplicationFactorProp.getValue();
  }

  public AdminClusterMetaRecord setCurrentReplicationFactor(final int currentReplicationFactor) {
    currentReplicationFactorProp.setValue(currentReplicationFactor);
    return this;
  }

  @Override
  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public AdminClusterMetaRecord setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  @Override
  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public AdminClusterMetaRecord setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 当前生效的分区组拓扑信息 */
  @Override
  public PartitionInfoMetaRecord getMeta() {
    return metaProp.getValue();
  }

  /** 更新前的分区组拓扑信息 */
  @Override
  public PartitionInfoMetaRecord getLastMeta() {
    return lastMetaProp.getValue();
  }

  public AdminClusterMetaRecord setMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, metaProp);
    return this;
  }

  public AdminClusterMetaRecord setLastMeta(final PartitionInfoMetaRecord lastMeta) {
    copyInto(lastMeta, lastMetaProp);
    return this;
  }

  /** 判断指定成员是否包含在当前生效的分区组拓扑(meta)中 */
  public boolean metaContainsMember(final String memberId) {
    for (final PartitionMemberMetaRecord member : getMeta().members()) {
      if (member.getMemberId().equals(memberId)) {
        return true;
      }
    }
    return false;
  }

  /** 判断指定成员是否包含在更新前的分区组拓扑(lastMeta)中 */
  public boolean lastMetaContainsMember(final String memberId) {
    for (final PartitionMemberMetaRecord member : getLastMeta().members()) {
      if (member.getMemberId().equals(memberId)) {
        return true;
      }
    }
    return false;
  }
}
