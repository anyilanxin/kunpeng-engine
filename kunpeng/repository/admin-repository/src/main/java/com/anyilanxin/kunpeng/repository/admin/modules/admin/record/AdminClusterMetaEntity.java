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
package com.anyilanxin.kunpeng.repository.admin.modules.admin.record;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
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
public class AdminClusterMetaEntity extends UnpackedObject implements ValueType {
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

  public AdminClusterMetaEntity() {
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

  public void wrap(final AdminClusterMetaRecord record) {
    setVersion(record.getVersion())
        .setReplicationFactor(record.getReplicationFactor())
        .setCurrentReplicationFactor(record.getCurrentReplicationFactor())
        .setCreateTime(record.getCreateTime())
        .setUpdateTime(record.getUpdateTime());
    copyInto(record.getMeta(), metaProp);
    copyInto(record.getLastMeta(), lastMetaProp);
  }

  public AdminClusterMetaRecord unwrap(final AdminClusterMetaRecord record) {
    record.reset();
    return record
        .setVersion(getVersion())
        .setReplicationFactor(getReplicationFactor())
        .setCurrentReplicationFactor(getCurrentReplicationFactor())
        .setCreateTime(getCreateTime())
        .setUpdateTime(getUpdateTime())
        .setMeta(getMeta())
        .setLastMeta(getLastMeta());
  }

  public int getVersion() {
    return versionProp.getValue();
  }

  public AdminClusterMetaEntity setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  /** 期望的副本因子 */
  public int getReplicationFactor() {
    return replicationFactorProp.getValue();
  }

  public AdminClusterMetaEntity setReplicationFactor(final int replicationFactor) {
    replicationFactorProp.setValue(replicationFactor);
    return this;
  }

  /** 当前实际的副本因子 */
  public int getCurrentReplicationFactor() {
    return currentReplicationFactorProp.getValue();
  }

  public AdminClusterMetaEntity setCurrentReplicationFactor(final int currentReplicationFactor) {
    currentReplicationFactorProp.setValue(currentReplicationFactor);
    return this;
  }

  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public AdminClusterMetaEntity setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public AdminClusterMetaEntity setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 当前生效的分区组拓扑信息 */
  public PartitionInfoMetaRecord getMeta() {
    return metaProp.getValue();
  }

  /** 更新前的分区组拓扑信息 */
  public PartitionInfoMetaRecord getLastMeta() {
    return lastMetaProp.getValue();
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
