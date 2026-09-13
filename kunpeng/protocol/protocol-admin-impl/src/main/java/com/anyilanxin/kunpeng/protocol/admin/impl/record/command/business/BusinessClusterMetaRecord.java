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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessClusterMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务集群元数据实体，记录业务集群配置版本号、期望与当前副本因子、创建与更新时间， 以及当前与上一版的分区组拓扑列表。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessClusterMetaRecord extends UnifiedRecordValue<BusinessClusterMetaRecord>
    implements BusinessClusterMetaRecordValue {
  // structpack-ids[BusinessClusterMetaRecord]: 1,2,3,4,5,6,7,8
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
  private final IntegerProperty currentPartitionCountProp =
      new IntegerProperty(8, "CURRENT_PARTITION_COUNT", 0);

  public BusinessClusterMetaRecord() {
    super(8);
    // formatting:off
    declareProperty(versionProp)
      .declareProperty(replicationFactorProp)
      .declareProperty(currentReplicationFactorProp)
      .declareProperty(createTimeProp)
      .declareProperty(updateTimeProp)
      .declareProperty(metaProp)
      .declareProperty(lastMetaProp)
      .declareProperty(currentPartitionCountProp);
    // formatting:on
  }

  @Override
  public int getVersion() {
    return versionProp.getValue();
  }

  public BusinessClusterMetaRecord setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  /** 期望的副本因子 */
  @Override
  public int getReplicationFactor() {
    return replicationFactorProp.getValue();
  }

  public BusinessClusterMetaRecord setReplicationFactor(final int replicationFactor) {
    replicationFactorProp.setValue(replicationFactor);
    return this;
  }

  /** 当前实际的副本因子 */
  @Override
  public int getCurrentReplicationFactor() {
    return currentReplicationFactorProp.getValue();
  }

  public BusinessClusterMetaRecord setCurrentReplicationFactor(final int currentReplicationFactor) {
    currentReplicationFactorProp.setValue(currentReplicationFactor);
    return this;
  }

  /** 当前分区数（元数据为空时为 0，视为集群分区未初始化） */
  @Override
  public int getCurrentPartitionCount() {
    return currentPartitionCountProp.getValue();
  }

  public BusinessClusterMetaRecord setCurrentPartitionCount(final int currentPartitionCount) {
    currentPartitionCountProp.setValue(currentPartitionCount);
    return this;
  }

  @Override
  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public BusinessClusterMetaRecord setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  @Override
  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public BusinessClusterMetaRecord setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 当前生效的分区组拓扑列表(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionInfoMetaRecord> meta() {
    return metaProp;
  }

  @Override
  public List<PartitionInfoMetaRecordValue> getMeta() {
    final List<PartitionInfoMetaRecordValue> list = new ArrayList<>(metaProp.size());
    for (final PartitionInfoMetaRecord info : metaProp) {
      list.add(info);
    }
    return list;
  }

  public List<PartitionInfoMetaRecord> getMetaRecord() {
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

  @Override
  public List<PartitionInfoMetaRecordValue> getLastMeta() {
    final List<PartitionInfoMetaRecordValue> list = new ArrayList<>(lastMetaProp.size());
    for (final PartitionInfoMetaRecord info : lastMetaProp) {
      list.add(info);
    }
    return list;
  }

  public List<PartitionInfoMetaRecord> getLastMetaRecord() {
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
