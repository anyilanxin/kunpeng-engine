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
package com.anyilanxin.kunpeng.repository.admin.modules.business.record;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 业务集群元数据实体，记录业务集群配置版本号、期望与当前副本因子、创建与更新时间， 以及当前与上一版的分区组拓扑列表。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessClusterMetaEntity extends UnpackedObject implements ValueType {
  // structpack-ids[BusinessClusterMetaRecord]: 1,2,3,4,5,6,7
  private final IntegerProperty versionProp = new IntegerProperty(1, "VERSION", 0);
  private final IntegerProperty replicationFactorProp =
      new IntegerProperty(2, "REPLICATION_FACTOR", 0);
  private final IntegerProperty currentReplicationFactorProp =
      new IntegerProperty(3, "CURRENT_REPLICATION_FACTOR", 0);
  private final LongProperty createTimeProp = new LongProperty(4, "CREATE_TIME", -1);
  private final LongProperty updateTimeProp = new LongProperty(5, "UPDATE_TIME", -1);
  private final ArrayProperty<PartitionInfoMetaRecord> metaProp =
      new ArrayProperty<>(6, "META", PartitionInfoMetaRecord::new);
  private final ArrayProperty<PartitionInfoMetaRecord> lastMetaProp =
      new ArrayProperty<>(7, "LAST_META", PartitionInfoMetaRecord::new);

  public BusinessClusterMetaEntity() {
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

  public void wrap(final BusinessClusterMetaRecord record) {
    reset();
    setVersion(record.getVersion())
        .setReplicationFactor(record.getReplicationFactor())
        .setCurrentReplicationFactor(record.getCurrentReplicationFactor())
        .setCreateTime(record.getCreateTime())
        .setUpdateTime(record.getUpdateTime());
    appendAll(record.meta(), metaProp);
    appendAll(record.lastMeta(), lastMetaProp);
  }

  public BusinessClusterMetaRecord unwrap(final BusinessClusterMetaRecord record) {
    record.reset();
    record
        .setVersion(getVersion())
        .setReplicationFactor(getReplicationFactor())
        .setCurrentReplicationFactor(getCurrentReplicationFactor())
        .setCreateTime(getCreateTime())
        .setUpdateTime(getUpdateTime());
    appendAll(metaProp, record.meta());
    appendAll(lastMetaProp, record.lastMeta());
    return record;
  }

  private static void appendAll(
      final ArrayProperty<PartitionInfoMetaRecord> source,
      final ArrayProperty<PartitionInfoMetaRecord> target) {
    for (final PartitionInfoMetaRecord info : source) {
      final UnsafeBuffer buffer = new UnsafeBuffer(new byte[info.getLength()]);
      info.write(buffer, 0);
      target.add().wrap(buffer, 0, buffer.capacity());
    }
  }

  public int getVersion() {
    return versionProp.getValue();
  }

  public BusinessClusterMetaEntity setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  /** 期望的副本因子 */
  public int getReplicationFactor() {
    return replicationFactorProp.getValue();
  }

  public BusinessClusterMetaEntity setReplicationFactor(final int replicationFactor) {
    replicationFactorProp.setValue(replicationFactor);
    return this;
  }

  /** 当前实际的副本因子 */
  public int getCurrentReplicationFactor() {
    return currentReplicationFactorProp.getValue();
  }

  public BusinessClusterMetaEntity setCurrentReplicationFactor(final int currentReplicationFactor) {
    currentReplicationFactorProp.setValue(currentReplicationFactor);
    return this;
  }

  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public BusinessClusterMetaEntity setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public BusinessClusterMetaEntity setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 当前生效的分区组拓扑列表(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionInfoMetaRecord> meta() {
    return metaProp;
  }

  public List<PartitionInfoMetaRecord> getMeta() {
    final List<PartitionInfoMetaRecord> list = new ArrayList<>(metaProp.size());
    for (final PartitionInfoMetaRecord info : metaProp) {
      list.add(info);
    }
    return list;
  }

  /** 更新前的分区组拓扑列表(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionInfoMetaRecord> lastMeta() {
    return lastMetaProp;
  }

  public List<PartitionInfoMetaRecord> getLastMeta() {
    final List<PartitionInfoMetaRecord> list = new ArrayList<>(lastMetaProp.size());
    for (final PartitionInfoMetaRecord info : lastMetaProp) {
      list.add(info);
    }
    return list;
  }

  /** 获取当前生效拓扑(meta)中包含指定成员的全部分区组信息 */
  public List<PartitionInfoMetaRecord> findMetaByMemberId(final String memberId) {
    final List<PartitionInfoMetaRecord> result = new ArrayList<>(metaProp.size());
    for (final PartitionInfoMetaRecord info : metaProp) {
      if (containsMember(info, memberId)) {
        result.add(info);
      }
    }
    return result;
  }

  /** 获取更新前拓扑(lastMeta)中包含指定成员的全部分区组信息 */
  public List<PartitionInfoMetaRecord> findLastMetaByMemberId(final String memberId) {
    final List<PartitionInfoMetaRecord> result = new ArrayList<>(lastMetaProp.size());
    for (final PartitionInfoMetaRecord info : lastMetaProp) {
      if (containsMember(info, memberId)) {
        result.add(info);
      }
    }
    return result;
  }

  private boolean containsMember(final PartitionInfoMetaRecord info, final String memberId) {
    for (final PartitionMemberMetaRecord member : info.members()) {
      if (member.getMemberId().equals(memberId)) {
        return true;
      }
    }
    return false;
  }
}
